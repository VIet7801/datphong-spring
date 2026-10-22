package com.vn.datphong.mapper;

import com.vn.datphong.dto.response.*;
import com.vn.datphong.entity.User;
import java.util.stream.Collectors;

public final class UserViewMapper {
    private UserViewMapper() { }

    public static UserProfileResponse profile(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getFullName(),
                user.getEmail(), user.getPhone(), user.getGender(), user.getAvatar(), user.getVersion());
    }

    public static AdminUserResponse admin(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getFullName(),
                maskEmail(user.getEmail()), maskPhone(user.getPhone()), user.getGender(),
                user.getStatus(), user.getActive(),
                user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet()), user.getVersion());
    }

    private static String maskEmail(String value) {
        if (value == null) return null;
        int at = value.lastIndexOf('@');
        return at > 0 ? value.charAt(0) + "***" + value.substring(at) : "[REDACTED]";
    }

    private static String maskPhone(String value) {
        if (value == null) return null;
        return value.length() > 3 ? "***" + value.substring(value.length() - 3) : "***";
    }
}
