package com.vn.datphong.security;

import com.vn.datphong.entity.User;
import com.vn.datphong.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

/** Danh tính nội bộ; không serialize principal này thành API response. */
public class AccountPrincipal extends org.springframework.security.core.userdetails.User {
    private final Long userId;
    private final Long tokenVersion;

    public AccountPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        super(user.getUsername(), user.getPassword(),
                Boolean.TRUE.equals(user.getActive()) && user.getStatus() == UserStatus.ACTIVE
                        && user.getEmailVerifiedAt() != null,
                true, true, user.getStatus() != UserStatus.LOCKED, authorities);
        this.userId = user.getId();
        this.tokenVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
    }

    public Long getUserId() { return userId; }
    public Long getTokenVersion() { return tokenVersion; }
}
