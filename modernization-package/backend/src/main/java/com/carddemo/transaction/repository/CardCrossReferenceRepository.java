package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.CardCrossReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository for CardCrossReference entity.
 * Replaces CCXREF VSAM KSDS + CXACAIX Alternate Index lookups.
 *
 * Path A: Account ID -> Card Number (replaces EXEC CICS READ DATASET(CXACAIX) RIDFLD(XREF-ACCT-ID))
 * Path B: Card Number -> Account ID (replaces EXEC CICS READ DATASET(CCXREF) RIDFLD(XREF-CARD-NUM))
 *
 * Business Rules: BR-AT-04, BR-AT-05
 */
@Repository
public interface CardCrossReferenceRepository extends JpaRepository<CardCrossReference, String> {

    /**
     * Path A: Resolve Account ID to Card Number.
     * Uses idx_xref_account_id index (replaces CXACAIX AIX).
     */
    Optional<CardCrossReference> findFirstByAccountId(BigDecimal accountId);

    /**
     * Path B: Resolve Card Number to Account ID.
     * Uses PK index (replaces CCXREF KSDS primary key lookup).
     */
    Optional<CardCrossReference> findByCardNumber(String cardNumber);
}
