package com.carddemo.dto;

import com.carddemo.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private String userId;
    private String firstName;
    private String lastName;
    private User.UserType userType;
}
