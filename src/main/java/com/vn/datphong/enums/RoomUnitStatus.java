package com.vn.datphong.enums;

/**
 * Trạng thái vận hành đơn vị phòng trong mô hình tương thích cũ.
 *
 * <p>Dùng tại: {@code RoomUnit.status}.</p>
 *
 * <p>RoomUnit không phải nguồn tồn phòng song song với Room trong thiết kế lõi.</p>
 */
public enum RoomUnitStatus {
    /** Đơn vị phòng đang được kích hoạt. */
    ACTIVE,

    /** Đơn vị phòng đang bảo trì. */
    MAINTENANCE,

    /** Đơn vị phòng ngừng phục vụ. */
    OUT_OF_SERVICE,

    /** Đơn vị phòng không được kích hoạt. */
    INACTIVE
}
