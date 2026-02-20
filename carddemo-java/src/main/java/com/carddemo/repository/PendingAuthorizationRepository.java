package com.carddemo.repository;

import com.carddemo.entity.PendingAuthorization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PendingAuthorizationRepository extends JpaRepository<PendingAuthorization, Long> {

    Page<PendingAuthorization> findByCardNum(String cardNum, Pageable pageable);

    Page<PendingAuthorization> findByAcctId(Long acctId, Pageable pageable);

    List<PendingAuthorization> findByExpiryDateBefore(LocalDateTime date);

    Page<PendingAuthorization> findByAuthStatus(String authStatus, Pageable pageable);
}
