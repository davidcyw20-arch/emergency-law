package com.yunxian.emergencylaw.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordMatcher {

    private PasswordMatcher() {
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (storedPassword.equals(rawPassword)) {
            return true;
        }

        String md5 = md5Hex(rawPassword);
        if (storedPassword.equalsIgnoreCase(md5)) {
            return true;
        }

        if (storedPassword.regionMatches(true, 0, "{MD5}", 0, 5)) {
            String digest = storedPassword.substring(5).trim();
            return digest.equalsIgnoreCase(md5);
        }

        return false;
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
