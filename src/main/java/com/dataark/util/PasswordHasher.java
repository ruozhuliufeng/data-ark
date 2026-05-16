package com.dataark.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "sha256$" + base64(salt) + "$" + digest(salt, password);
    }

    public static boolean verify(String password, String encoded) {
        if (encoded == null || !encoded.startsWith("sha256$")) {
            return false;
        }
        String[] parts = encoded.split("\\$");
        if (parts.length != 3) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        return digest(salt, password).equals(parts[2]);
    }

    private static String digest(byte[] salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            return base64(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Password hash failed", e);
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
