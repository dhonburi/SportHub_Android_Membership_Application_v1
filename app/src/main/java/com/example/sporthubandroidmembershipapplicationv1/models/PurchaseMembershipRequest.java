package com.example.sporthubandroidmembershipapplicationv1.models;

public class PurchaseMembershipRequest {

    private int membershipPlanId;

    public PurchaseMembershipRequest(int membershipPlanId) {
        this.membershipPlanId = membershipPlanId;
    }

    public int getMembershipPlanId() {
        return membershipPlanId;
    }

    public void setMembershipPlanId(int membershipPlanId) {
        this.membershipPlanId = membershipPlanId;
    }
}