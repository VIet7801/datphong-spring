package com.vn.datphong.service;

import com.vn.datphong.dto.request.HostBookingNoteRequest;
import com.vn.datphong.dto.response.HostBookingResponse;
import com.vn.datphong.dto.response.PageResponse;

import java.time.LocalDate;

public interface HostBookingService {

    PageResponse<HostBookingResponse> list(Long propertyId, String status, LocalDate from, LocalDate to, int page, int size);

    HostBookingResponse detail(Long id);


    /** HOST chỉ sửa được ghi chú; kiểm tra version để không ghi đè thay đổi của người khác. */
    HostBookingResponse updateNote(Long id, HostBookingNoteRequest req);


    /** Idempotent: đã CHECKED_IN thì trả lại hiện trạng. Chỉ trong khoảng [ngày nhận, ngày trả) theo múi giờ cơ sở. */
    HostBookingResponse checkIn(Long id);

    HostBookingResponse checkOut(Long id);
}

