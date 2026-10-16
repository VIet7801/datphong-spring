package com.vn.datphong.enums;

/**
 * Trạng thái vận hành phòng vật lý được dùng để đặt phòng.
 *
 * <p>Dùng tại: {@code Room.status}.</p>
 *
 * <p>Phòng có trống theo ngày hay không được xác định bằng khoảng lưu trú, allocation và block, không chỉ từ enum này.</p>
 */
public enum RoomStatus {
    /** Mã phòng sẵn sàng của luồng cũ, giữ để tương thích. */
    AVAILABLE,

    /** Phòng đang được khai thác theo mô hình lõi. */
    ACTIVE,

    /** Mã đang có khách của mô hình cũ, không dùng làm trạng thái tồn phòng toàn cục. */
    OCCUPIED,

    /** Phòng đang bảo trì. */
    MAINTENANCE,

    /** Phòng ngừng phục vụ. */
    OUT_OF_SERVICE,

    /** Phòng đang không được kích hoạt. */
    INACTIVE
}
