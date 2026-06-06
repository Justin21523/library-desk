package com.justin.libradesk.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Password hashing using BCrypt (salted, adaptive work factor).
 *
 * <p>Hashes created before this project moved off SHA-256 are still recognised
 * by {@link #matches} so existing accounts keep working; {@link #needsRehash}
 * lets the auth flow transparently upgrade them to BCrypt on the next login.
 */
public final class PasswordHasher {

    /** BCrypt cost factor (2^cost rounds). 12 is a reasonable desktop default. */
    private static final int COST = 12;

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            return BCrypt.verifyer().verify(rawPassword.toCharArray(), storedHash).verified;
        }
        return legacyMatches(rawPassword, storedHash);
    }

    /** @return true if the stored hash is a legacy (non-BCrypt) hash that should be upgraded. */
    public static boolean needsRehash(String storedHash) {
        return storedHash == null || !isBcrypt(storedHash);
    }

    private static boolean isBcrypt(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    /** Legacy SHA-256 hex comparison, kept only to verify pre-existing accounts. */
    private static boolean legacyMatches(String rawPassword, String storedHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed).equalsIgnoreCase(storedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
