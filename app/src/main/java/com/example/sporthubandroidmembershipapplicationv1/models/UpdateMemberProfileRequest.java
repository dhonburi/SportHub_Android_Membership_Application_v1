package com.example.sporthubandroidmembershipapplicationv1.models;

public class UpdateMemberProfileRequest {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String gender;

    public UpdateMemberProfileRequest(
            String firstName,
            String lastName,
            String email,
            String phone,
            String gender
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
    }
}