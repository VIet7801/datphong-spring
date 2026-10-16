package com.vn.datphong.enums;

/**
 * Dạng nội dung tin nhắn hiện đang được ánh xạ vào cột content_type.
 *
 * <p>Dùng tại: {@code Message.msgType}.</p>
 *
 * <p>Phân biệt với Message.purpose là mục đích nghiệp vụ. Tên msgType được giữ để tương thích accessor và API hiện có.</p>
 */
public enum MessageTypeEnum {
    /** Tin nhắn văn bản. */
    TEXT,

    /** Tin nhắn hình ảnh. */
    IMAGE,

    /** Tin nhắn vị trí, cần hợp đồng dữ liệu tương ứng. */
    LOCATION,

    /** Tin nhắn theo mẫu, cần hợp đồng dữ liệu tương ứng. */
    TEMPLATE
}
