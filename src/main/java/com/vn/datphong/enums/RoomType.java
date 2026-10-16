package com.vn.datphong.enums;

/**
 * Danh mục loại phòng cố định của trường Room.type cũ.
 *
 * <p>Dùng tại: {@code Room.type}.</p>
 *
 * <p>Không thay thế RoomTypeEntity là danh mục loại phòng do cơ sở quản lý, được tham chiếu qua Room.roomTypeEntity.</p>
 */
public enum RoomType {
    /** Loại phòng đơn. */
    SINGLE,

    /** Loại phòng đôi. */
    DOUBLE,

    /** Loại phòng suite. */
    SUITE,

    /** Loại phòng deluxe. */
    DELUXE,

    /** Loại phòng gia đình. */
    FAMILY
}
