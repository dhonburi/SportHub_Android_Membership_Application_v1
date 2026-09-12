package com.example.sporthubandroidmembershipapplicationv1.models;

import java.util.Map;

public class RegisterResponse {
    private boolean success;
    private String code;
    private String message;
    private Integer memberId;
    private String memberNumber;
    private Map<String, String[]> errors;

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public Map<String, String[]> getErrors() {
        return errors;
    }
}