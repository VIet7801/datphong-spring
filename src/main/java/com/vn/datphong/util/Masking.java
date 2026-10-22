package com.vn.datphong.util;

/** Che dữ liệu cá nhân của khách khi HOST chỉ cần nhận diện booking. */
public final class Masking {
    private Masking() {
    }

    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String shown = local.isEmpty() ? "" : local.substring(0, 1);
        return shown + "***" + email.substring(at);
    }

    public static String phone(String phone) {
        if (phone == null || phone.length() < 4) {
            return null;
        }
        return "*".repeat(phone.length() - 3) + phone.substring(phone.length() - 3);
    }
}