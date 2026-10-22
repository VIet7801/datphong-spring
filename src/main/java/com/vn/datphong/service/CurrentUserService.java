package com.vn.datphong.service;

import com.vn.datphong.entity.User;

public interface CurrentUserService {

    String username();

    User require();

    boolean hasAuthority(String authority);

    boolean isAdmin();

    boolean isSupportOrAdmin();
}
