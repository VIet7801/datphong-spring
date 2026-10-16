package com.vn.datphong.enums;

/**
 * Vai trò của một người tham gia trong phạm vi một hội thoại.
 *
 * <p>Dùng tại: {@code ConversationParticipant.participantRole}.</p>
 *
 * <p>Không thay thế bảng Role/UserRole hoặc kiểm tra quyền truy cập hội thoại.</p>
 */
public enum ParticipantRole {
    /** Khách tham gia hội thoại. */
    CUSTOMER,

    /** Chủ cơ sở lưu trú tham gia hội thoại. */
    HOST,

    /** Nhân viên hỗ trợ tham gia hội thoại. */
    SUPPORT
}
