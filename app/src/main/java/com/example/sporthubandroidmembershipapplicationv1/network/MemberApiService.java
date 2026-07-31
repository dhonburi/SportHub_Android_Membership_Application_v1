package com.example.sporthubandroidmembershipapplicationv1.network;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberProfileResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface MemberApiService {

    @GET("api/members/{memberId}")
    Call<MemberProfileResponse> getMemberProfile(
            @Path("memberId") int memberId
    );
}