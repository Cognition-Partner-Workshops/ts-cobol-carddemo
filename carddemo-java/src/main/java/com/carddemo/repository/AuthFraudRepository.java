package com.carddemo.repository;

import com.carddemo.entity.AuthFraud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthFraudRepository extends JpaRepository<AuthFraud, Long> {

    List<AuthFraud> findByCardNum(String cardNum);

    List<AuthFraud> findByAcctId(Long acctId);
}
