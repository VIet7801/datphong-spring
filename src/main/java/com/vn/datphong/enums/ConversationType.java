package com.vn.datphong.enums;

/**
 * Mục đích tổng quát của hội thoại giữa khách, chủ nhà và hỗ trợ.
 *
 * <p>Dùng tại: {@code Conversation.type}.</p>
 */
public enum ConversationType {
    /** Hội thoại hỏi thông tin trước hoặc ngoài một booking cụ thể. */
    INQUIRY,

    /** Hội thoại hỗ trợ liên quan đến booking. */
    BOOKING_SUPPORT
}
