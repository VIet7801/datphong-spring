package com.vn.datphong.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationResponse {
    private Long userId;

    private Long roomId;
    private String roomName;
    private String roomCode;

    private Double score;
    private String modelVersion;
    private LocalDateTime generatedAt;
}
