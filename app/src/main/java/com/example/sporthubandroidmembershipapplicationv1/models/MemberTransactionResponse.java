package com.example.sporthubandroidmembershipapplicationv1.models;

public class MemberTransactionResponse {

    private int transactionId;
    private int memberId;
    private String transactionType;
    private String description;
    private double amount;
    private double balanceAfter;
    private String currency;
    private String occurredAtUtc;

    public int getTransactionId() {
        return transactionId;
    }

    public int getMemberId() {
        return memberId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getCurrency() {
        return currency;
    }

    public String getOccurredAtUtc() {
        return occurredAtUtc;
    }
}
