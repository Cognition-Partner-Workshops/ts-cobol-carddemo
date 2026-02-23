package com.carddemo.api.service;

import com.carddemo.api.dto.CardResponse;
import com.carddemo.api.dto.CardUpdateRequest;
import com.carddemo.api.dto.PageResponse;
import com.carddemo.core.domain.Card;
import com.carddemo.core.exception.ResourceNotFoundException;
import com.carddemo.core.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Card operations.
 * Replaces business logic from COCRDLIC (Card List), COCRDSLC (Card View),
 * and COCRDUPC (Card Update).
 *
 * Key COBOL logic replaced:
 * - VSAM READ on CARDDATA file → JPA findById
 * - VSAM STARTBR/READNEXT browse → JPA paginated queries
 * - VSAM REWRITE on CARDDATA file → JPA save
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;

    public PageResponse<CardResponse> listCards(Long accountId, Pageable pageable) {
        Page<Card> page;
        if (accountId != null) {
            page = cardRepository.findByAcctId(accountId, pageable);
        } else {
            page = cardRepository.findAll(pageable);
        }
        return buildPageResponse(page);
    }

    public CardResponse getCard(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardNumber));
        return mapToResponse(card);
    }

    @Transactional
    public CardResponse updateCard(String cardNumber, CardUpdateRequest request) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", cardNumber));

        if (request.getEmbossedName() != null) {
            card.setEmbossedName(request.getEmbossedName());
        }
        if (request.getExpirationDate() != null) {
            card.setExpirationDate(request.getExpirationDate());
        }
        if (request.getActiveStatus() != null) {
            card.setActiveStatus(request.getActiveStatus());
        }

        Card saved = cardRepository.save(card);
        return mapToResponse(saved);
    }

    private CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .cardNumber(card.getCardNum())
                .accountId(card.getAcctId())
                .cvvCode(card.getCvvCode())
                .embossedName(card.getEmbossedName())
                .expirationDate(card.getExpirationDate())
                .activeStatus(card.getActiveStatus())
                .build();
    }

    private PageResponse<CardResponse> buildPageResponse(Page<Card> page) {
        return PageResponse.<CardResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
