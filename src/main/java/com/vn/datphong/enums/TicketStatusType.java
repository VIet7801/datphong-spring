package com.vn.datphong.enums;

/**
 * Trạng thái phiếu hỗ trợ tổng quát trong phần mở rộng helpdesk.
 *
 * <p>Dùng tại: {@code SupportTicket.status}.</p>
 *
 * <p>Không thay ComplaintStatus của ComplaintTicket cho luồng phản ánh lõi.</p>
 */
public enum TicketStatusType {
    /** Phiếu hỗ trợ mới được mở. */
    OPEN,

    /** Đang kiểm tra và xử lý nội dung phiếu. */
    INVESTIGATING,

    /** Phiếu đã được giải quyết. */
    RESOLVED,

    /** Phiếu đã bị từ chối. */
    REJECTED
}
