package com.example.sporthubandroidmembershipapplicationv1.models;

import com.google.gson.annotations.SerializedName;

public class MembershipQrValidationResponse {

    @SerializedName("isApproved")
    private boolean isApproved;

    @SerializedName("decision")
    private String decision;

    @SerializedName("resultCode")
    private String resultCode;

    @SerializedName("message")
    private String message;

    @SerializedName("accessType")
    private String accessType;

    @SerializedName("memberNumber")
    private String memberNumber;

    @SerializedName("planName")
    private String planName;

    @SerializedName("membershipStatus")
    private String membershipStatus;

    @SerializedName("remainingEntries")
    private Integer remainingEntries;

    public boolean isApproved() {
        return isApproved;
    }

    public String getDecision() {
        return decision;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public String getAccessType() {
        return accessType;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public String getPlanName() {
        return planName;
    }

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public Integer getRemainingEntries() {
        return remainingEntries;
    }
}