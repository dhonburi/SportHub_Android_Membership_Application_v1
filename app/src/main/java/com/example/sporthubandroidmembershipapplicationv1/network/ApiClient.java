package com.example.sporthubandroidmembershipapplicationv1.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    /*
     * 10.0.2.2 allows the Android emulator to access a backend
     * running on this Mac.
     *
     * Later, when connecting to Dhon's Windows backend,
     * replace this with Dhon's IPv4 address and API port.
     */
    private static final String BASE_URL = "http://10.0.2.2:5097/";

    private static Retrofit retrofit;

    private ApiClient() {
        // Prevent this utility class from being instantiated.
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static AuthApiService getAuthApiService() {
        return getClient().create(AuthApiService.class);
    }
}