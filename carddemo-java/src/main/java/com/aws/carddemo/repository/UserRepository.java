package com.aws.carddemo.repository;

import com.aws.carddemo.entity.User;
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

    List<User> findByEnabled(Boolean enabled);

    @Query("SELECT u FROM User u WHERE u.userType = 'A'")
    List<User> findAllAdmins();

    @Query("SELECT u FROM User u WHERE u.userType = 'U'")
    List<User> findAllRegularUsers();

    @Query("SELECT u FROM User u WHERE LOWER(u.userLastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> searchByLastName(@Param("name") String name);

    @Query("SELECT COUNT(u) FROM User u WHERE u.userType = :type")
    long countByType(@Param("type") String type);

    boolean existsByUserId(String userId);
}
