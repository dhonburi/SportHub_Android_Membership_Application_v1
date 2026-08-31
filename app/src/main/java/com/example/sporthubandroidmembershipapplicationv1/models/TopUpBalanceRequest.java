package com.example.sporthubandroidmembershipapplicationv1.models;

public class TopUpBalanceRequest {

    private double amount;

    public TopUpBalanceRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}