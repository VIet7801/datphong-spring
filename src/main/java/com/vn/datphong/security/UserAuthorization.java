package com.vn.datphong.security;

import com.vn.datphong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userAuthorization")
@RequiredArgsConstructor
public class UserAuthorization {
    private final UserRepository userRepository;

    public boolean isOwner(Long id, Authentication authentication) {
        if (id == null || authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        // Username lấy từ Authentication đã xác thực, không lấy từ request body.
        return userRepository.existsByIdAndUsername(id, authentication.getName());
    }
}
