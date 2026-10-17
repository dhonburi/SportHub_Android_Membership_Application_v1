package com.example.sporthubandroidmembershipapplicationv1.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.example.sporthubandroidmembershipapplicationv1.models.LoginResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;
import com.example.sporthubandroidmembershipapplicationv1.security.PinCrypto;
import com.example.sporthubandroidmembershipapplicationv1.security.SessionPolicy;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class MemberSession {
    private static final String PREFS = "sporthub_member_session";
    private static final String ALIAS = "SportHub.MemberSession.v1";
    private static final Object LOCK = new Object();
    private final Context context;
    private final SharedPreferences prefs;
    public MemberSession(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }
    private static String encode(byte[] v) { return Base64.encodeToString(v, Base64.NO_WRAP); }
    private static byte[] decode(String v) { return Base64.decode(v, Base64.NO_WRAP); }
    private JSONObject read() {
        String encoded = prefs.getString("encrypted_state", null);
        if (encoded == null) return new JSONObject();
        try {
            String[] pieces = encoded.split(":", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, decode(pieces[0])));
            return new JSONObject(new String(cipher.doFinal(decode(pieces[1])), StandardCharsets.UTF_8));
        } catch (Exception error) { prefs.edit().clear().commit(); return new JSONObject(); }
    }
    private void write(JSONObject state) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            String encrypted = encode(cipher.getIV()) + ":" + encode(cipher.doFinal(state.toString().getBytes(StandardCharsets.UTF_8)));
            if (!prefs.edit().clear().putString("encrypted_state", encrypted).commit())
                throw new IllegalStateException("Cannot save device session");
        } catch (Exception error) {
            prefs.edit().clear().commit();
            throw new IllegalStateException("Cannot save the secure device session. Please sign in again.", error);
        }
    }
    private int boot() { return Settings.Global.getInt(context.getContentResolver(), "boot_count", -1); }
    private void anchor(JSONObject state, long now) throws org.json.JSONException {
        state.put("server_anchor", now); state.put("wall_anchor", System.currentTimeMillis());
        state.put("elapsed_anchor", SystemClock.elapsedRealtime()); state.put("boot", boot());
    }
    public void saveLogin(LoginResponse response, String displayName) {
        synchronized (LOCK) {
            if (response.getMemberId() == null || response.getMemberId() <= 0 || response.getMemberAccessToken() == null
                    || response.getMemberAccessToken().length() != 64 || response.getMemberSessionExpiresAtUnixMs() == null
                    || response.getServerTimeUnixMs() == null || response.getMemberSessionExpiresAtUnixMs() <= response.getServerTimeUnixMs())
                throw new IllegalStateException("The API session update is not deployed. Deploy the US-15 backend first.");
            try {
                JSONObject old = read(), state = new JSONObject();
                if (old.optInt("pin_owner", -1) == response.getMemberId() && old.has("pin_hash"))
                    for (String field : new String[]{"pin_owner", "pin_salt", "pin_hash"}) state.put(field, old.get(field));
                state.put("member_id", response.getMemberId()); state.put("member_number", response.getMemberNumber());
                state.put("display_name", displayName); state.put("is_admin", response.isAdmin());
                state.put("member_token", response.getMemberAccessToken()); state.put("staff_token", response.getStaffAccessToken());
                state.put("expires_at", response.getMemberSessionExpiresAtUnixMs()); state.put("failures", 0);
                anchor(state, response.getServerTimeUnixMs()); write(state);
            } catch (org.json.JSONException error) { throw new IllegalStateException(error); }
        }
    }
    public void updateServerTime(long now) {
        synchronized (LOCK) {
            try { JSONObject state = read(); anchor(state, now); write(state); }
            catch (org.json.JSONException error) { throw new IllegalStateException(error); }
        }
    }
    public long estimatedServerNow() {
        synchronized (LOCK) {
            JSONObject state = read(); long delta = SystemClock.elapsedRealtime() - state.optLong("elapsed_anchor");
            if (state.optInt("boot", -2) == boot() && delta >= 0) return state.optLong("server_anchor") + delta;
            long wallDelta = System.currentTimeMillis() - state.optLong("wall_anchor");
            return wallDelta < -120_000 ? Long.MAX_VALUE : state.optLong("server_anchor") + Math.max(0, wallDelta);
        }
    }
    public long getExpiresAt() { synchronized (LOCK) { return read().optLong("expires_at"); } }
    public int getMemberId() { synchronized (LOCK) { return read().optInt("member_id", -1); } }
    public String getMemberNumber() { synchronized (LOCK) { return read().optString("member_number", ""); } }
    public String getDisplayName() { synchronized (LOCK) { return read().optString("display_name", "Member"); } }
    public boolean isAdmin() { synchronized (LOCK) { return read().optBoolean("is_admin", false); } }
    public String getStaffAccessToken() { synchronized (LOCK) { return read().optString("staff_token", ""); } }
    public String getMemberAccessToken() { synchronized (LOCK) { return read().optString("member_token", ""); } }
    public boolean hasLogin() { return getMemberId() > 0 && !getMemberAccessToken().isEmpty(); }
    public boolean isExpired() { return !hasLogin() || SessionPolicy.expired(estimatedServerNow(), getExpiresAt()); }
    public boolean hasPin() {
        synchronized (LOCK) {
            JSONObject state = read();
            return state.optInt("pin_owner", -1) == state.optInt("member_id", -2) && state.has("pin_hash");
        }
    }
    public void setPin(String pin, String expectedToken) throws Exception {
        if (!PinCrypto.valid(pin)) throw new IllegalArgumentException("Enter exactly six digits.");
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        byte[] hash = PinCrypto.derive(pin, salt, PinCrypto.ITERATIONS);
        try {
            synchronized (LOCK) {
                JSONObject state = read();
                // Do not attach a hash from an old screen to a newer login or another account.
                if (expectedToken == null || !expectedToken.equals(state.optString("member_token")) || isExpired())
                    throw new IllegalStateException("Sign in again before setting a PIN.");
                state.put("pin_owner", state.getInt("member_id")); state.put("pin_salt", encode(salt));
                state.put("pin_hash", encode(hash)); state.put("failures", 0); write(state);
            }
        } finally { Arrays.fill(hash, (byte) 0); Arrays.fill(salt, (byte) 0); }
    }
    public boolean verifyPin(String pin) throws Exception {
        if (!PinCrypto.valid(pin)) return false;
        JSONObject snapshot;
        synchronized (LOCK) {
            snapshot = read();
            if (isExpired() || !hasPin() || snapshot.optInt("failures") >= SessionPolicy.MAX_PIN_ATTEMPTS) return false;
        }
        byte[] hash = PinCrypto.derive(pin, decode(snapshot.getString("pin_salt")), PinCrypto.ITERATIONS);
        boolean matches;
        try { matches = MessageDigest.isEqual(hash, decode(snapshot.getString("pin_hash"))); }
        finally { Arrays.fill(hash, (byte) 0); }
        synchronized (LOCK) {
            JSONObject state = read();
            if (!snapshot.optString("member_token").equals(state.optString("member_token")) || isExpired()) return false;
            state.put("failures", matches ? 0 : state.optInt("failures") + 1); write(state);
            if (!matches && state.optInt("failures") >= SessionPolicy.MAX_PIN_ATTEMPTS) invalidate();
        }
        return matches;
    }
    public int attemptsRemaining() {
        synchronized (LOCK) { return Math.max(0, SessionPolicy.MAX_PIN_ATTEMPTS - read().optInt("failures")); }
    }
    public void invalidate() {
        synchronized (LOCK) {
            JSONObject old = read(), retained = new JSONObject();
            try {
                for (String field : new String[]{"pin_owner", "pin_salt", "pin_hash", "failures"})
                    if (old.has(field)) retained.put(field, old.get(field));
                write(retained);
            } catch (org.json.JSONException error) { prefs.edit().clear().commit(); }
        }
    }
    public void clear() {
        String token = getMemberAccessToken();
        synchronized (LOCK) { prefs.edit().clear().commit(); }
        // Local logout is immediate. Server revocation requires a successful network call.
        if (!token.isEmpty()) ApiClient.getAuthApiService().logout("Bearer " + token).enqueue(new Callback<Void>() {
            public void onResponse(Call<Void> call, Response<Void> response) { }
            public void onFailure(Call<Void> call, Throwable error) { }
        });
    }
}