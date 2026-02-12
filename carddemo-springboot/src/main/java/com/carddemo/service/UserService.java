package com.carddemo.service;

import com.carddemo.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Business logic for user authentication and authorization.
 * Will contain migrated logic from:
 * - COSGN00C.cbl (Sign-on processing)
 * - USRSEC VSAM file operations
 *
 * Supports two roles: Regular User and Admin User.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
