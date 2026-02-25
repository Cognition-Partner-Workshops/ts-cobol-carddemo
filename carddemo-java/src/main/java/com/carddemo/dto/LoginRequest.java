package com.carddemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO - replaces COSGN00C sign-on screen input (BMS map COSGN00).
 */
public class LoginRequest {

    @NotBlank(message = "Please enter User ID")
    @Size(max = 8)
    private String userId;

    @NotBlank(message = "Please enter Password")
    @Size(max = 8)
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
