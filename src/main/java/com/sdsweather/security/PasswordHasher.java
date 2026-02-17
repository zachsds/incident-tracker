package com.sdsweather.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordHasher - PBKDF2 password hashing and verification utility.
 *
 * Implements PBKDF2WithHmacSHA256 with per-password random salts for secure
 * password storage. Matches the algorithm used server-side for compatibility.
 *
 * Hash format: "{iterations}:{base64-salt}:{base64-hash}"
 *
 * Security parameters:
 *   Algorithm:  PBKDF2WithHmacSHA256
 *   Salt:       16 bytes, randomly generated per password
 *   Key length: 256 bits
 *   Iterations: 120,000 (OWASP recommended minimum)
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class PasswordHasher {

    private static final int saltLen = 16;
    private static final int keyLenBits = 256;
    private static final int iterations = 120_000;

    /**
     * Hashes a plaintext password using PBKDF2 with a random salt.
     *
     * @param password The plaintext password
     * @return Formatted string: "iterations:salt:hash"
     * @throws Exception If cryptographic operations fail
     */
    public static String hash(String password) throws Exception {

        byte[] salt = new byte[saltLen];
        new SecureRandom().nextBytes(salt);

        byte[] derived = pbkdf2(password.toCharArray(), salt, iterations, keyLenBits);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derived);

        return iterations + ":" + saltB64 + ":" + hashB64;
    }

    /**
     * Verifies a plaintext password against a stored hash.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param password The plaintext password to verify
     * @param stored   The stored hash in format "iterations:salt:hash"
     * @return true if the password matches, false otherwise
     * @throws Exception If cryptographic operations fail
     */
    public static boolean verify(String password, String stored) throws Exception {

        String[] parts = stored.split(":");
        if (parts.length != 3) return false;

        int it = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);

        byte[] actual = pbkdf2(password.toCharArray(), salt, it, expected.length * 8);

        return constantTimeEquals(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLenBits) throws Exception {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLenBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= (a[i] ^ b[i]);
        return r == 0;
    }
}
