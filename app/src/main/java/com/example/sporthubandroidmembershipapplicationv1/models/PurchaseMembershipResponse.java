package com.example.sporthubandroidmembershipapplicationv1.models;

public class PurchaseMembershipResponse {

    private int memberMembershipId;
    private int memberId;
    private int membershipPlanId;
    private String planName;
    private double pricePaid;
    private double balance;
    private String currency;
    private String status;
    private String startDate;
    private String expiryDate;
    private Integer remainingEntries;
    private String message;

    public int getMemberMembershipId() {
        return memberMembershipId;
    }

    public int getMemberId() {
        return memberId;
    }

    public int getMembershipPlanId() {
        return membershipPlanId;
    }

    public String getPlanName() {
        return planName;
    }

    public double getPricePaid() {
        return pricePaid;
    }

    public double getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public Integer getRemainingEntries() {
        return remainingEntries;
    }

    public String getMessage() {
        return message;
    }
}