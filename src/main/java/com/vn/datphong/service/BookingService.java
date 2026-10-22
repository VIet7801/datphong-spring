package com.vn.datphong.service;

import com.vn.datphong.dto.request.BookingCancelRequest;
import com.vn.datphong.dto.request.BookingCreateRequest;
import com.vn.datphong.dto.response.BookingDetailResponse;
import com.vn.datphong.dto.response.BookingResponse;
import com.vn.datphong.dto.response.CancellationPreviewResponse;
import com.vn.datphong.dto.response.PageResponse;
import com.vn.datphong.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {

       /** Idempotent theo (user, Idempotency-Key): trùng key + cùng nội dung phát lại kết quả cũ; khác nội dung -> IDEMPOTENCY_CONFLICT. */
       BookingResponse createBooking(BookingCreateRequest req, String idempotencyKey);

       BookingResponse viewOf(Long id);

       PageResponse<BookingResponse> mine(String statusFilter, int page, int size);

       BookingDetailResponse detail(Long id);

       /** Xem trước phí hủy theo SNAPSHOT chính sách lúc đặt, không theo cấu hình hiện tại. */
       CancellationPreviewResponse preview(Long bookingId);

       /** Idempotent: booking đã hủy trả về trạng thái hiện tại thay vì lỗi. */
       BookingResponse cancel(Long bookingId, BookingCancelRequest req);

       /** @return true nếu booking đã được chuyển EXPIRED và allocation đã được giải phóng. */
       boolean expireIfDue(Long bookingId);

       /** Job định kỳ quét các booking quá hạn giữ chỗ. */
       void scan();
}

