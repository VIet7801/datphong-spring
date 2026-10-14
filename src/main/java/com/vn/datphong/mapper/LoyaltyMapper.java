package com.vn.datphong.mapper;

<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.dto.response.LoyaltyRankResponse;
import com.vn.datphong.entity.LoyaltyRank;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoyaltyMapper {

    public static LoyaltyRankResponse toResponse(LoyaltyRank e) {
        return LoyaltyRankResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .minPoints(e.getMinPoints())
                .discountPercent(e.getDiscountPercent())
                .benefits(e.getBenefits())
                .build();
    }
}
