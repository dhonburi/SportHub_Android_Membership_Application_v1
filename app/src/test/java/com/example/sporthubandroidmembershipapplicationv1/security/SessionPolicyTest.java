package com.example.sporthubandroidmembershipapplicationv1.security;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionPolicyTest {
    @Test public void sevenDayBoundary() {
        long expires = SessionPolicy.LOGIN_LIFETIME_MS;
        assertEquals(168L * 60 * 60 * 1000, expires);
        assertFalse(SessionPolicy.expired(expires - 1, expires)); assertTrue(SessionPolicy.expired(expires, expires));
    }
    @Test public void graceAndScreenLock() {
        assertFalse(SessionPolicy.lockOnReturn(29_999, false)); assertTrue(SessionPolicy.lockOnReturn(30_000, false));
        assertTrue(SessionPolicy.lockOnReturn(1, true)); assertTrue(SessionPolicy.lockOnReturn(-1, false));
        assertEquals(5, SessionPolicy.MAX_PIN_ATTEMPTS);
    }
    @Test public void sixDigits() {
        assertTrue(PinCrypto.valid("001234")); assertFalse(PinCrypto.valid("12345"));
        assertFalse(PinCrypto.valid("1234567")); assertFalse(PinCrypto.valid("ABC123"));
    }
    @Test public void independentPbkdf2Vector() throws Exception {
        byte[] hash = PinCrypto.derive("001234", "0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 1000);
        StringBuilder hex = new StringBuilder();
        for (byte value : hash) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        assertEquals("37328269c5ba3ec80e269fe0da66aa352bc33f56fe5b7327fb5065b28aad255b", hex.toString());
    }
}