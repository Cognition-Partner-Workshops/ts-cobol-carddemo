package com.carddemo.dto;

public class LoginResponse {

    private String userId;
    private String firstName;
    private String lastName;
    private String userType;
    private String message;

    public LoginResponse() {}

    public LoginResponse(String userId, String firstName, String lastName, String userType, String message) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userType = userType;
        this.message = message;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
