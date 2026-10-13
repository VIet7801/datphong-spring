package com.vn.datphong.service.Impl;

import com.vn.datphong.dto.request.BookingRequest;
import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.entity.Booking;
import com.vn.datphong.entity.Room;
import com.vn.datphong.entity.User;
import com.vn.datphong.entity.Voucher;
import com.vn.datphong.enums.BookingStatusType;
import com.vn.datphong.exception.BusinessException;
import com.vn.datphong.exception.ResourceNotFoundExcept;
import com.vn.datphong.mapper.BookingMapper;
import com.vn.datphong.repository.BookingRepository;
import com.vn.datphong.repository.RoomRepository;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.repository.VoucherRepository;
import com.vn.datphong.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private VoucherRepository voucher;

    private static final int HOLD_MINUTES = 15;
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public BookingResponse create( BookingRequest req) {

        User user =userRepository.findById(req.getId()).orElseThrow(() -> new ResourceNotFoundExcept("user not found: "+ req.getId()));
        Room room = roomRepository.findById(req.getId()).orElseThrow(() ->  new ResourceNotFoundExcept("room not found: "+req.getId()));
        Voucher voucher = null;
        if (bookingRepository.existsById(req.getId())){
            throw new BusinessException("Don hang da ton tai: "+req.getBookingCode());
        }
        long nights = ChronoUnit.DAYS.between(req.getCheckinDate(), req.getCheckoutDate());
        BigDecimal pricePerNight = BigDecimal.valueOf(room.getPricePerNight());
        BigDecimal total = pricePerNight.multiply(BigDecimal.valueOf(nights));
        Booking booking = Booking.builder()
                .bookingCode("BV" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(user)
                .room(room)
                .checkinDate(req.getCheckinDate())
                .checkoutDate(req.getCheckoutDate())
                .guestsCount(req.getGuestsCount())
                .status(BookingStatusType.HOLD)//giữ
                .holdExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES))
                .totalPrice(total)
                .currency("VND")
                .voucher(voucher)
                .loyaltyDiscountPercent(BigDecimal.ZERO)
                .build();
        return BookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse getById(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExcept("Booking khong ton tai: "+id));
        return BookingMapper.toResponse(b);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public BookingResponse update(Long id, BookingRequest req) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExcept("Booking khong ton tai: "+id));
        b.setStatus(BookingStatusType.CANCELLED);
        Booking merged = bookingRepository.save(b);
        return BookingMapper.toResponse(merged);
    }

    @Override
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    @Override
    public void delete(Long id) {
        bookingRepository.deleteById(id);
    }
}
