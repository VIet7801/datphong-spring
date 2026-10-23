package com.vn.datphong.dto.response;

import java.util.Set;

public record AuthTokenResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresIn, long refreshExpiresIn, UserSummary user) {
    public record UserSummary(Long id, String fullName, String username,
                              Set<String> roles, Set<String> permissions) { }
}
