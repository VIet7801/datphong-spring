package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.BookingCancelRequest;
import com.vn.datphong.dto.request.BookingCreateRequest;
import com.vn.datphong.dto.response.BookingDetailResponse;
import com.vn.datphong.dto.response.BookingHistoryResponse;
import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.dto.response.CancellationPreviewResponse;
import com.vn.datphong.dto.response.PageResponse;
import com.vn.datphong.entity.*;
import com.vn.datphong.enums.BookingStatusChangeSource;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.enums.OutboxStatus;
import com.vn.datphong.enums.PaymentStatusType;
import com.vn.datphong.enums.PropertyStatus;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.mapper.BookingMapper;
import com.vn.datphong.repository.*;
import com.vn.datphong.service.BookingService;
import com.vn.datphong.service.RefundService;
import com.vn.datphong.util.BookingStateMachine;
import com.vn.datphong.util.CancellationPolicySnapshot;
import com.vn.datphong.util.Hashing;
import com.vn.datphong.util.Json;
import com.vn.datphong.util.StayPricing;
import com.vn.datphong.util.StayRules;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final String IDEMPOTENCY_SCOPE_PREFIX = "booking:create:";
    private static final int MAX_NIGHTS = 30;
    /** Payment đang bay: không hết hạn/hủy booking, để callback/reconcile quyết định kết quả. */
    private static final Set<PaymentStatusType> IN_FLIGHT = EnumSet.of(PaymentStatusType.INITIATED,
            PaymentStatusType.PROCESSING, PaymentStatusType.TIMED_OUT);
    private static final Set<BookingStatusType> ALREADY_CANCELLED = EnumSet.of(BookingStatusType.CANCELLED,
            BookingStatusType.REFUND_PENDING, BookingStatusType.REFUNDED, BookingStatusType.CANCELLATION_PENDING);

    private final BookingRepository bookingRepository;
    private final BookingNightRepository nightRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final RoomAllocationRepository allocationRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final RoomRepository roomRepository;
    private final RoomRateRepository roomRateRepository;
    private final CancellationPolicyRuleRepository ruleRepository;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;
    private final OutboxEventRepository outboxRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readTx;
    private final int holdMinutes;
    private final BigDecimal taxRatePercent;
    private final BigDecimal flatFee;

    public BookingServiceImpl(BookingRepository bookingRepository, BookingNightRepository nightRepository,
                              BookingStatusHistoryRepository historyRepository, RoomAllocationRepository allocationRepository,
                              IdempotencyRecordRepository idempotencyRepository, RoomRepository roomRepository,
                              RoomRateRepository roomRateRepository, CancellationPolicyRuleRepository ruleRepository,
                              PaymentRepository paymentRepository, RefundService refundService,
                              OutboxEventRepository outboxRepository, UserRepository userRepository, Clock clock,
                              PlatformTransactionManager txManager,
                              @Value("${app.booking.hold-minutes:15}") int holdMinutes,
                              @Value("${app.pricing.tax-rate-percent:0}") BigDecimal taxRatePercent,
                              @Value("${app.pricing.flat-fee:0}") BigDecimal flatFee) {
        this.bookingRepository = bookingRepository;
        this.nightRepository = nightRepository;
        this.historyRepository = historyRepository;
        this.allocationRepository = allocationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.roomRepository = roomRepository;
        this.roomRateRepository = roomRateRepository;
        this.ruleRepository = ruleRepository;
        this.paymentRepository = paymentRepository;
        this.refundService = refundService;
        this.outboxRepository = outboxRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.writeTx = new TransactionTemplate(txManager);
        this.readTx = new TransactionTemplate(txManager);
        this.readTx.setReadOnly(true);
        this.holdMinutes = holdMinutes;
        this.taxRatePercent = taxRatePercent;
        this.flatFee = flatFee;
    }

    // ================================================================== UC-09 đặt phòng

    @Override
    public BookingResponse createBooking(BookingCreateRequest req, String idempotencyKey) {
        User user = currentUser();
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw DomainException.badRequest("IDEMPOTENCY_KEY_REQUIRED", "Thiếu hoặc sai header Idempotency-Key (tối đa 120 ký tự)");
        }
        String hash = Hashing.requestHash(req.getRoomId(), req.getCheckinDate(), req.getCheckoutDate(), req.getGuestsCount(),
                req.getExpectedTotal() == null ? "" : req.getExpectedTotal().stripTrailingZeros().toPlainString(),
                req.getSpecialRequest() == null ? "" : req.getSpecialRequest());
        String scope = IDEMPOTENCY_SCOPE_PREFIX + user.getId();
        Optional<IdempotencyRecord> existing = idempotencyRepository.findByScopeAndIdempotencyKey(scope, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), hash);
        }
        try {
            Long id = writeTx.execute(status -> createInTx(user, req, idempotencyKey, hash));
            return viewOf(id);
        } catch (DataIntegrityViolationException e) {
            Optional<IdempotencyRecord> now = idempotencyRepository.findByScopeAndIdempotencyKey(scope, idempotencyKey);
            if (now.isPresent()) {
                return replay(now.get(), hash);
            }
            throw DomainException.conflict("ROOM_UNAVAILABLE", "Phòng không còn trống cho khoảng ngày đã chọn");
        }
    }

    private BookingResponse replay(IdempotencyRecord rec, String hash) {
        if (!rec.getRequestHash().equals(hash)) {
            throw DomainException.conflict("IDEMPOTENCY_CONFLICT", "Idempotency-Key đã được dùng cho một yêu cầu khác");
        }
        if (rec.getResourceId() == null) {
            throw DomainException.conflict("REQUEST_IN_PROGRESS", "Yêu cầu trước đó đang được xử lý, thử lại sau");
        }
        return viewOf(rec.getResourceId());
    }

    /**
     * Một transaction: chiếm idempotency key -> khóa phòng -> giải phóng hold quá hạn -> kiểm tra trùng -> tính giá ->
     * ghi booking, từng đêm, allocation, lịch sử, outbox. Người thứ hai cùng đặt một phòng chờ khóa phòng rồi nhận
     * ROOM_UNAVAILABLE; exclusion constraint là lớp bảo vệ cuối.
     */
    private Long createInTx(User user, BookingCreateRequest req, String idempotencyKey, String requestHash) {
        // 1. Chiếm idempotency key trước: bản trùng đồng thời sẽ chờ index unique rồi nhận lỗi để phát lại.
        IdempotencyRecord rec = idempotencyRepository.saveAndFlush(IdempotencyRecord.builder()
                .scope(IDEMPOTENCY_SCOPE_PREFIX + user.getId()).idempotencyKey(idempotencyKey).requestHash(requestHash)
                .resourceType("BOOKING").build());
        // 2. Khóa phòng.
        Room room = roomRepository.findByIdForUpdate(req.getRoomId())
                .orElseThrow(() -> DomainException.notFound("ROOM_NOT_FOUND", "Không tìm thấy phòng"));
        Property property = room.getProperty();
        RoomTypeEntity type = room.getRoomTypeEntity();
        if (property == null || type == null || property.getStatus() != PropertyStatus.PUBLISHED
                || !Boolean.TRUE.equals(room.getActive()) || !Boolean.TRUE.equals(type.getActive())
                || room.getStatus() == null
                || !(room.getStatus().name().equals("AVAILABLE") || room.getStatus().name().equals("ACTIVE"))) {
            throw DomainException.conflict("ROOM_UNAVAILABLE", "Phòng hiện không nhận đặt");
        }
        if (property.getOwner() != null && property.getOwner().getId().equals(user.getId())) {
            throw DomainException.conflict("OWN_PROPERTY", "Không thể đặt phòng thuộc cơ sở của chính bạn");
        }
        // 3. Kiểm tra ngày theo múi giờ cơ sở và số khách.
        LocalDate today = LocalDate.now(clock.withZone(zoneOf(property)));
        try {
            StayRules.validate(req.getCheckinDate(), req.getCheckoutDate(), today, MAX_NIGHTS);
        } catch (StayRules.InvalidStayException e) {
            throw DomainException.badRequest(e.getCode(), e.getMessage());
        }
        int maxGuests = Math.min(room.getCapacity() == null ? 1 : room.getCapacity(),
                type.getMaxGuests() == null ? 1 : type.getMaxGuests());
        if (req.getGuestsCount() < 1 || req.getGuestsCount() > maxGuests) {
            throw DomainException.badRequest("VALIDATION_ERROR", "Số khách phải từ 1 đến " + maxGuests);
        }
        // 4. Giải phóng hold quá hạn đang cản chỗ, rồi kiểm tra trùng.
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long staleId : allocationRepository.findStaleBookingIds(room.getId(), req.getCheckinDate(), req.getCheckoutDate(), now)) {
            expireInTx(staleId);
        }
        if (allocationRepository.countActiveOverlap(room.getId(), req.getCheckinDate(), req.getCheckoutDate(), now) > 0) {
            throw DomainException.conflict("ROOM_UNAVAILABLE", "Phòng không còn trống cho khoảng ngày đã chọn");
        }
        // 5. Giá chốt phía server; so với giá khách đã thấy.
        StayPricing.PricedStay priced = StayPricing.price(
                roomRateRepository.findAllInRange(type.getId(), req.getCheckinDate(), req.getCheckoutDate()),
                req.getCheckinDate(), req.getCheckoutDate(), taxRatePercent, flatFee);
        var b = priced.breakdown();
        if (req.getExpectedTotal() != null && req.getExpectedTotal().compareTo(b.total()) != 0) {
            throw DomainException.conflict("PRICE_CHANGED", "Giá đã thay đổi so với báo giá",
                    Map.of("currentTotal", b.total(), "currency", priced.currency()));
        }
        // 6. Ghi dữ liệu.
        LocalDateTime expiresAt = now.plusMinutes(holdMinutes);
        String snapshot = StayPricing.snapshot(property, property.getCancellationPolicy() == null ? List.of()
                : ruleRepository.findByPolicy(property.getCancellationPolicy().getId())).serialize();
        Booking booking = Booking.builder()
                .bookingCode(newCode())
                .user(user).room(room).property(property).roomType(type).roomUnit(room.getRoomUnit())
                .checkinDate(req.getCheckinDate()).checkoutDate(req.getCheckoutDate()).guestsCount(req.getGuestsCount())
                .status(BookingStatusType.PENDING_PAYMENT)
                .holdExpiresAt(expiresAt).expiresAt(expiresAt)
                .totalPrice(b.total()).subtotalAmount(b.subtotal()).taxAmount(b.tax()).feeAmount(b.fee())
                .currency(priced.currency()).priceVersion(priced.priceVersion())
                .idempotencyKey(idempotencyKey)
                .cancellationPolicySnapshot(snapshot)
                .specialRequest(req.getSpecialRequest())
                .build();
        booking = bookingRepository.saveAndFlush(booking);
        for (RoomRate r : priced.nights()) {
            nightRepository.save(BookingNight.builder().booking(booking).stayDate(r.getStayDate())
                    .nightlyRate(r.getNightlyRate()).currency(priced.currency()).priceVersion(r.getPriceVersion()).build());
        }
        RoomAllocation alloc = new RoomAllocation();
        alloc.setRoom(room);
        alloc.setBooking(booking);
        alloc.setCheckinDate(req.getCheckinDate());
        alloc.setCheckoutDate(req.getCheckoutDate());
        alloc.setHoldExpiresAt(expiresAt);
        allocationRepository.saveAndFlush(alloc); // exclusion constraint kiểm tra tại đây
        historyRepository.save(BookingStatusHistory.builder().booking(booking).fromStatus(null)
                .toStatus(BookingStatusType.PENDING_PAYMENT).source(BookingStatusChangeSource.CUSTOMER).actor(user)
                .reason("Tạo booking").correlationId(MDC.get("traceId")).build());
        publish("BOOKING_CREATED", "BOOKING", booking.getId(), Json.obj("bookingId", booking.getId(),
                "bookingCode", booking.getBookingCode(), "userId", user.getId(),
                "total", booking.getTotalPrice(), "expiresAt", expiresAt.toString()));
        rec.setResourceId(booking.getId());
        idempotencyRepository.save(rec);
        return booking.getId();
    }

    private String newCode() {
        for (int i = 0; i < 5; i++) {
            String code = "BK" + Hashing.randomCode(8);
            if (!bookingRepository.existsByBookingCode(code)) {
                return code;
            }
        }
        throw DomainException.conflict("CODE_GENERATION_FAILED", "Không tạo được mã booking, thử lại");
    }

    // ================================================================== xem

    @Override
    public BookingResponse viewOf(Long id) {
        // Dựng view trong một transaction (open-in-view đang tắt nên không được map entity đã detach).
        return readTx.execute(status -> view(bookingRepository.findById(id).orElseThrow(this::notFound), true));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> mine(String statusFilter, int page, int size) {
        User user = currentUser();
        Set<BookingStatusType> statuses = EnumSet.allOf(BookingStatusType.class);
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                statuses = EnumSet.of(BookingStatusType.valueOf(statusFilter.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw DomainException.badRequest("VALIDATION_ERROR", "Trạng thái không hợp lệ: " + statusFilter);
            }
        }
        Page<Booking> p = bookingRepository.findByUser(user.getId(), statuses,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        List<BookingResponse> items = new ArrayList<>();
        p.forEach(b -> items.add(view(b, false)));
        return new PageResponse<>(items, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse detail(Long id) {
        Booking b = bookingRepository.findById(id).orElseThrow(this::notFound);
        authorize(b);
        List<BookingHistoryResponse> history = new ArrayList<>();
        historyRepository.findByBooking(id).forEach(h -> history.add(BookingMapper.toHistory(h)));
        return new BookingDetailResponse(view(b, true), history);
    }

    private BookingResponse view(Booking b, boolean withNights) {
        return BookingMapper.toResponse(b, withNights ? nightRepository.findByBooking(b.getId()) : null);
    }

    // ================================================================== UC-10 / UC-11 hủy

    @Override
    @Transactional(readOnly = true)
    public CancellationPreviewResponse preview(Long bookingId) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow(this::notFound);
        authorize(b);
        CancellationPolicySnapshot policy = CancellationPolicySnapshot.parse(b.getCancellationPolicySnapshot());
        if (b.getStatus() == BookingStatusType.PENDING_PAYMENT) {
            return CancellationPreviewResponse.builder().bookingId(b.getId()).currentStatus(b.getStatus()).cancellable(true)
                    .paidAmount(BigDecimal.ZERO).refundPercent(BigDecimal.ZERO).cancellationFee(BigDecimal.ZERO)
                    .refundableAmount(BigDecimal.ZERO).currency(b.getCurrency()).policySnapshot(policy.getName())
                    .reasonIfNotAllowed(null).build();
        }
        if (b.getStatus() != BookingStatusType.CONFIRMED) {
            return CancellationPreviewResponse.builder().bookingId(b.getId()).currentStatus(b.getStatus()).cancellable(false)
                    .paidAmount(BigDecimal.ZERO).refundPercent(BigDecimal.ZERO).cancellationFee(BigDecimal.ZERO)
                    .refundableAmount(BigDecimal.ZERO).currency(b.getCurrency()).policySnapshot(policy.getName())
                    .reasonIfNotAllowed("Trạng thái hiện tại không cho phép hủy").build();
        }
        BigDecimal paid = paymentRepository.netCaptured(b.getId());
        var r = policy.evaluate(clock.instant(), b.getCheckinDate(), paid, b.getCurrency());
        return CancellationPreviewResponse.builder().bookingId(b.getId()).currentStatus(b.getStatus()).cancellable(true)
                .paidAmount(paid).refundPercent(r.refundPercent()).cancellationFee(r.feeAmount())
                .refundableAmount(r.refundAmount()).currency(b.getCurrency()).policySnapshot(policy.getName())
                .reasonIfNotAllowed(null).build();
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long bookingId, BookingCancelRequest req) {
        String reason = req == null ? null : req.getReason();
        User actor = currentUser();
        Booking b = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(this::notFound);
        authorize(b);
        if (ALREADY_CANCELLED.contains(b.getStatus())) {
            return view(b, true);
        }
        BookingStatusChangeSource source = b.getUser().getId().equals(actor.getId())
                ? BookingStatusChangeSource.CUSTOMER
                : (hasAuthority("ROLE_ADMIN") ? BookingStatusChangeSource.ADMIN : BookingStatusChangeSource.SUPPORT);
        LocalDateTime now = LocalDateTime.now(clock);
        if (b.getStatus() == BookingStatusType.PENDING_PAYMENT) {
            if (!paymentRepository.findByBookingAndStatuses(bookingId, IN_FLIGHT).isEmpty()) {
                throw DomainException.conflict("PAYMENT_IN_PROGRESS", "Đang có thanh toán xử lý, vui lòng thử lại sau ít phút");
            }
            transition(b, BookingStatusType.CANCELLED, source, actor, reason);
            releaseAllocation(bookingId, "CANCELLED");
            stamp(b, actor, reason, BigDecimal.ZERO, BigDecimal.ZERO);
            publish("BOOKING_CANCELLED", "BOOKING", bookingId, Json.obj("bookingId", bookingId,
                    "bookingCode", b.getBookingCode(), "userId", b.getUser().getId(), "refund", 0));
            return view(b, true);
        }
        if (b.getStatus() != BookingStatusType.CONFIRMED) {
            throw DomainException.conflict("INVALID_STATE", "Không thể hủy booking ở trạng thái " + b.getStatus());
        }
        List<Payment> paid = paymentRepository.findByBookingAndStatuses(bookingId, EnumSet.of(PaymentStatusType.SUCCEEDED));
        BigDecimal net = paymentRepository.netCaptured(bookingId);
        CancellationPolicySnapshot policy = CancellationPolicySnapshot.parse(b.getCancellationPolicySnapshot());
        var result = policy.evaluate(clock.instant(), b.getCheckinDate(), net, b.getCurrency());
        stamp(b, actor, reason, result.feeAmount(), result.refundAmount());
        releaseAllocation(bookingId, "CANCELLED");
        if (result.refundAmount().signum() > 0 && !paid.isEmpty()) {
            transition(b, BookingStatusType.REFUND_PENDING, source, actor, reason);
            refundService.request(b, paid.get(0), result.refundAmount(), RefundService.KIND_CANCELLATION,
                    reason, "refund:cancel:" + bookingId);
        } else {
            transition(b, BookingStatusType.CANCELLED, source, actor, reason);
        }
        publish("BOOKING_CANCELLED", "BOOKING", bookingId, Json.obj("bookingId", bookingId,
                "bookingCode", b.getBookingCode(), "userId", b.getUser().getId(), "refund", result.refundAmount(),
                "fee", result.feeAmount(), "at", now.toString()));
        return view(b, true);
    }

    private void stamp(Booking b, User actor, String reason, BigDecimal fee, BigDecimal refund) {
        b.setCancelledAt(LocalDateTime.now(clock));
        b.setCancelledBy(actor);
        b.setCancellationReason(reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)));
        b.setCancellationFee(fee);
        b.setRefundableAmount(refund);
    }

    /** Chủ booking hoặc SUPPORT/ADMIN. Người khác nhận 404 để không lộ sự tồn tại của booking. */
    private void authorize(Booking b) {
        User u = currentUser();
        if (!b.getUser().getId().equals(u.getId()) && !isSupportOrAdmin()) {
            throw notFound();
        }
    }

    private DomainException notFound() {
        return DomainException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy booking");
    }

    // ================================================================== hết hạn giữ chỗ

    @Override
    public boolean expireIfDue(Long bookingId) {
        return Boolean.TRUE.equals(writeTx.execute(status -> expireInTx(bookingId)));
    }

    /**
     * Có hai đường vào: job định kỳ và giải phóng "lười" khi có booking mới tranh cùng phòng. Cả hai khóa booking rồi
     * kiểm tra lại trạng thái nên chạy trùng lặp là vô hại. Phải gọi trong transaction.
     */
    private boolean expireInTx(Long bookingId) {
        Booking b = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (b == null || b.getStatus() != BookingStatusType.PENDING_PAYMENT) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (b.getExpiresAt() == null || b.getExpiresAt().isAfter(now)) {
            return false;
        }
        if (!paymentRepository.findByBookingAndStatuses(bookingId, IN_FLIGHT).isEmpty()) {
            log.info("Bỏ qua hết hạn booking {} vì còn thanh toán đang xử lý", bookingId);
            return false;
        }
        transition(b, BookingStatusType.EXPIRED, BookingStatusChangeSource.SYSTEM, null, "Hết hạn giữ chỗ");
        releaseAllocation(bookingId, "EXPIRED");
        publish("BOOKING_EXPIRED", "BOOKING", bookingId,
                Json.obj("bookingId", bookingId, "bookingCode", b.getBookingCode(), "userId", b.getUser().getId()));
        return true;
    }

    @Override
    @Scheduled(fixedDelayString = "${app.booking.expiry-scan-ms:30000}")
    public void scan() {
        try {
            List<Long> ids = bookingRepository.findDueIds(BookingStatusType.PENDING_PAYMENT, LocalDateTime.now(clock),
                    PageRequest.of(0, 100));
            for (Long id : ids) {
                try {
                    expireIfDue(id);
                } catch (RuntimeException e) {
                    log.warn("Không thể hết hạn booking {}: {}", id, e.toString());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Quét hết hạn thất bại: {}", e.toString());
        }
    }

    // ---------------------------------------------------------------- helper nội bộ: vòng đời booking
    /** Mọi đổi trạng thái booking đi qua đây, luôn trong transaction đã khóa booking. */
    private void transition(Booking booking, BookingStatusType to, BookingStatusChangeSource source, User actor, String reason) {
        BookingStatusType from = booking.getStatus();
        if (from == to) {
            return;
        }
        if (!BookingStateMachine.canTransition(from, to)) {
            throw DomainException.conflict("INVALID_STATE", "Không thể chuyển booking từ " + from + " sang " + to);
        }
        booking.setStatus(to);
        historyRepository.save(BookingStatusHistory.builder()
                .booking(booking).fromStatus(from).toStatus(to).source(source).actor(actor)
                .reason(reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)))
                .correlationId(MDC.get("traceId"))
                .build());
    }

    private void releaseAllocation(Long bookingId, String reason) {
        allocationRepository.findByBookingId(bookingId).ifPresent(a -> {
            if (a.getReleasedAt() == null) {
                a.setReleasedAt(LocalDateTime.now(clock));
                a.setStatus(com.vn.datphong.enums.AllocationStatus.RELEASED);
                a.setReleaseReason(reason);
                a.setHoldExpiresAt(null);
            }
        });
    }

    /** Xác nhận thanh toán: allocation trở thành vĩnh viễn (không còn hạn giữ). */
    private void pinAllocation(Long bookingId) {
        allocationRepository.findByBookingId(bookingId).ifPresent(a -> {
            a.setHoldExpiresAt(null);
            a.setStatus(com.vn.datphong.enums.AllocationStatus.CONFIRMED);
        });
    }

    // ---------------------------------------------------------------- helper nội bộ: outbox
    /** Ghi sự kiện outbox CÙNG transaction với thay đổi nghiệp vụ: rollback thì sự kiện cũng mất. */
    private void publish(String eventType, String aggregateType, Long aggregateId, String payload) {
        outboxRepository.save(OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload == null ? "" : payload)
                .status(OutboxStatus.PENDING)
                .availableAt(LocalDateTime.now(clock))
                .build());
    }

    // ---------------------------------------------------------------- helper nội bộ: người gọi
    /** Danh tính lấy từ SecurityContext (JWT đã xác thực), không bao giờ từ body request. */
    private Authentication authentication() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw DomainException.unauthorized("UNAUTHORIZED", "Chưa xác thực");
        }
        return a;
    }

    private User currentUser() {
        return userRepository.findByUsername(authentication().getName())
                .orElseThrow(() -> DomainException.unauthorized("UNAUTHORIZED", "Tài khoản không còn tồn tại"));
    }

    private boolean hasAuthority(String authority) {
        return authentication().getAuthorities().stream().anyMatch(g -> authority.equals(g.getAuthority()));
    }

    private boolean isSupportOrAdmin() {
        return hasAuthority("ROLE_SUPPORT") || hasAuthority("ROLE_ADMIN");
    }

    private static ZoneId zoneOf(Property p) {
        try {
            return ZoneId.of(p.getTimezone());
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }
}
