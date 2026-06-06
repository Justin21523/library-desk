package com.justin.libradesk.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Minimal password hashing helper for Phase 1.
 *
 * <p>TODO(security): replace SHA-256 with a salted, slow KDF (BCrypt/Argon2)
 * before this project is used beyond a learning/portfolio context. SHA-256 is
 * used here only to avoid storing plaintext while keeping Phase 1 dependency-free.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean matches(String rawPassword, String expectedHash) {
        return hash(rawPassword).equalsIgnoreCase(expectedHash);
    }
}
