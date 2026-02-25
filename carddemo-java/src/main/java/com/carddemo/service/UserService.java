package com.carddemo.service;

import com.carddemo.entity.User;
import com.carddemo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User service - migrated from:
 *   COUSR00C (CU00 - List Users)
 *   COUSR01C (CU01 - Add User)
 *   COUSR02C (CU02 - Update User)
 *   COUSR03C (CU03 - Delete User)
 *
 * Original COBOL logic:
 * COUSR00C: STARTBR/READNEXT on USRSEC file to paginate users
 * COUSR01C: Validate input, WRITE new record to USRSEC
 * COUSR02C: READ/REWRITE record in USRSEC
 * COUSR03C: READ/DELETE record in USRSEC
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * List users with pagination - migrated from COUSR00C browse logic.
     */
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Get a user by ID.
     */
    public Optional<User> getUser(String userId) {
        return userRepository.findByUserId(userId.toUpperCase().trim());
    }

    /**
     * Add a new user - migrated from COUSR01C.
     * Validates user doesn't already exist, then writes to USRSEC.
     */
    @Transactional
    public User addUser(User user) {
        String userId = user.getUserId().toUpperCase().trim();
        if (userRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("User ID already exists");
        }

        user.setUserId(userId);
        if (user.getUserType() == null || user.getUserType().isBlank()) {
            user.setUserType("U");
        }
        return userRepository.save(user);
    }

    /**
     * Update an existing user - migrated from COUSR02C.
     * Reads existing record, applies changes, rewrites.
     */
    @Transactional
    public User updateUser(String userId, User updatedData) {
        User existing = userRepository.findByUserId(userId.toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updatedData.getFirstName() != null) {
            existing.setFirstName(updatedData.getFirstName());
        }
        if (updatedData.getLastName() != null) {
            existing.setLastName(updatedData.getLastName());
        }
        if (updatedData.getPassword() != null && !updatedData.getPassword().isBlank()) {
            existing.setPassword(updatedData.getPassword());
        }
        if (updatedData.getUserType() != null) {
            existing.setUserType(updatedData.getUserType());
        }

        return userRepository.save(existing);
    }

    /**
     * Delete a user - migrated from COUSR03C.
     */
    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findByUserId(userId.toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
    }
}
