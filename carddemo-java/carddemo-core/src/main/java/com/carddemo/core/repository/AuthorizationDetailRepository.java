package com.carddemo.core.repository;

import com.carddemo.core.domain.AuthorizationDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AuthorizationDetail entity.
 * Replaces IMS DL/I calls to PAUTDTL1 segment in DBPAUTP0 database.
 */
@Repository
public interface AuthorizationDetailRepository
        extends JpaRepository<AuthorizationDetail, Long> {

    Page<AuthorizationDetail> findByAcctId(Long acctId, Pageable pageable);

    List<AuthorizationDetail> findByAcctIdAndMatchStatus(Long acctId, String matchStatus);

    List<AuthorizationDetail> findByCardNum(String cardNum);
}
