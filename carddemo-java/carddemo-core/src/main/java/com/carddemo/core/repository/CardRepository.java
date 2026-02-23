package com.carddemo.core.repository;

import com.carddemo.core.domain.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Card entity.
 * Replaces VSAM READ/WRITE/REWRITE/DELETE operations on CARDDATA file.
 * VSAM key: CARD-NUM (PIC X(16))
 * VSAM AIX: by CARD-ACCT-ID → findByAcctId
 */
@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    Page<Card> findByAcctId(Long acctId, Pageable pageable);

    List<Card> findByAcctId(Long acctId);

    Page<Card> findByActiveStatus(String activeStatus, Pageable pageable);

    Page<Card> findByCardNumStartingWith(String prefix, Pageable pageable);
}
