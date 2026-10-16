package com.vn.datphong.enums;

/**
 * Phân loại hình thức media gắn với ảnh phòng hoặc nội dung đánh giá.
 *
 * <p>Dùng tại: {@code ReviewMedia.mediaType}, {@code RoomImage.mediaType}.</p>
 *
 * <p>Không thay thế MIME contentType và việc kiểm tra tệp của MediaAsset.</p>
 */
public enum MediaTypeEnum {
    /** Tệp hình ảnh. */
    IMAGE,

    /** Tệp video. */
    VIDEO
}
