package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserIdAndPassword(String userId, String password);

    List<User> findByUserType(String userType);

    List<User> findByLastName(String lastName);
}
