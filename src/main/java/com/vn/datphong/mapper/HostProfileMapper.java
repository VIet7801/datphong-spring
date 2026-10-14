package com.vn.datphong.mapper;

import com.vn.datphong.dto.request.HostProfileRequest;
import com.vn.datphong.dto.response.HostProfileResponse;
import com.vn.datphong.entity.HostProfile;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.HostStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HostProfileMapper {

    public static HostProfile toEntity(
            HostProfileRequest request,
            User user
    ) {
        return HostProfile.builder()
                .user(user)
                .displayName(request.getDisplayName())
                .businessName(request.getBusinessName())
                .bio(request.getBio())
                .hostStatus(HostStatus.PENDING)
                .autoBookingDefault(
                        request.getAutoBookingDefault() != null
                                ? request.getAutoBookingDefault()
                                : false
                )
                .active(true)
                .build();
    }

    public static HostProfileResponse toResponse(
            HostProfile hostProfile
    ) {
        return HostProfileResponse.builder()
                .id(hostProfile.getId())
                .username(hostProfile.getUser().getUsername())
                .displayName(hostProfile.getDisplayName())
                .businessName(hostProfile.getBusinessName())
                .bio(hostProfile.getBio())
                .hostStatus(hostProfile.getHostStatus())
                .autoBookingDefault(hostProfile.getAutoBookingDefault())
                .active(hostProfile.getActive())
                .build();
    }
}