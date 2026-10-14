package com.vn.datphong.mapper;

<<<<<<< HEAD

=======
>>>>>>> c6cf529492187948577f3f0b5dd5299a95e510d7
import com.vn.datphong.dto.request.RoleCreateRequest;
import com.vn.datphong.dto.response.RoleResponse;
import com.vn.datphong.entity.Role;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoleMapper {

    public static Role toEntity(RoleCreateRequest req) {
        Role role = new Role();
        role.setCode(req.getCode());
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        return role;
    }

    public static RoleResponse toResponse(Role e) {
        return RoleResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .build();
    }
}
