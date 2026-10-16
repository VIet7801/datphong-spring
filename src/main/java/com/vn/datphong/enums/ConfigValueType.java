package com.vn.datphong.enums;

/**
 * Kiểu dữ liệu dùng để diễn giải và kiểm tra giá trị cấu hình.
 *
 * <p>Dùng tại: {@code ConfigDefinition.valueType}, {@code ConfigValue.valueType}.</p>
 */
public enum ConfigValueType {
    /** Chuỗi ký tự. */
    STRING,

    /** Số nguyên. */
    INTEGER,

    /** Số thập phân. */
    DECIMAL,

    /** Giá trị đúng hoặc sai. */
    BOOLEAN,

    /** Khoảng thời gian theo định dạng cấu hình đã thống nhất. */
    DURATION,

    /** Dữ liệu JSON theo cấu trúc được kiểm tra. */
    JSON
}
