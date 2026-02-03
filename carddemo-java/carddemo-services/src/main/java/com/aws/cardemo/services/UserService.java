package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.User;
import com.aws.cardemo.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing User business logic.
 * 
 * This service provides the business layer for user operations in the CardDemo application.
 * It encapsulates all user-related business rules and coordinates with the persistence layer
 * through the UserRepository. All methods are transactional to ensure data consistency.
 * 
 * Key responsibilities include:
 * - CRUD operations for user management
 * - User authentication
 * - User filtering by type and name
 * 
 * Note: In production, password handling should use proper encryption and hashing.
 * Consider integrating with Spring Security for robust authentication.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves all users from the database.
     * 
     * @return List of all User entities in the system
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by their unique identifier.
     * 
     * @param userId The unique user identifier (8 characters max)
     * @return Optional containing the User if found, empty Optional otherwise
     */
    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * Creates a new user in the system.
     * 
     * Note: In production, ensure password is properly hashed before storage.
     * 
     * @param user The User entity to create
     * @return The created User with any generated values populated
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Updates an existing user in the system.
     * 
     * @param user The User entity with updated values
     * @return The updated User entity
     */
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Deletes a user from the system.
     * 
     * This is a hard delete operation. Consider implementing soft delete for production use.
     * 
     * @param userId The unique identifier of the user to delete
     */
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Authenticates a user by their credentials.
     * 
     * Note: This is a basic authentication method. In production, use Spring Security
     * with proper password hashing (BCrypt) and consider implementing JWT tokens.
     * 
     * @param userId The user identifier
     * @param password The user's password (should be hashed in production)
     * @return Optional containing the User if credentials are valid, empty Optional otherwise
     */
    public Optional<User> authenticate(String userId, String password) {
        return userRepository.findByUserIdAndPassword(userId, password);
    }

    /**
     * Retrieves all users of a specific type.
     * 
     * @param userType The user type code (e.g., 'A' for Admin, 'U' for User)
     * @return List of users matching the specified type
     */
    public List<User> getUsersByType(String userType) {
        return userRepository.findByUserType(userType);
    }

    /**
     * Retrieves all users with a specific last name.
     * 
     * @param lastName The last name to search for (exact match)
     * @return List of users with the specified last name
     */
    public List<User> getUsersByLastName(String lastName) {
        return userRepository.findByLastName(lastName);
    }
}
