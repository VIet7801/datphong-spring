package com.vn.datphong.enums;

/**
 * Trạng thái vòng đời mẫu email.
 *
 * <p>Dùng tại: {@code EmailTemplate.status}.</p>
 *
 * <p>Phiên bản nội dung cụ thể nằm ở EmailTemplateVersion.</p>
 */
public enum TemplateStatus {
    /** Mẫu email đang ở dạng bản nháp. */
    DRAFT,

    /** Mẫu email đang được kích hoạt. */
    ACTIVE,

    /** Mẫu email đã được lưu trữ, không còn là mẫu đang hoạt động. */
    ARCHIVED
}
