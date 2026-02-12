package com.carddemo.repository;

import com.carddemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for User entities.
 * Replaces VSAM I/O operations on USRSEC file.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
