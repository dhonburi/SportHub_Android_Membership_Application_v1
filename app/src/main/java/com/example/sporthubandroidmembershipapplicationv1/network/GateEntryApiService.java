package com.example.sporthubandroidmembershipapplicationv1.network;

import com.example.sporthubandroidmembershipapplicationv1.models.GateEntryRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.GateEntryResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GateEntryApiService {

    @POST("api/gate-entry/process")
    Call<GateEntryResponse> processGateEntry(
            @Header("Authorization") String staffAuthorization,
            @Body GateEntryRequest request
    );
}
