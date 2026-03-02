package com.yunxian.emergencylaw.security;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordMatcher {

    private PasswordMatcher() {
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        String raw = rawPassword.trim();
        String stored = storedPassword.trim();
        if (raw.isEmpty() || stored.isEmpty()) {
            return false;
        }

        if (stored.equals(raw)) {
            return true;
        }

        if (isBcryptHash(stored) && BCrypt.checkpw(raw, stored)) {
            return true;
        }

        String md5 = md5Hex(raw);
        if (stored.equalsIgnoreCase(md5)) {
            return true;
        }

        if (stored.regionMatches(true, 0, "{MD5}", 0, 5)) {
            String digest = stored.substring(5).trim();
            return digest.equalsIgnoreCase(md5);
        }

        return false;
    }

    private static boolean isBcryptHash(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("md5 unavailable", e);
        }
    }
}
