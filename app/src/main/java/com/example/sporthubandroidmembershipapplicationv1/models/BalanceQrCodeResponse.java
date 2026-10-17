package com.example.sporthubandroidmembershipapplicationv1.models;

public class BalanceQrCodeResponse {

    private int memberId;
    private double balance;
    private String currency;
    private String qrToken;
    private String issuedAtUtc;
    private String expiresAtUtc;
    private int validitySeconds;

    public int getMemberId() {
        return memberId;
    }

    public double getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getQrToken() {
        return qrToken;
    }

    public String getIssuedAtUtc() {
        return issuedAtUtc;
    }

    public String getExpiresAtUtc() {
        return expiresAtUtc;
    }

    public int getValiditySeconds() {
        return validitySeconds;
    }
}