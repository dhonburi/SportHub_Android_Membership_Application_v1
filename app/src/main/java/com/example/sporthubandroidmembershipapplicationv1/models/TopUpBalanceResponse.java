package com.example.sporthubandroidmembershipapplicationv1.models;

public class TopUpBalanceResponse {

    private int memberId;
    private double amountAdded;
    private double balance;
    private String currency;
    private String message;

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public double getAmountAdded() {
        return amountAdded;
    }

    public void setAmountAdded(double amountAdded) {
        this.amountAdded = amountAdded;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}