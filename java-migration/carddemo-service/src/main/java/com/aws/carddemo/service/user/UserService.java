package com.aws.carddemo.service.user;

import com.aws.carddemo.domain.entity.User;
import com.aws.carddemo.domain.repository.UserRepository;
import com.aws.carddemo.service.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * User Service - migrated from COUSR00C.cbl, COUSR01C.cbl, COUSR02C.cbl, COUSR03C.cbl
 * Handles user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * List all users - migrated from COUSR00C.cbl
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(Pageable pageable) {
        log.info("Listing all users");
        return userRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get user by ID - migrated from COUSR01C.cbl
     */
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUser(String userId) {
        log.info("Fetching user: {}", userId);
        return userRepository.findByUserId(userId.toUpperCase())
                .map(this::mapToDTO);
    }

    /**
     * Create new user - migrated from COUSR02C.cbl
     */
    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        log.info("Creating new user: {}", request.getUserId());

        String userId = request.getUserId().toUpperCase();
        
        if (userRepository.existsByUserId(userId)) {
            throw new UserAlreadyExistsException("User already exists: " + userId);
        }

        User user = User.builder()
                .userId(userId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword().toUpperCase()))
                .userType(request.getUserType())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", userId);
        
        return mapToDTO(savedUser);
    }

    /**
     * Update user - migrated from COUSR03C.cbl
     */
    @Transactional
    public UserDTO updateUser(String userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword().toUpperCase()));
        }
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User savedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);
        
        return mapToDTO(savedUser);
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(String userId) {
        log.info("Deleting user: {}", userId);

        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        userRepository.delete(user);
        log.info("User deleted successfully: {}", userId);
    }

    /**
     * List admin users
     */
    @Transactional(readOnly = true)
    public List<UserDTO> listAdmins() {
        return userRepository.findAllAdmins().stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Search users by name
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> searchByName(String name, Pageable pageable) {
        return userRepository.searchByName(name, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    public UserStatistics getStatistics() {
        return UserStatistics.builder()
                .totalUsers(userRepository.count())
                .totalAdmins(userRepository.countAdmins())
                .totalEnabledUsers(userRepository.countEnabledUsers())
                .build();
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .enabled(user.getEnabled())
                .admin(user.isAdmin())
                .build();
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class UserCreateRequest {
        private String userId;
        private String firstName;
        private String lastName;
        private String password;
        private String userType;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class UserUpdateRequest {
        private String firstName;
        private String lastName;
        private String password;
        private String userType;
        private Boolean enabled;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class UserStatistics {
        private long totalUsers;
        private long totalAdmins;
        private long totalEnabledUsers;
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }
}
