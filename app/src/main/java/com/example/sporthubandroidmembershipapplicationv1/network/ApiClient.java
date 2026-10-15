package com.example.sporthubandroidmembershipapplicationv1.network;

import android.content.Context;
import java.util.Locale;
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
    private static Context applicationContext;
    private ApiClient() { }
    public static void initialize(Context context) { applicationContext = context.getApplicationContext(); }
    public static synchronized Retrofit getClient() {
        if (retrofit == null) {
            if (applicationContext == null) throw new IllegalStateException("SportHubApplication must initialise ApiClient.");
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
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
            retrofit = new Retrofit.Builder().client(client).baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
    public static AuthApiService getAuthApiService() { return getClient().create(AuthApiService.class); }
    public static MemberApiService getMemberApiService() { return getClient().create(MemberApiService.class); }
    public static MembershipQrApiService getMembershipQrApiService() { return getClient().create(MembershipQrApiService.class); }
    public static GateEntryApiService getGateEntryApiService() { return getClient().create(GateEntryApiService.class); }
}