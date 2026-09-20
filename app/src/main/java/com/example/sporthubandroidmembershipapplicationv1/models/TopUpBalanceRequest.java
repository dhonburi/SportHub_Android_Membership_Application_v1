package com.example.sporthubandroidmembershipapplicationv1.models;

public class TopUpBalanceRequest {

    private double amount;

    private String operationId;

    public TopUpBalanceRequest(
            double amount,
            String operationId
    ) {
        this.amount = amount;
        this.operationId = operationId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }
}
