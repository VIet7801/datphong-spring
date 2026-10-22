package com.vn.datphong.dto.request;

import com.vn.datphong.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusUpdateRequest extends VersionedReasonRequest {
    @NotNull
    private UserStatus status;
}
