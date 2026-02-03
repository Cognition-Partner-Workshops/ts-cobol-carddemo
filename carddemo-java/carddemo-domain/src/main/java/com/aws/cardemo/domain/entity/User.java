package com.aws.cardemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA Entity representing a system user in the CardDemo system.
 * 
 * This entity maps to the 'users' table and stores user authentication and profile
 * information. It represents the modernized version of the COBOL USRDATA-RECORD
 * from the original mainframe application.
 * 
 * User type codes:
 * - 'A' = Administrator (full system access)
 * - 'U' = Regular User (limited access)
 * 
 * Note: In production, passwords should be hashed using BCrypt or similar algorithms.
 * Consider integrating with Spring Security for proper authentication and authorization.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Unique user identifier (primary key).
     * Maximum length: 8 characters.
     */
    @Id
    @Column(name = "user_id", length = 8)
    private String userId;

    /**
     * User password.
     * Note: Should be hashed in production systems.
     * Required field.
     */
    @NotNull
    @Column(name = "password", length = 8)
    private String password;

    /**
     * User's first name.
     * Required field.
     */
    @NotNull
    @Column(name = "first_name", length = 20)
    private String firstName;

    /**
     * User's last name.
     * Required field.
     */
    @NotNull
    @Column(name = "last_name", length = 20)
    private String lastName;

    /**
     * User type code.
     * 'A' = Administrator, 'U' = Regular User.
     * Required field.
     */
    @NotNull
    @Column(name = "user_type", length = 1)
    private String userType;
}
