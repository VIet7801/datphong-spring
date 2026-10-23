package com.vn.datphong.security;

import com.vn.datphong.entity.User;
import com.vn.datphong.enums.UserStatus;
import com.vn.datphong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository users;

    public User require() {
        return load(false);
    }

    public User requireForUpdate() {
        return load(true);
    }

    private User load(boolean lock) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Cần đăng nhập");
        }
        User user = (lock ? users.findByUsernameForUpdate(authentication.getName())
                : users.findByUsername(authentication.getName()))
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Tài khoản không tồn tại"));
        if (!Boolean.TRUE.equals(user.getActive()) || user.getStatus() != UserStatus.ACTIVE
                || user.getEmailVerifiedAt() == null) {
            throw new AuthenticationCredentialsNotFoundException("Tài khoản không được phép truy cập");
        }
        return user;
    }
}
