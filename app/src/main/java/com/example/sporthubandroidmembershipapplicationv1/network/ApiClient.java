package com.example.sporthubandroidmembershipapplicationv1.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    /*
     * Dhon's Windows laptop address while both computers
     * are connected to the same phone hotspot.
     *
     * This IP may change after reconnecting to the hotspot.
     */
    private static final String BASE_URL =
            "http://172.20.10.3:5097/";

    private static Retrofit retrofit;

    private ApiClient() {
        // Prevent this utility class from being instantiated.
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(
                            GsonConverterFactory.create()
                    )
                    .build();
        }

        return retrofit;
    }

    public static AuthApiService getAuthApiService() {
        return getClient().create(
                AuthApiService.class
        );
    }
}