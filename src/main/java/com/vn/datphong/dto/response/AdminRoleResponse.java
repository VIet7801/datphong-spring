package com.vn.datphong.dto.response;

import java.util.Set;

public record AdminRoleResponse(Long id, String code, String name, String description,
                                Set<String> permissions, Long version) { }
