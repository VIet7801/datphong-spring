package com.vn.datphong.enums;

/**
 * Trạng thái công bố và vận hành cơ sở lưu trú.
 *
 * <p>Dùng tại: {@code Property.status}.</p>
 */
public enum PropertyStatus {
    /** Cơ sở đang ở dạng bản nháp. */
    DRAFT,

    /** Cơ sở đã được công bố. */
    PUBLISHED,

    /** Cơ sở đang tạm dừng công bố hoặc vận hành theo luồng nghiệp vụ. */
    PAUSED,

    /** Cơ sở đã được lưu trữ, ngừng sử dụng trong luồng công bố hiện hành. */
    ARCHIVED
}
