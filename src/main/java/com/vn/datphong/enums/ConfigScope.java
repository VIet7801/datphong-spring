package com.vn.datphong.enums;

/**
 * Phạm vi áp dụng của một giá trị cấu hình.
 *
 * <p>Dùng tại: {@code ConfigValue.scope}.</p>
 *
 * <p>ConfigDefinition.allowedScopes lưu danh sách phạm vi trong JSON. Không suy ra quan hệ FK tenant chỉ từ enum này.</p>
 */
public enum ConfigScope {
    /** Áp dụng ở cấp toàn hệ thống. */
    GLOBAL,

    /** Phạm vi tenant dự kiến, chưa cho ghi khi chưa có cơ chế xác minh tenant. */
    TENANT,

    /** Áp dụng cho một cơ sở lưu trú xác định qua scopeId. */
    PROPERTY
}
