package com.example.sporthubandroidmembershipapplicationv1.models;

public class LoginResponse {

    private boolean success;
    private Integer userId;
    private Integer memberId;
    private String memberNumber;
    private boolean isAdmin;
    private String staffAccessToken;
    private String staffAccessTokenExpiresAtUtc;
    private String message;

    public LoginResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getStaffAccessToken() {
        return staffAccessToken;
    }

    public void setStaffAccessToken(String staffAccessToken) {
        this.staffAccessToken = staffAccessToken;
    }

    public String getStaffAccessTokenExpiresAtUtc() {
        return staffAccessTokenExpiresAtUtc;
    }

    public void setStaffAccessTokenExpiresAtUtc(
            String staffAccessTokenExpiresAtUtc
    ) {
        this.staffAccessTokenExpiresAtUtc =
                staffAccessTokenExpiresAtUtc;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}