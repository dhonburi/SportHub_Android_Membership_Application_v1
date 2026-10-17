package com.example.sporthubandroidmembershipapplicationv1.models;

public class MembershipPlanResponse {

    private int membershipPlanId;
    private String planName;
    private double price;
    private String description;
    private boolean isAlreadyActive;

    public int getMembershipPlanId() {
        return membershipPlanId;
    }

    public void setMembershipPlanId(int membershipPlanId) {
        this.membershipPlanId = membershipPlanId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAlreadyActive() {
        return isAlreadyActive;
    }

    public void setAlreadyActive(boolean alreadyActive) {
        isAlreadyActive = alreadyActive;
    }
}