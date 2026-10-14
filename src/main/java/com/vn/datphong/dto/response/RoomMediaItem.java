package com.vn.datphong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một item media của phòng (ảnh hoặc video) — FE dùng id để xoá, mediaType để render đúng. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMediaItem {
    private Long id;
    private String url;
    /** IMAGE hoặc VIDEO */
    private String mediaType;
}
