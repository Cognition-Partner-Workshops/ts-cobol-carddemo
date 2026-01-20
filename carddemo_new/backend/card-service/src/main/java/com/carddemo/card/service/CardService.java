package com.carddemo.card.service;

import com.carddemo.card.dto.CardDto;
import com.carddemo.card.dto.CardUpdateRequest;
import com.carddemo.card.entity.Card;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.common.dto.PageResponse;
import com.carddemo.common.exception.BusinessException;
import com.carddemo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    public CardDto getCardByNumber(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));
        return mapToCardDto(card);
    }

    public PageResponse<CardDto> getCardsByAccount(String accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("cardNumber").ascending());
        Page<Card> cardPage = cardRepository.findByAccountId(accountId, pageable);

        List<CardDto> cards = cardPage.getContent().stream()
                .map(this::mapToCardDto)
                .collect(Collectors.toList());

        return PageResponse.<CardDto>builder()
                .content(cards)
                .pageNumber(cardPage.getNumber())
                .pageSize(cardPage.getSize())
                .totalElements(cardPage.getTotalElements())
                .totalPages(cardPage.getTotalPages())
                .first(cardPage.isFirst())
                .last(cardPage.isLast())
                .build();
    }

    public List<CardDto> getCardsByCustomer(String customerId) {
        List<Card> cards = cardRepository.findByCustomerId(customerId);
        return cards.stream()
                .map(this::mapToCardDto)
                .collect(Collectors.toList());
    }

    public PageResponse<CardDto> getAllCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("cardNumber").ascending());
        Page<Card> cardPage = cardRepository.findAll(pageable);

        List<CardDto> cards = cardPage.getContent().stream()
                .map(this::mapToCardDto)
                .collect(Collectors.toList());

        return PageResponse.<CardDto>builder()
                .content(cards)
                .pageNumber(cardPage.getNumber())
                .pageSize(cardPage.getSize())
                .totalElements(cardPage.getTotalElements())
                .totalPages(cardPage.getTotalPages())
                .first(cardPage.isFirst())
                .last(cardPage.isLast())
                .build();
    }

    @Transactional
    public CardDto updateCard(String cardNumber, CardUpdateRequest request) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));

        if (request.getEmbossedName() != null) {
            if (!request.getEmbossedName().matches("^[A-Za-z\\s]*$")) {
                throw new BusinessException("Embossed name must contain only alphabetic characters", "INVALID_EMBOSSED_NAME");
            }
            card.setEmbossedName(request.getEmbossedName().toUpperCase());
        }

        if (request.getExpirationDate() != null) {
            card.setExpirationDate(request.getExpirationDate());
        }

        if (request.getActiveStatus() != null) {
            card.setActiveStatus(request.getActiveStatus());
        }

        Card savedCard = cardRepository.save(card);
        return mapToCardDto(savedCard);
    }

    private CardDto mapToCardDto(Card card) {
        String maskedNumber = maskCardNumber(card.getCardNumber());

        return CardDto.builder()
                .cardNumber(card.getCardNumber())
                .maskedCardNumber(maskedNumber)
                .accountId(card.getAccountId())
                .embossedName(card.getEmbossedName())
                .expirationDate(card.getExpirationDate())
                .activeStatus(card.getActiveStatus())
                .customerId(card.getCustomerId())
                .expired(card.isExpired())
                .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
