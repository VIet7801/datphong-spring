package com.vn.datphong.enums;

/**
 * Trạng thái tạo và lưu giữ tệp xuất báo cáo.
 *
 * <p>Dùng tại: {@code ReportExport.status}.</p>
 */
public enum ReportStatus {
    /** Yêu cầu xuất báo cáo đang chờ xử lý. */
    QUEUED,

    /** Đang tạo tệp báo cáo. */
    RUNNING,

    /** Đã tạo xong tệp báo cáo. */
    COMPLETED,

    /** Xuất báo cáo thất bại. */
    FAILED,

    /** Yêu cầu xuất đã bị hủy. */
    CANCELLED,

    /** Tệp hoặc quyền tải báo cáo đã hết hạn theo chính sách. */
    EXPIRED
}
