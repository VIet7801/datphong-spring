package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.User;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {
    private final UserRepository userRepository;
    private Authentication authentication() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw DomainException.unauthorized("UNAUTHORIZED", "Chưa xác thực");
        }
        return a;
    }

    @Override
    public String username() {
        return authentication().getName();
    }

    @Override
    public User require() {
        String username = username();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> DomainException.unauthorized("UNAUTHORIZED", "Tài khoản không còn tồn tại"));
    }

    @Override
    public boolean hasAuthority(String authority) {
        return authentication().getAuthorities().stream().anyMatch(g -> authority.equals(g.getAuthority()));
    }

    @Override
    public boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }

    @Override
    public boolean isSupportOrAdmin() {
        return hasAuthority("ROLE_SUPPORT") || hasAuthority("ROLE_ADMIN");
    }
}
