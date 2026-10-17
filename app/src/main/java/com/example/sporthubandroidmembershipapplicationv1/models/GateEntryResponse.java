package com.example.sporthubandroidmembershipapplicationv1.models;

import com.google.gson.annotations.SerializedName;

public class GateEntryResponse {

    @SerializedName("isApproved")
    private boolean isApproved;

    @SerializedName("isDuplicate")
    private boolean isDuplicate;

    @SerializedName("decision")
    private String decision;

    @SerializedName("resultCode")
    private String resultCode;

    @SerializedName("message")
    private String message;

    @SerializedName("processingId")
    private String processingId;

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

    @SerializedName("amountCharged")
    private Double amountCharged;

    @SerializedName("balance")
    private Double balance;

    @SerializedName("currency")
    private String currency;

    @SerializedName("processedAtUtc")
    private String processedAtUtc;

    public boolean isApproved() {
        return isApproved;
    }

    public boolean isDuplicate() {
        return isDuplicate;
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

    public String getProcessingId() {
        return processingId;
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

    public Double getAmountCharged() {
        return amountCharged;
    }

    public Double getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProcessedAtUtc() {
        return processedAtUtc;
    }
}
