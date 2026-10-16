//package com.vn.datphong.service.Impl;
//
//
//import com.vn.datphong.dto.request.BookingCreateRequest;
//import com.vn.datphong.dto.request.BookingRequest;
//import com.vn.datphong.dto.response.BookingResponse;
//import com.vn.datphong.entity.Booking;
//import com.vn.datphong.entity.Room;
//import com.vn.datphong.entity.User;
//import com.vn.datphong.entity.Voucher;
//import com.vn.datphong.enums.BookingStatusType;
//import com.vn.datphong.exception.BusinessException;
//import com.vn.datphong.exception.ResourceNotFoundExcept;
//import com.vn.datphong.mapper.BookingMapper;
//import com.vn.datphong.repository.BookingRepository;
//import com.vn.datphong.repository.RoomRepository;
//import com.vn.datphong.repository.UserRepository;
//import com.vn.datphong.repository.VoucherRepository;
//import com.vn.datphong.service.BookingService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Propagation;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class BookingServiceImpl implements BookingService {
//    private final BookingRepository bookingRepository;
//    private final RoomRepository roomRepository;
//    @Override
//    public BookingResponse createBooking(BookingCreateRequest req, Long userId, String idempotencyKey) {
//          Room room=roomRepository.findById(req.getRoomId())
//                  .orElseThrow(()->new ResourceNotFoundExcept("không tìm thấy phòng"));
//
//    }
//}
