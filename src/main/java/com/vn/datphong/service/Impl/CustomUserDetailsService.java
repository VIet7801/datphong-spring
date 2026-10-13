package com.vn.datphong.service.Impl;

import com.vn.datphong.entity.User;
import com.vn.datphong.exception.ResourceNotFoundExcept;
import com.vn.datphong.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user=userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundExcept("không tìm thấy user"));
//        Set<Role> hashSet=new HashSet<>();
//        hashSet.add(new Role(4L,"ROLE_ADMIN","df","df",new HashSet<>()));
//        user.setRoles(hashSet);
        // 2. Chuyển role trong database thành quyền của Spring Security
        var authorities = user.getRoles().stream()
                .map(role -> {
                    String code = role.getCode();

                    // Nếu role là USER thì chuyển thành ROLE_USER
//                    if (!code.startsWith("ROLE_")) {
//                        code = "ROLE_" + code;
//                    }

                    return new SimpleGrantedAuthority(code);
                })
                .toList();

        // 3. Chuyển User entity thành UserDetails
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}