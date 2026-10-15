package com.example.sporthubandroidmembershipapplicationv1.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PinCrypto {
    public static final int ITERATIONS = 600_000;
    private PinCrypto() { }
    public static boolean valid(String pin) { return pin != null && pin.matches("[0-9]{6}"); }
    // PBKDF2-HMAC-SHA256 without relying on the API-26 SecretKeyFactory provider.
    public static byte[] derive(String pin, byte[] salt, int iterations) throws GeneralSecurityException {
        if (iterations <= 0) throw new IllegalArgumentException("Invalid iterations");
        byte[] password = pin.getBytes(StandardCharsets.UTF_8);
        byte[] input = Arrays.copyOf(salt, salt.length + 4);
        input[input.length - 1] = 1;
        byte[] block = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(password, "HmacSHA256"));
            block = mac.doFinal(input);
            byte[] result = block.clone();
            for (int i = 1; i < iterations; i++) {
                byte[] next = mac.doFinal(block);
                Arrays.fill(block, (byte) 0); block = next;
                for (int j = 0; j < result.length; j++) result[j] ^= block[j];
            }
            return result;
        } finally {
            Arrays.fill(password, (byte) 0); Arrays.fill(input, (byte) 0);
            if (block != null) Arrays.fill(block, (byte) 0);
        }
    }
}