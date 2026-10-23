package com.vn.datphong.dto.response;

import com.vn.datphong.enums.UserStatus;

public record RegistrationResponse(UserStatus status, String message) { }
