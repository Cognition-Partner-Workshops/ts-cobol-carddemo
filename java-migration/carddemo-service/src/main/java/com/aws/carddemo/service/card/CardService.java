package com.aws.carddemo.service.card;

import com.aws.carddemo.domain.entity.Account;
import com.aws.carddemo.domain.entity.Card;
import com.aws.carddemo.domain.repository.AccountRepository;
import com.aws.carddemo.domain.repository.CardRepository;
import com.aws.carddemo.service.dto.CardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Card Service - migrated from COCRDLIC.cbl, COCRDSLC.cbl, COCRDUPC.cbl
 * Handles card list, view, and update operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    /**
     * List cards by account - migrated from COCRDLIC.cbl
     */
    @Transactional(readOnly = true)
    public Page<CardDTO> listCards(Long accountId, Pageable pageable) {
        log.info("Listing cards for account: {}", accountId);
        return cardRepository.findByAccountAccountId(accountId, pageable)
                .map(this::mapToDTO);
    }

    /**
     * List all cards with pagination
     */
    @Transactional(readOnly = true)
    public Page<CardDTO> listAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get card details - migrated from COCRDSLC.cbl
     */
    @Transactional(readOnly = true)
    public Optional<CardDTO> getCard(String cardNumber) {
        log.info("Fetching card: {}", maskCardNumber(cardNumber));
        return cardRepository.findById(cardNumber)
                .map(this::mapToDTO);
    }

    /**
     * Update card - migrated from COCRDUPC.cbl
     */
    @Transactional
    public CardDTO updateCard(String cardNumber, CardUpdateRequest request) {
        log.info("Updating card: {}", maskCardNumber(cardNumber));
        
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + maskCardNumber(cardNumber)));

        if (request.getActiveStatus() != null) {
            card.setActiveStatus(request.getActiveStatus());
        }
        if (request.getEmbossedName() != null) {
            card.setEmbossedName(request.getEmbossedName());
        }
        if (request.getExpirationDate() != null) {
            card.setExpirationDate(request.getExpirationDate());
        }

        Card savedCard = cardRepository.save(card);
        log.info("Card updated successfully: {}", maskCardNumber(cardNumber));
        
        return mapToDTO(savedCard);
    }

    /**
     * Create new card
     */
    @Transactional
    public CardDTO createCard(CardCreateRequest request) {
        log.info("Creating new card for account: {}", request.getAccountId());

        if (cardRepository.existsByCardNumber(request.getCardNumber())) {
            throw new CardAlreadyExistsException("Card already exists: " + maskCardNumber(request.getCardNumber()));
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountId()));

        Card card = Card.builder()
                .cardNumber(request.getCardNumber())
                .account(account)
                .cvvCode(request.getCvvCode())
                .embossedName(request.getEmbossedName())
                .expirationDate(request.getExpirationDate())
                .activeStatus("Y")
                .build();

        Card savedCard = cardRepository.save(card);
        log.info("Card created successfully: {}", maskCardNumber(request.getCardNumber()));
        
        return mapToDTO(savedCard);
    }

    /**
     * Find expiring cards
     */
    @Transactional(readOnly = true)
    public List<CardDTO> findExpiringCards(LocalDate beforeDate) {
        return cardRepository.findExpiringCards(beforeDate).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Find expired but active cards
     */
    @Transactional(readOnly = true)
    public List<CardDTO> findExpiredActiveCards() {
        return cardRepository.findExpiredActiveCards().stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Get card statistics
     */
    @Transactional(readOnly = true)
    public CardStatistics getStatistics() {
        return CardStatistics.builder()
                .totalActiveCards(cardRepository.countActiveCards())
                .build();
    }

    private CardDTO mapToDTO(Card card) {
        return CardDTO.builder()
                .cardNumber(card.getCardNumber())
                .maskedCardNumber(card.getMaskedCardNumber())
                .accountId(card.getAccount().getAccountId())
                .embossedName(card.getEmbossedName())
                .expirationDate(card.getExpirationDate())
                .activeStatus(card.getActiveStatus())
                .expired(card.isExpired())
                .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) {
            return "****";
        }
        return "****" + cardNumber.substring(12);
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class CardUpdateRequest {
        private String activeStatus;
        private String embossedName;
        private LocalDate expirationDate;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class CardCreateRequest {
        private String cardNumber;
        private Long accountId;
        private Integer cvvCode;
        private String embossedName;
        private LocalDate expirationDate;
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class CardStatistics {
        private long totalActiveCards;
    }

    public static class CardNotFoundException extends RuntimeException {
        public CardNotFoundException(String message) {
            super(message);
        }
    }

    public static class CardAlreadyExistsException extends RuntimeException {
        public CardAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) {
            super(message);
        }
    }
}
