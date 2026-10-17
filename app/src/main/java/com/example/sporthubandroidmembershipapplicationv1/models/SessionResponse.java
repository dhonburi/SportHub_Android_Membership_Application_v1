package com.example.sporthubandroidmembershipapplicationv1.models;

public class SessionResponse {
    private int memberId;
    private long memberSessionExpiresAtUnixMs;
    private long serverTimeUnixMs;
    public int getMemberId() { return memberId; }
    public long getMemberSessionExpiresAtUnixMs() { return memberSessionExpiresAtUnixMs; }
    public long getServerTimeUnixMs() { return serverTimeUnixMs; }
}