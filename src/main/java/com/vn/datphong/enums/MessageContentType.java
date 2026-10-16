package com.vn.datphong.enums;

/**
 * Tập mã mô tả dạng nội dung tin nhắn theo thiết kế.
 *
 * <p>Hiện chưa có thuộc tính entity nào sử dụng enum này.</p>
 *
 * <p>Hiện chưa được gắn với thuộc tính entity. Message.msgType đang dùng MessageTypeEnum và ánh xạ cột content_type, không tạo thêm nguồn dữ liệu thứ hai.</p>
 */
public enum MessageContentType {
    /** Nội dung văn bản. */
    TEXT,

    /** Nội dung hình ảnh. */
    IMAGE,

    /** Nội dung vị trí, chỉ sử dụng khi có hợp đồng dữ liệu phù hợp. */
    LOCATION,

    /** Nội dung theo mẫu, chỉ sử dụng khi có hợp đồng dữ liệu phù hợp. */
    TEMPLATE
}
