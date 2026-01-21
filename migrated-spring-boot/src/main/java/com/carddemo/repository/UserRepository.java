package com.carddemo.repository;

import com.carddemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for User entity operations.
 * Provides CRUD operations and custom queries for user management.
 *
 * <p>Replaces mainframe VSAM file operations for USRSEC file.
 *
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by user ID (case-insensitive).
     *
     * @param userId the user identifier
     * @return optional containing the user if found
     */
    Optional<User> findByUserIdIgnoreCase(String userId);

    /**
     * Find all users by user type.
     *
     * @param userType the user type ('A' for Admin, 'U' for User)
     * @return list of users with the specified type
     */
    List<User> findByUserType(String userType);

    /**
     * Find all admin users.
     *
     * @return list of admin users
     */
    default List<User> findAllAdmins() {
        return findByUserType("A");
    }

    /**
     * Find all regular users.
     *
     * @return list of regular users
     */
    default List<User> findAllRegularUsers() {
        return findByUserType("U");
    }

    /**
     * Find users by last name.
     *
     * @param lastName the last name to search for
     * @return list of users with matching last name
     */
    List<User> findByLastName(String lastName);

    /**
     * Check if a user exists by user ID.
     *
     * @param userId the user identifier
     * @return true if user exists, false otherwise
     */
    boolean existsByUserId(String userId);

    /**
     * Count users by user type.
     *
     * @param userType the user type to count
     * @return count of users with the specified type
     */
    long countByUserType(String userType);
}
