package com.vn.datphong.mapper;


<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.dto.request.VoucherCreateRequest;
import com.vn.datphong.dto.response.VoucherResponse;
import com.vn.datphong.entity.Voucher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VoucherMapper {
    public static Voucher toEntity(VoucherCreateRequest req){
        return  Voucher.builder()
                .code(req.getCode())
//                .ownerType(req.getType())
                .owner(req.getUser())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minNights(req.getMinNights())
                .minOrderValue(req.getMinOrderValue())
                .targetRank(req.getTargetRank())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .usageLimitTotal(req.getUsageLimitTotal())
                .usageLimitPerUser(req.getUsageLimitPerUser())
                .build();

    }
    public static VoucherResponse toResponse(Voucher req) {

        return VoucherResponse.builder()
                .id(req.getId())
                .code(req.getCode())
//                .ownerType(req.getOwnerType())
//                .owner(req.getOwner())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minNights(req.getMinNights())
                .minOrderValue(req.getMinOrderValue())
                .targetRank(req.getTargetRank())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .usageLimitPerUser(req.getUsageLimitTotal())
                .usageLimitPerUser(req.getUsageLimitPerUser())
                .build();
    }

}
