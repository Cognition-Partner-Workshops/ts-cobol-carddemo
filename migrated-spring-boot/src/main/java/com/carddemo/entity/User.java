package com.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JPA entity representing a system user for authentication and authorization.
 * Migrated from mainframe copybook: CSUSR01Y.cpy (SEC-USER-DATA)
 *
 * <p>This entity maps the VSAM user security file structure to a relational database table.
 * User types: 'A' for Admin, 'U' for regular User.
 *
 * @see com.carddemo.repository.UserRepository
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Size(max = 8)
    @Column(name = "user_id", length = 8)
    private String userId;

    @NotNull
    @Size(max = 20)
    @Column(name = "first_name", length = 20, nullable = false)
    private String firstName;

    @NotNull
    @Size(max = 20)
    @Column(name = "last_name", length = 20, nullable = false)
    private String lastName;

    @NotNull
    @Size(max = 8)
    @Column(name = "password", length = 8, nullable = false)
    private String password;

    @NotNull
    @Size(max = 1)
    @Column(name = "user_type", length = 1, nullable = false)
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
        return "A".equals(userType);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }
}
