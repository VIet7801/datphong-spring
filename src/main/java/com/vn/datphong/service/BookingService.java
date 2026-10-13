package com.vn.datphong.service;

import com.vn.datphong.dto.request.BookingRequest;
import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {
        BookingResponse create(BookingRequest req);
        BookingResponse getById(Long id);
        BookingResponse update(Long id,BookingRequest req);
        List<Booking> findAll();
        void delete(Long id);
    }

