package com.vn.datphong.security;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AuthEndpoints {
    public static final Set<String> PUBLIC_POSTS = Stream.of("/api/v1/auth", "/api/auth")
            .flatMap(base -> Stream.of("/register", "/verify", "/resend-verification",
                    "/login", "/refresh", "/refresh-token", "/logout").map(path -> base + path))
            .collect(Collectors.toUnmodifiableSet());

    private AuthEndpoints() { }
}
