package com.example.sporthubandroidmembershipapplicationv1.network;

import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.MemberProfileResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.UpdateMemberProfileRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface MemberApiService {

    @GET("api/members/{memberId}")
    Call<MemberProfileResponse> getMemberProfile(
            @Path("memberId") int memberId
    );

    @PUT("api/members/{memberId}")
    Call<MemberProfileResponse> updateMemberProfile(
            @Path("memberId") int memberId,
            @Body UpdateMemberProfileRequest request
    );

    @GET("api/members/{memberId}/membership")
    Call<MemberMembershipResponse> getMemberMembership(
            @Path("memberId") int memberId
    );
}