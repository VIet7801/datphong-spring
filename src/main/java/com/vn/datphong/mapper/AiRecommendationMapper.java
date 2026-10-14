package com.vn.datphong.mapper;

<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.dto.response.AiRecommendationResponse;
import com.vn.datphong.entity.AI_Recommendations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AiRecommendationMapper {

    public static AiRecommendationResponse toResponse(AI_Recommendations e) {
        return AiRecommendationResponse.builder()
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .roomId(e.getRoom() != null ? e.getRoom().getId() : null)
                .roomName(e.getRoom() != null ? e.getRoom().getName() : null)
                .roomCode(e.getRoom() != null ? e.getRoom().getCode() : null)
                .score(e.getScore())
                .modelVersion(e.getModelVersion())
                .generatedAt(e.getGeneratedAt())
                .build();
    }
}
