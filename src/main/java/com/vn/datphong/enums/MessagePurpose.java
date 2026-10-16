package com.vn.datphong.enums;

/**
 * Mục đích nghiệp vụ của tin nhắn, tách khỏi dạng nội dung.
 *
 * <p>Dùng tại: {@code Message.purpose}.</p>
 */
public enum MessagePurpose {
    /** Trao đổi thông thường. */
    NORMAL,

    /** Tin nhắn phục vụ phản ánh hoặc khiếu nại. */
    COMPLAINT,

    /** Tin nhắn phục vụ yêu cầu nhận phòng. */
    CHECKIN_REQUEST
}
