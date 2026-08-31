package com.example.sporthubandroidmembershipapplicationv1.network;

import com.example.sporthubandroidmembershipapplicationv1.models.BalanceQrCodeResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.MemberMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.MemberProfileResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.MembershipPlanResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.MembershipQrCodeResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.PurchaseMembershipRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.PurchaseMembershipResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.TopUpBalanceRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.TopUpBalanceResponse;
import com.example.sporthubandroidmembershipapplicationv1.models.UpdateMemberProfileRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
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

    @POST("api/members/{memberId}/balance/top-up")
    Call<TopUpBalanceResponse> topUpMemberBalance(
            @Path("memberId") int memberId,
            @Body TopUpBalanceRequest request
    );

    @GET("api/members/{memberId}/balance/qr-code")
    Call<BalanceQrCodeResponse> getBalanceQrCode(
            @Path("memberId") int memberId
    );

    @GET("api/members/{memberId}/membership-plans")
    Call<List<MembershipPlanResponse>> getMembershipPlans(
            @Path("memberId") int memberId
    );

    @POST("api/members/{memberId}/memberships/purchase")
    Call<PurchaseMembershipResponse> purchaseMembership(
            @Path("memberId") int memberId,
            @Body PurchaseMembershipRequest request
    );

    @GET("api/members/{memberId}/membership")
    Call<MemberMembershipResponse> getMemberMembership(
            @Path("memberId") int memberId
    );

    @GET("api/members/{memberId}/memberships")
    Call<List<MemberMembershipResponse>> getMemberMemberships(
            @Path("memberId") int memberId
    );

    @GET("api/members/{memberId}/memberships/{memberMembershipId}/qr-code")
    Call<MembershipQrCodeResponse> getMembershipQrCode(
            @Path("memberId") int memberId,
            @Path("memberMembershipId") int memberMembershipId
    );
}