package com.vn.datphong.enums;

/**
 * Trạng thái hiển thị nội dung tin nhắn.
 *
 * <p>Dùng tại: {@code Message.status}.</p>
 */
public enum MessageStatus {
    /** Tin nhắn đã được gửi và lưu trong hội thoại. */
    SENT,

    /** Nội dung đã được che hoặc gỡ theo chính sách, vẫn giữ dấu vết tin nhắn. */
    REDACTED
}
