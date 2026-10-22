package com.vn.datphong.security;

import java.util.Set;

/** Mã permission phải trùng với dữ liệu trong bảng permissions. */
public final class Permissions {
    public static final String USER_READ = "USER_READ";
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String USER_ROLE_MANAGE = "USER_ROLE_MANAGE";
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String PERMISSION_READ = "PERMISSION_READ";
    public static final String PERMISSION_MANAGE = "PERMISSION_MANAGE";
    public static final String AUDIT_READ = "AUDIT_READ";

    public static final Set<String> BUILT_IN = Set.of(USER_READ, USER_MANAGE,
            USER_ROLE_MANAGE, ROLE_READ, ROLE_MANAGE, PERMISSION_READ,
            PERMISSION_MANAGE, AUDIT_READ);

    // Hai mã permission này đã dùng trong catalog; không được hiểu chúng là role.
    private static final Set<String> ROLE_CATALOG_PERMISSIONS = Set.of(ROLE_READ, ROLE_MANAGE);

    public static boolean isRoleAuthority(String code) {
        return code != null && code.startsWith("ROLE_") && !ROLE_CATALOG_PERMISSIONS.contains(code);
    }

    public static boolean isPermissionAuthority(String code) {
        return code != null && (!code.startsWith("ROLE_") || ROLE_CATALOG_PERMISSIONS.contains(code));
    }

    private Permissions() { }
}
