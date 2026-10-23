package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.User;
import com.vn.datphong.repository.*;
import com.vn.datphong.security.*;
import com.vn.datphong.util.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identity) {
        User user = (identity.contains("@")
                ? userRepository.findByNormalizedEmail(EmailAddress.normalize(identity))
                : userRepository.findByUsername(identity))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return principal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        return principal(userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản")));
    }

    public AccountPrincipal principal(User user) {
        var authorities = new LinkedHashSet<SimpleGrantedAuthority>();
        user.getRoles().stream().map(role -> role.getCode()).filter(Permissions::isRoleAuthority)
                .map(SimpleGrantedAuthority::new).forEach(authorities::add);
        rolePermissionRepository.findPermissionCodesByUserId(user.getId()).stream()
                .filter(Permissions::isPermissionAuthority)
                .map(SimpleGrantedAuthority::new).forEach(authorities::add);
        return new AccountPrincipal(user, authorities);
    }
}
