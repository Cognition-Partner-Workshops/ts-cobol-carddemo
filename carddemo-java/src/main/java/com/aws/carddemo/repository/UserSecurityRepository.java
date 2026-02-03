package com.aws.carddemo.repository;

import com.aws.carddemo.model.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, String> {

    List<UserSecurity> findBySecUsrType(String userType);

    List<UserSecurity> findBySecUsrLname(String lastName);
}
