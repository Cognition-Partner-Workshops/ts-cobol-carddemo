package com.carddemo.core.repository;

import com.carddemo.core.domain.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CardXref (Card-Account-Customer cross-reference) entity.
 * Replaces VSAM READ/WRITE/REWRITE/DELETE operations on CARDXREF file.
 * VSAM key: XREF-CARD-NUM (PIC X(16))
 * VSAM AIX equivalents: by customer ID, by account ID
 */
@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    List<CardXref> findByCustId(Long custId);

    List<CardXref> findByAcctId(Long acctId);

    Optional<CardXref> findByCardNum(String cardNum);
}
