package com.vn.datphong.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserRolesUpdateRequest extends VersionedReasonRequest {
    @NotEmpty
    @Size(max = 30)
    private Set<@NotNull @Positive Long> roleIds;
}
