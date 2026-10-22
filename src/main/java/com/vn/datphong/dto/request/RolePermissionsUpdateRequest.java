package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class RolePermissionsUpdateRequest extends VersionedReasonRequest {
    @NotNull
    @Size(max = 200)
    private Set<@NotNull @Positive Long> permissionIds;
}
