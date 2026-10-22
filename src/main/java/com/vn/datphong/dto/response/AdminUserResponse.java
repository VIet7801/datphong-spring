package com.vn.datphong.dto.response;

import com.vn.datphong.enums.UserStatus;
import java.util.Set;

/** Email và phone được che trước khi trả qua API quản trị. */
public record AdminUserResponse(Long id, String username, String fullName,
                                String email, String phone, Boolean gender,
                                UserStatus status, Boolean active,
                                Set<String> roles, Long version) { }
