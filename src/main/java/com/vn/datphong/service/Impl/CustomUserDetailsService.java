package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.User;
import com.vn.datphong.enums.UserStatus;
import com.vn.datphong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Dùng chung cho hai luồng: DaoAuthenticationProvider khi login và JWT filter ở request sau.
 * Chỉ tải danh tính/quyền/trạng thái từ DB; provider/filter mới quyết định chấp nhận hay từ chối.
 * UserDetails là dữ liệu nội bộ cho Spring Security, không phải response DTO cho FE.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm đúng tài khoản từ username cần xác thực.
        User user=userRepository.findByUsername(username)
                // Đúng loại exception để login và JWT filter trả 401 khi user đã bị xóa.
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        // 2. Chuyển role trong database thành quyền của Spring Security
        var authorities = user.getRoles().stream()
                .map(role -> {
                    String code = role.getCode();

                    return new SimpleGrantedAuthority(code);
                })
                .toList();

        // 3. Chuyển User entity thành UserDetails
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                // Chỉ tài khoản có active=true và status=ACTIVE mới được xác thực.
                // Boolean.TRUE.equals(...) xử lý cả dữ liệu active=null theo hướng từ chối.
                .disabled(!Boolean.TRUE.equals(user.getActive()) || user.getStatus() != UserStatus.ACTIVE)
                // LOCKED được ánh xạ riêng để Spring Security nhận biết tài khoản bị khóa.
                .accountLocked(user.getStatus() == UserStatus.LOCKED)
                .build();
    }
}
