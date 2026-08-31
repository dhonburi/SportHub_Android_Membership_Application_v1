package com.example.sporthubandroidmembershipapplicationv1.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    /*
     * Deployed Azure SportHub API.
     *
     * The API connects to Azure SQL.
     * Retrofit requires the final forward slash.
     */
    private static final String BASE_URL =
            "https://sporthub-api-jp-dfcndsfgh5d0bydt.japaneast-01.azurewebsites.net/";

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

    public static MemberApiService getMemberApiService() {
        return getClient().create(
                MemberApiService.class
        );
    }
}