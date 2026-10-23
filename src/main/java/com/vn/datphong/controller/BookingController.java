package com.vn.datphong.controller;

import com.vn.datphong.dto.request.BookingCancelRequest;
import com.vn.datphong.dto.request.BookingCreateRequest;
import com.vn.datphong.dto.response.*;
import com.vn.datphong.entity.Booking;
import com.vn.datphong.repository.BookingRepository;
import com.vn.datphong.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody BookingCreateRequest req,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return bookingService.createBooking(req, key);
    }

    @GetMapping("/me")
    public PageResponse<BookingResponse> mine(@RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return bookingService.mine(status, page, size);
    }

    @GetMapping("/{id}")
    public BookingDetailResponse detail(@PathVariable Long id) {
        return bookingService.detail(id);
    }

    @GetMapping("/{id}/cancellation-preview")
    public CancellationPreviewResponse preview(@PathVariable Long id) {
        return bookingService.preview(id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, @RequestBody(required = false) BookingCancelRequest req) {
        return bookingService.cancel(id, req);
    }
}