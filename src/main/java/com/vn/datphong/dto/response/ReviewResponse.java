package com.vn.datphong.dto.response;

<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.enums.MediaTypeEnum;
import com.vn.datphong.enums.ReviewStatusType;
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
public class ReviewResponse {
    private Long id;

    private Long bookingId;
    private String bookingCode;

    private Long userId;
    private String userFullName;

    private Long roomId;
    private String roomName;

    private Short rating;
    private String comment;
    private ReviewStatusType status;

    /** Ảnh/video đính kèm review (MinIO url). */
    private List<MediaItem> media;

    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaItem {
        private String url;
        private MediaTypeEnum mediaType;
    }
}
