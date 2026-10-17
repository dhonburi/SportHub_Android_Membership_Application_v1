package com.example.sporthubandroidmembershipapplicationv1.models;

import com.google.gson.annotations.SerializedName;

public class MembershipQrValidationRequest {

    @SerializedName("qrToken")
    private final String qrToken;

    public MembershipQrValidationRequest(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getQrToken() {
        return qrToken;
    }
}