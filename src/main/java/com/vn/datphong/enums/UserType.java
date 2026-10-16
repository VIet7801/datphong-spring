package com.vn.datphong.enums;

/**
 * Tập mã phân loại người dùng được giữ trong code hiện có.
 *
 * <p>Hiện chưa có thuộc tính entity nào sử dụng enum này.</p>
 *
 * <p>Hiện chưa được gắn với thuộc tính entity. User.roles sử dụng các entity Role/UserRole, không dùng enum này để thay bảng phân quyền.</p>
 */
public enum UserType {
    /** Khách chưa xác thực, không bắt buộc là một bản ghi Role. */
    GUEST,

    /** Mã người dùng cũ, không tự đồng nhất với CUSTOMER. */
    USER,

    /** Khách đặt phòng. */
    CUSTOMER,

    /** Chủ cơ sở lưu trú. */
    HOST,

    /** Nhân viên hỗ trợ. */
    SUPPORT,

    /** Quản trị viên. */
    ADMIN
}
