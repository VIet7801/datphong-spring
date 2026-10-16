package com.vn.datphong.enums;

/**
 * Phân loại phiếu hỗ trợ tổng quát trong phần mở rộng helpdesk.
 *
 * <p>Dùng tại: {@code SupportTicket.ticketType}.</p>
 */
public enum TicketTypeEnum {
    /** Phản ánh về chất lượng dịch vụ. */
    QUALITY_COMPLAINT,

    /** Báo cáo hư hỏng hoặc thiệt hại. */
    DAMAGE_REPORT,

    /** Nội dung hỗ trợ khác. */
    OTHER
}
