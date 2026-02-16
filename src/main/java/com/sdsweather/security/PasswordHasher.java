package com.sdsweather.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {

    private static final int saltLen = 16;
    private static final int keyLenBits = 256;
    private static final int iterations = 120_000;

    public static String hash(String password) throws Exception {

        byte[] salt = new byte[saltLen];
        new SecureRandom().nextBytes(salt);

        byte[] derived = pbkdf2(password.toCharArray(), salt, iterations, keyLenBits);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derived);

        return iterations + ":" + saltB64 + ":" + hashB64;
    }

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
