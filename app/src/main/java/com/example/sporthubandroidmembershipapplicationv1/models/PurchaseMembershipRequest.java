package com.example.sporthubandroidmembershipapplicationv1.models;

public class PurchaseMembershipRequest {

    private int membershipPlanId;

    private String operationId;

    public PurchaseMembershipRequest(
            int membershipPlanId,
            String operationId
    ) {
        this.membershipPlanId = membershipPlanId;
        this.operationId = operationId;
    }

    public int getMembershipPlanId() {
        return membershipPlanId;
    }

    public void setMembershipPlanId(int membershipPlanId) {
        this.membershipPlanId = membershipPlanId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }
}
