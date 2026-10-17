package com.example.sporthubandroidmembershipapplicationv1.network;

import com.example.sporthubandroidmembershipapplicationv1.models.MembershipQrValidationRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.MembershipQrValidationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface MembershipQrApiService {

    @POST("api/membership-qr/validate")
    Call<MembershipQrValidationResponse> validateMembershipQr(
            @Body MembershipQrValidationRequest request
    );
}