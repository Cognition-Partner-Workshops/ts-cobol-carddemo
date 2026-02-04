package com.aws.carddemo.service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private String userId;
    private String firstName;
    private String lastName;
    private String userType;
    private Boolean enabled;
    private boolean admin;
}
