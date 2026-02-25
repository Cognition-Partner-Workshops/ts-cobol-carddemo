package com.cardemo.repository;

import com.cardemo.entity.AuthFraud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuthFraudRepository extends JpaRepository<AuthFraud, AuthFraud.AuthFraudId> {
    List<AuthFraud> findByCardNum(String cardNum);
}
