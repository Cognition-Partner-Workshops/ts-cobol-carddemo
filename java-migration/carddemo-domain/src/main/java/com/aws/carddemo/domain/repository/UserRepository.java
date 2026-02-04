package com.aws.carddemo.domain.repository;

import com.aws.carddemo.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);

    List<User> findByUserType(String userType);

    Page<User> findByUserType(String userType, Pageable pageable);

    List<User> findByEnabled(Boolean enabled);

    @Query("SELECT u FROM User u WHERE u.userType = 'A'")
    List<User> findAllAdmins();

    @Query("SELECT u FROM User u WHERE u.userType = 'U'")
    List<User> findAllRegularUsers();

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<User> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = 'A'")
    long countAdmins();

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countEnabledUsers();

    boolean existsByUserId(String userId);
}
