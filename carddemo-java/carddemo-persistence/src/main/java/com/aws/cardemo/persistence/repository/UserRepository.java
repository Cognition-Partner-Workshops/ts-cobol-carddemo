package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity persistence operations.
 * 
 * This repository provides data access methods for system users in the CardDemo application.
 * It extends JpaRepository to inherit standard CRUD operations and adds custom query methods
 * for user-specific business requirements.
 * 
 * The repository supports:
 * - Standard CRUD operations (inherited from JpaRepository)
 * - User authentication queries
 * - User filtering by type and name
 * 
 * Note: In production, password handling should use proper encryption and hashing.
 * Consider integrating with Spring Security for robust authentication.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Finds a user by their credentials for authentication.
     * 
     * Note: This is a basic authentication method. In production, use Spring Security
     * with proper password hashing (BCrypt) and consider implementing JWT tokens.
     * 
     * @param userId The user identifier
     * @param password The user's password (should be hashed in production)
     * @return Optional containing the User if credentials are valid, empty Optional otherwise
     */
    Optional<User> findByUserIdAndPassword(String userId, String password);

    /**
     * Finds all users of a specific type.
     * 
     * @param userType The user type code (e.g., 'A' for Admin, 'U' for User)
     * @return List of users matching the specified type
     */
    List<User> findByUserType(String userType);

    /**
     * Finds all users with a specific last name.
     * 
     * @param lastName The last name to search for (exact match)
     * @return List of users with the specified last name
     */
    List<User> findByLastName(String lastName);
}
