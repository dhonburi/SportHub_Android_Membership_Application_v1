package com.example.sporthubandroidmembershipapplicationv1.security;

public final class SessionPolicy {
    public static final long LOGIN_LIFETIME_MS = 7L * 24 * 60 * 60 * 1000;
    public static final long BACKGROUND_GRACE_MS = 30_000;
    public static final int MAX_PIN_ATTEMPTS = 5;
    private SessionPolicy() { }
    public static boolean expired(long now, long expires) { return expires <= 0 || now >= expires; }
    public static boolean lockOnReturn(long awayMillis, boolean phoneLocked) {
        return phoneLocked || awayMillis < 0 || awayMillis >= BACKGROUND_GRACE_MS;
    }
}