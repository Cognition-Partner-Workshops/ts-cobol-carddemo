package com.aws.carddemo.card;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.card.dto.CardXrefResponse;
import com.aws.carddemo.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CardXrefService {

    private final CardXrefRepository cardXrefRepository;

    public CardXrefService(CardXrefRepository cardXrefRepository) {
        this.cardXrefRepository = cardXrefRepository;
    }

    public CardXrefResponse findByCardNumber(String cardNumber) {
        CardXref xref = cardXrefRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card cross-reference not found for card number: " + cardNumber));
        return CardXrefResponse.from(xref);
    }

    public List<CardXrefResponse> findByAccountId(Long accountId) {
        List<CardXref> xrefs = cardXrefRepository.findByAccountId(accountId);
        if (xrefs.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No card cross-references found for account ID: " + accountId);
        }
        return xrefs.stream().map(CardXrefResponse::from).toList();
    }

    public List<CardXrefResponse> findByCustomerId(Long customerId) {
        List<CardXref> xrefs = cardXrefRepository.findByCustomerId(customerId);
        if (xrefs.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No card cross-references found for customer ID: " + customerId);
        }
        return xrefs.stream().map(CardXrefResponse::from).toList();
    }
}
