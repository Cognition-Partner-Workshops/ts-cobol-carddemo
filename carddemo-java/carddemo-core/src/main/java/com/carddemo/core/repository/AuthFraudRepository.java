package com.carddemo.core.repository;

import com.carddemo.core.domain.AuthFraud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AuthFraud entity.
 * Replaces DB2 embedded SQL on AUTHFRDS table from the IMS-DB2-MQ module.
 */
@Repository
public interface AuthFraudRepository extends JpaRepository<AuthFraud, Long> {

    List<AuthFraud> findByCardNum(String cardNum);

    Page<AuthFraud> findByAcctId(Long acctId, Pageable pageable);
}
