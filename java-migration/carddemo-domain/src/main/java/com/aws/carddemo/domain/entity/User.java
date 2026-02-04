package com.aws.carddemo.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * User entity - migrated from CSUSR01Y.cpy
 * Original VSAM record length: 80 bytes
 * Stores user security data for authentication
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_type", columnList = "userType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Size(max = 8)
    @Column(name = "user_id", length = 8)
    private String userId;

    @NotBlank
    @Size(max = 20)
    @Column(name = "first_name", length = 20, nullable = false)
    private String firstName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "last_name", length = 20, nullable = false)
    private String lastName;

    @NotBlank
    @Size(max = 60)
    @Column(name = "password", length = 60, nullable = false)
    private String password;

    @NotNull
    @Column(name = "user_type", length = 1, nullable = false)
    private String userType;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "account_non_expired")
    @Builder.Default
    private Boolean accountNonExpired = true;

    @Column(name = "account_non_locked")
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired")
    @Builder.Default
    private Boolean credentialsNonExpired = true;

    @Version
    private Long version;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    public boolean isAdmin() {
        return "A".equalsIgnoreCase(userType);
    }

    public boolean isRegularUser() {
        return "U".equalsIgnoreCase(userType);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
