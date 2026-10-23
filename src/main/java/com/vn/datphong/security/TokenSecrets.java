package com.vn.datphong.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Token ngẫu nhiên 256 bit. Bảng xác minh/refresh chỉ lưu SHA-256, không lưu token gốc. */
public final class TokenSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenSecrets() { }

    public static String create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static boolean matches(String raw, String hash) {
        return MessageDigest.isEqual(hash(raw).getBytes(StandardCharsets.US_ASCII),
                hash.getBytes(StandardCharsets.US_ASCII));
    }
}
