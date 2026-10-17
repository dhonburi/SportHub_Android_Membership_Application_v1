package com.example.sporthubandroidmembershipapplicationv1.models;

public class MembershipQrCodeResponse {

    private int memberMembershipId;
    private String planName;
    private String status;
    private String qrToken;
    private String issuedAtUtc;
    private String expiresAtUtc;
    private int validitySeconds;

    public int getMemberMembershipId() {
        return memberMembershipId;
    }

    public String getPlanName() {
        return planName;
    }

    public String getStatus() {
        return status;
    }

    public String getQrToken() {
        return qrToken;
    }

    public String getIssuedAtUtc() {
        return issuedAtUtc;
    }

    public String getExpiresAtUtc() {
        return expiresAtUtc;
    }

    public int getValiditySeconds() {
        return validitySeconds;
    }
}