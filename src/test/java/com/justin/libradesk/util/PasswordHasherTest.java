package com.justin.libradesk.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void bcryptHashVerifiesAndDoesNotNeedRehash() {
        String hash = PasswordHasher.hash("secret");

        assertTrue(hash.startsWith("$2"));
        assertTrue(PasswordHasher.matches("secret", hash));
        assertFalse(PasswordHasher.matches("wrong", hash));
        assertFalse(PasswordHasher.needsRehash(hash));
    }

    @Test
    void legacySha256StillVerifiesButNeedsRehash() throws Exception {
        String legacy = sha256Hex("secret");

        assertTrue(PasswordHasher.matches("secret", legacy));
        assertFalse(PasswordHasher.matches("wrong", legacy));
        assertTrue(PasswordHasher.needsRehash(legacy));
    }

    private static String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
