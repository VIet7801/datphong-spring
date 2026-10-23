package com.vn.datphong.util;

import java.util.Locale;

public final class EmailAddress {
    private EmailAddress() { }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
