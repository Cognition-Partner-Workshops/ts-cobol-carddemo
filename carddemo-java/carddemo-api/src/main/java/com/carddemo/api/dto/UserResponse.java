package com.carddemo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User response DTO replacing BMS map COUSR02 output fields.
 * Maps from UserSecurity entity (CSUSR01Y copybook).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String userId;
    private String firstName;
    private String lastName;
    private String userType;
}
