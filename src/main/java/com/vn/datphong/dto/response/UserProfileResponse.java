package com.vn.datphong.dto.response;

/** Hồ sơ cá nhân không công bố role, trạng thái quản trị hay dữ liệu xác thực. */
public record UserProfileResponse(Long id, String username, String fullName,
                                  String email, String phone, Boolean gender,
                                  String avatar, Long version) { }
