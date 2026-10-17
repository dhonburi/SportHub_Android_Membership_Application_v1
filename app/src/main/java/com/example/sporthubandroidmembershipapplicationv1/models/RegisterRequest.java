package com.example.sporthubandroidmembershipapplicationv1.models;

public class RegisterRequest {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String gender;
    private final String dateOfBirth;
    private final String password;
    private final String confirmPassword;

    public RegisterRequest(String firstName, String lastName, String email,
                           String phone, String gender, String dateOfBirth,
                           String password, String confirmPassword) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getEmail() {
        return email;
    }
}