package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.HostBookingNoteRequest;
import com.vn.datphong.dto.response.HostBookingResponse;
import com.vn.datphong.dto.response.PageResponse;
import com.vn.datphong.entity.*;
import com.vn.datphong.enums.BookingStatusChangeSource;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.enums.OutboxStatus;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.*;
import com.vn.datphong.service.BookingService;
import com.vn.datphong.service.HostBookingService;
import com.vn.datphong.util.BookingStateMachine;
import com.vn.datphong.util.Json;
import com.vn.datphong.util.Masking;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HostBookingServiceImpl implements HostBookingService {
    private static final Set<BookingStatusType> CONTACT_VISIBLE = EnumSet.of(BookingStatusType.CONFIRMED,
            BookingStatusType.CHECKED_IN, BookingStatusType.COMPLETED);
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final OutboxEventRepository outboxRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HostBookingResponse> list(Long propertyId, String status, LocalDate from, LocalDate to, int page, int size) {
        User host = currentUser();
        List<Long> mine = new ArrayList<>();
        propertyRepository.findByOwner(host.getId()).forEach(p -> mine.add(p.getId()));
        if (propertyId != null) {
            if (!mine.contains(propertyId)) {
                throw DomainException.notFound("PROPERTY_NOT_FOUND", "Không tìm thấy cơ sở");
            }
            mine.retainAll(List.of(propertyId));
        }
        if (mine.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        Set<BookingStatusType> statuses = EnumSet.allOf(BookingStatusType.class);
        if (status != null && !status.isBlank()) {
            try {
                statuses = EnumSet.of(BookingStatusType.valueOf(status.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw DomainException.badRequest("VALIDATION_ERROR", "Trạng thái không hợp lệ");
            }
        }
        LocalDate f = from == null ? LocalDate.of(2000, 1, 1) : from;
        LocalDate t = to == null ? LocalDate.of(2100, 1, 1) : to;
        var p = bookingRepository.findForHost(mine, statuses, f, t, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        List<HostBookingResponse> items = new ArrayList<>();
        p.forEach(b -> items.add(view(b)));
        return new PageResponse<>(items, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public HostBookingResponse detail(Long id) {
        return view(owned(bookingRepository.findById(id).orElse(null)));
    }

    /** HOST chỉ sửa được ghi chú; kiểm tra version để không ghi đè thay đổi của người khác. */
    @Override
    @Transactional
    public HostBookingResponse updateNote(Long id, HostBookingNoteRequest req) {
        String note = req.getHostNote();
        Long version = req.getVersion();
        Booking b = owned(bookingRepository.findByIdForUpdate(id).orElse(null));
        if (version == null || !version.equals(b.getVersion())) {
            throw DomainException.conflict("VERSION_CONFLICT", "Booking đã được cập nhật bởi người khác, hãy tải lại");
        }
        b.setHostNote(note == null ? null : note.substring(0, Math.min(note.length(), 500)));
        bookingRepository.flush();
        return view(b);
    }

    /** Idempotent: đã CHECKED_IN thì trả lại hiện trạng. Chỉ trong khoảng [ngày nhận, ngày trả) theo múi giờ cơ sở. */
    @Override
    @Transactional
    public HostBookingResponse checkIn(Long id) {
        Booking b = owned(bookingRepository.findByIdForUpdate(id).orElse(null));
        if (b.getStatus() == BookingStatusType.CHECKED_IN || b.getStatus() == BookingStatusType.COMPLETED) {
            return view(b);
        }
        if (b.getStatus() != BookingStatusType.CONFIRMED) {
            throw DomainException.conflict("INVALID_STATE", "Chỉ booking CONFIRMED mới nhận phòng được");
        }
        LocalDate today = LocalDate.now(clock.withZone(zoneOf(b.getProperty())));
        if (today.isBefore(b.getCheckinDate()) || !today.isBefore(b.getCheckoutDate())) {
            throw DomainException.conflict("OUTSIDE_STAY_WINDOW", "Chưa đến hoặc đã quá thời gian lưu trú của booking");
        }
        transition(b, BookingStatusType.CHECKED_IN, BookingStatusChangeSource.HOST, currentUser(), "Nhận phòng");
        b.setActualCheckIn(LocalDateTime.now(clock));
        return view(b);
    }

    @Override
    @Transactional
    public HostBookingResponse checkOut(Long id) {
        Booking b = owned(bookingRepository.findByIdForUpdate(id).orElse(null));
        if (b.getStatus() == BookingStatusType.COMPLETED) {
            return view(b);
        }
        if (b.getStatus() != BookingStatusType.CHECKED_IN) {
            throw DomainException.conflict("INVALID_STATE", "Chỉ booking CHECKED_IN mới trả phòng được");
        }
        transition(b, BookingStatusType.COMPLETED, BookingStatusChangeSource.HOST, currentUser(), "Trả phòng");
        b.setActualCheckOut(LocalDateTime.now(clock));
        publish("BOOKING_COMPLETED", "BOOKING", b.getId(), Json.obj("bookingId", b.getId(),
                "bookingCode", b.getBookingCode(), "userId", b.getUser().getId()));
        return view(b);
    }

    private Booking owned(Booking b) {
        if (b == null || b.getProperty() == null || b.getProperty().getOwner() == null
                || !b.getProperty().getOwner().getId().equals(currentUser().getId())) {
            throw DomainException.notFound("BOOKING_NOT_FOUND", "Không tìm thấy booking");
        }
        return b;
    }

    private HostBookingResponse view(Booking b) {
        boolean visible = CONTACT_VISIBLE.contains(b.getStatus());
        User g = b.getUser();
        return new HostBookingResponse(b.getId(), b.getBookingCode(), b.getStatus().name(), b.getProperty().getId(),
                b.getProperty().getName(), b.getRoom().getId(), b.getRoom().getName(), b.getCheckinDate(),
                b.getCheckoutDate(), b.getGuestsCount(), b.getTotalPrice(), b.getCurrency(), g.getFullName(),
                visible ? g.getEmail() : Masking.email(g.getEmail()), visible ? g.getPhone() : Masking.phone(g.getPhone()),
                b.getSpecialRequest(), b.getHostNote(), b.getVersion(), b.getActualCheckIn(), b.getActualCheckOut());
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