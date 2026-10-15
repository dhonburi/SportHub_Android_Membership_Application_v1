package com.example.sporthubandroidmembershipapplicationv1.network;

import android.content.Context;
import android.os.SystemClock;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import com.example.sporthubandroidmembershipapplicationv1.session.MemberSession;
import com.example.sporthubandroidmembershipapplicationv1.security.AppLockManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final String BASE_URL = "https://sporthub-api-jp-dfcndsfgh5d0bydt.japaneast-01.azurewebsites.net/";
    private static Retrofit retrofit;
    private static Retrofit authRetrofit;
    private static OkHttpClient sharedClient;
    private static OkHttpClient authClient;
    private static long lastWarmupAttemptMs = -1L;
    private static final long WARMUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);
    private static Context applicationContext;
    private ApiClient() { }
    public static void initialize(Context context) { applicationContext = context.getApplicationContext(); }
    public static synchronized Retrofit getClient() {
        if (retrofit == null) {
            if (applicationContext == null) throw new IllegalStateException("SportHubApplication must initialise ApiClient.");
            sharedClient = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request original = chain.request();
                String path = original.url().encodedPath().toLowerCase(Locale.ROOT);
                boolean memberRoute = path.startsWith("/api/members/") || path.equals("/api/auth/session");
                String token = "";
                Request request = original;
                if (memberRoute) {
                    MemberSession session = new MemberSession(applicationContext); token = session.getMemberAccessToken();
                    if (token.isEmpty() || session.isExpired()) {
                        AppLockManager.get().authenticationRejected(token);
                        throw new java.io.IOException("Sign in again to continue.");
                    }
                    if (path.startsWith("/api/members/") && !AppLockManager.get().isUnlocked())
                        throw new java.io.IOException("Unlock the application before accessing member information.");
                    request = original.newBuilder().header("Authorization", "Bearer " + token).build();
                }
                Response response = chain.proceed(request);
                if (memberRoute && response.code() == 401) AppLockManager.get().authenticationRejected(token);
                return response;
            }).build();
            retrofit = new Retrofit.Builder().client(sharedClient).baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
    private static synchronized Retrofit getAuthClient() {
        getClient();
        if (authRetrofit == null) {
            // Share connections with the normal client, but allow a bounded
            // startup window for account login and live PIN-session checks.
            // This does not add automatic application-level POST retries.
            authClient = sharedClient.newBuilder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .build();
            authRetrofit = new Retrofit.Builder().client(authClient).baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()).build();
        }
        return authRetrofit;
    }

    public static void warmUpAuthenticationService() {
        OkHttpClient client;
        synchronized (ApiClient.class) {
            long now = SystemClock.elapsedRealtime();
            if (lastWarmupAttemptMs >= 0
                    && now - lastWarmupAttemptMs < WARMUP_INTERVAL_MS) return;
            getAuthClient();
            lastWarmupAttemptMs = now;
            client = authClient;
        }

        // Start the public web application while the member fills in the form.
        // No credentials are sent. This is best-effort application/connection
        // warm-up, not a database-readiness or authentication check.
        Request request = new Request.Builder()
                .url(BASE_URL + "swagger/v1/swagger.json")
                .head()
                .build();
        okhttp3.Call call = client.newCall(request);
        call.timeout().timeout(15, TimeUnit.SECONDS);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException error) {
                // A failed warm-up must never block an actual sign-in attempt.
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                response.close();
            }
        });
    }

    public static AuthApiService getAuthApiService() { return getAuthClient().create(AuthApiService.class); }
    public static MemberApiService getMemberApiService() { return getClient().create(MemberApiService.class); }
    public static MembershipQrApiService getMembershipQrApiService() { return getClient().create(MembershipQrApiService.class); }
    public static GateEntryApiService getGateEntryApiService() { return getClient().create(GateEntryApiService.class); }
}
