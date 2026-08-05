package com.example.sporthubandroidmembershipapplicationv1.models;

public class MemberMembershipResponse {

    private int memberMembershipId;
    private String memberNumber;
    private String planName;
    private double price;
    private String description;
    private String status;
    private String startDate;
    private String expiryDate;
    private Integer remainingEntries;

    public int getMemberMembershipId() {
        return memberMembershipId;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public String getPlanName() {
        return planName;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
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
}