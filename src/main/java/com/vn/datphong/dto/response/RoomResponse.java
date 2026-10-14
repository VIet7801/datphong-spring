package com.vn.datphong.dto.response;

import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private String code;
    private String name;
    private RoomType type;
    private RoomStatus status;
    private Integer capacity;
    private Long pricePerNight;
    private String description;
    /** Ảnh bìa — URL MinIO của ảnh IMAGE đầu tiên (FE dùng cho card + hero). */
    private String imageUrl;
    /** Ảnh chi tiết (room_images trên MinIO, mediaType=IMAGE) — FE render carousel prev/next. */
    private List<String> images;
    /** Video giới thiệu phòng (room_images trên MinIO, mediaType=VIDEO). */
    private List<String> videos;
    /** Toàn bộ media (ảnh + video) kèm id — dùng cho màn quản lý để xoá từng item. */
    private List<RoomMediaItem> media;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
