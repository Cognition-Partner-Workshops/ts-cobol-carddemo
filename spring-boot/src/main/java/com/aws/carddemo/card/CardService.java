package com.aws.carddemo.card;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.card.dto.CardDetailResponse;
import com.aws.carddemo.card.dto.CardListItemResponse;
import com.aws.carddemo.card.dto.CardUpdateRequest;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;

@Service
@Transactional
public class CardService {

    private static final Set<String> VALID_STATUSES = Set.of("A", "C", "L");
    private static final int CARD_NUMBER_LENGTH = 16;

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public Page<CardListItemResponse> listCardsByAccount(Long accountId, Pageable pageable) {
        Page<Card> cards = cardRepository.findByAccountId(accountId, pageable);
        return cards.map(CardListItemResponse::from);
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getCardByNumber(String cardNumber) {
        validateCardNumberFormat(cardNumber);
        Card card = findCardByNumber(cardNumber);
        return CardDetailResponse.from(card);
    }

    public CardDetailResponse updateCard(String cardNumber, CardUpdateRequest request) {
        validateCardNumberFormat(cardNumber);
        Card card = findCardByNumber(cardNumber);

        if (request.cardStatus() != null) {
            validateStatusTransition(card.getCardStatus(), request.cardStatus());
            card.setCardStatus(request.cardStatus());
        }

        if (request.embossedName() != null) {
            if (request.embossedName().isBlank()) {
                throw new ValidationException("Embossed name must not be blank");
            }
            card.setEmbossedName(request.embossedName());
        }

        if (request.expiryDate() != null) {
            validateExpiryDate(request.expiryDate());
            card.setExpiryDate(request.expiryDate());
        }

        Card saved = cardRepository.save(card);
        return CardDetailResponse.from(saved);
    }

    private Card findCardByNumber(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card not found with number: " + cardNumber));
    }

    private void validateCardNumberFormat(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != CARD_NUMBER_LENGTH
                || !cardNumber.matches("\\d{" + CARD_NUMBER_LENGTH + "}")) {
            throw new ValidationException(
                    "Card number must be exactly " + CARD_NUMBER_LENGTH + " digits");
        }
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new ValidationException(
                    "Invalid card status: " + newStatus + ". Must be A (Active), C (Cancelled), or L (Lost)");
        }

        if ("C".equals(currentStatus) || "L".equals(currentStatus)) {
            if ("A".equals(newStatus)) {
                throw new ValidationException(
                        "Cannot reactivate a card with status: " + currentStatus);
            }
        }
    }

    private void validateExpiryDate(LocalDate expiryDate) {
        if (!expiryDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Expiry date must be in the future");
        }
    }
}
