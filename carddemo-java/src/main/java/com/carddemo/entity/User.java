package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User entity - migrated from VSAM file USRSEC / copybook CSUSR01Y.
 * Original record length: 80 bytes.
 *
 * Fields mapped from COBOL:
 *   SEC-USR-ID       PIC X(08) -> userId
 *   SEC-USR-FNAME    PIC X(20) -> firstName
 *   SEC-USR-LNAME    PIC X(20) -> lastName
 *   SEC-USR-PWD      PIC X(08) -> password
 *   SEC-USR-TYPE     PIC X(01) -> userType ('A' = Admin, 'U' = User)
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", length = 8, nullable = false)
    @NotBlank
    @Size(max = 8)
    private String userId;

    @Column(name = "first_name", length = 20)
    @Size(max = 20)
    private String firstName;

    @Column(name = "last_name", length = 20)
    @Size(max = 20)
    private String lastName;

    @Column(name = "password", length = 8, nullable = false)
    @NotBlank
    @Size(max = 8)
    private String password;

    @Column(name = "user_type", length = 1, nullable = false)
    @Size(max = 1)
    private String userType;

    public User() {
    }

    public User(String userId, String firstName, String lastName, String password, String userType) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.userType = userType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean isAdmin() {
        return "A".equalsIgnoreCase(userType);
    }

    public boolean isRegularUser() {
        return "U".equalsIgnoreCase(userType);
    }
}
