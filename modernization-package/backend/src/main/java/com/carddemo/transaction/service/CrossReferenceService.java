package com.carddemo.transaction.service;

import com.carddemo.transaction.dto.CrossReferenceResponse;
import com.carddemo.transaction.entity.CardCrossReference;
import com.carddemo.transaction.exception.ResourceNotFoundException;
import com.carddemo.transaction.repository.CardCrossReferenceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for bidirectional Account ID <-> Card Number resolution.
 * Replaces CXACAIX (Alternate Index) and CCXREF (KSDS) VSAM file lookups.
 * Business Rules: BR-AT-04, BR-AT-05
 */
@Service
public class CrossReferenceService {

    private final CardCrossReferenceRepository xrefRepository;

    public CrossReferenceService(CardCrossReferenceRepository xrefRepository) {
        this.xrefRepository = xrefRepository;
    }

    /**
     * Resolve cross-reference by Account ID or Card Number.
     * Exactly one parameter must be provided.
     *
     * Path A: Account ID -> Card Number (replaces EXEC CICS READ DATASET(CXACAIX))
     * Path B: Card Number -> Account ID (replaces EXEC CICS READ DATASET(CCXREF))
     */
    public CrossReferenceResponse resolve(String accountId, String cardNumber) {
        if (accountId != null && !accountId.isBlank() && cardNumber != null && !cardNumber.isBlank()) {
            throw new IllegalArgumentException("Provide either accountId or cardNumber, not both");
        }

        if ((accountId == null || accountId.isBlank()) && (cardNumber == null || cardNumber.isBlank())) {
            throw new IllegalArgumentException("Either accountId or cardNumber must be provided");
        }

        CardCrossReference xref;

        if (accountId != null && !accountId.isBlank()) {
            // Path A: Account ID -> Card Number
            BigDecimal acctId = new BigDecimal(accountId);
            xref = xrefRepository.findFirstByAccountId(acctId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Account ID NOT found...", "accountId", "BR-AT-04"));
        } else {
            // Path B: Card Number -> Account ID
            xref = xrefRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Card Number NOT found...", "cardNumber", "BR-AT-04"));
        }

        CrossReferenceResponse response = new CrossReferenceResponse();
        response.setCardNumber(xref.getCardNumber());
        response.setAccountId(String.format("%011d", xref.getAccountId().longValue()));
        response.setCustomerId(xref.getCustomerId().longValue());
        return response;
    }
}
