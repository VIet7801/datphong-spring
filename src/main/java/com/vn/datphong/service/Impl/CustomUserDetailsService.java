package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.User;
import com.vn.datphong.enums.UserStatus;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.repository.RolePermissionRepository;
import com.vn.datphong.security.AccountPrincipal;
import com.vn.datphong.security.Permissions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;

/**
 * Dùng chung cho hai luồng: DaoAuthenticationProvider khi login và JWT filter ở request sau.
 * Chỉ tải danh tính/quyền/trạng thái từ DB; provider/filter mới quyết định chấp nhận hay từ chối.
 * UserDetails là dữ liệu nội bộ cho Spring Security, không phải response DTO cho FE.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm đúng tài khoản từ username cần xác thực.
        User user=userRepository.findByUsername(username)
                // Đúng loại exception để login và JWT filter trả 401 khi user đã bị xóa.
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        // 2. Chuyển role trong database thành quyền của Spring Security
        var authorities = new LinkedHashSet<SimpleGrantedAuthority>();
        user.getRoles().stream().map(role -> role.getCode())
                .filter(Permissions::isRoleAuthority)
                .map(SimpleGrantedAuthority::new).forEach(authorities::add);
        // Permission là quyền nghiệp vụ, không bao giờ được giả dạng ROLE_ADMIN.
        rolePermissionRepository.findPermissionCodesByUserId(user.getId()).stream()
                .filter(Permissions::isPermissionAuthority)
                .map(SimpleGrantedAuthority::new).forEach(authorities::add);

        // 3. Chuyển User entity thành UserDetails
        return new AccountPrincipal(user, authorities);
    }
}
