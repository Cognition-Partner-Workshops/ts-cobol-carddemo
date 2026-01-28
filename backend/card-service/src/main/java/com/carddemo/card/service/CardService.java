package com.carddemo.card.service;

import com.carddemo.card.dto.CreateCardRequest;
import com.carddemo.card.dto.UpdateCardRequest;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.common.dto.CardDto;
import com.carddemo.common.dto.PagedResponse;
import com.carddemo.common.entity.Card;
import com.carddemo.common.exception.CardExpiredException;
import com.carddemo.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final Random random = new Random();

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public PagedResponse<CardDto> getAllCards(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Card> cardPage = cardRepository.findAll(pageable);

        List<CardDto> cards = cardPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<CardDto>builder()
                .content(cards)
                .page(cardPage.getNumber())
                .size(cardPage.getSize())
                .totalElements(cardPage.getTotalElements())
                .totalPages(cardPage.getTotalPages())
                .first(cardPage.isFirst())
                .last(cardPage.isLast())
                .build();
    }

    public CardDto getCardByNumber(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));
        return mapToDto(card);
    }

    public List<CardDto> getCardsByAccountId(Long accountId) {
        return cardRepository.findByAccountId(accountId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardDto createCard(CreateCardRequest request) {
        String cardNumber = generateCardNumber();
        String cvv = generateCvv();

        Card card = Card.builder()
                .cardNumber(cardNumber)
                .accountId(request.getAccountId())
                .cvvCode(cvv)
                .embossedName(request.getEmbossedName().toUpperCase())
                .expirationDate(request.getExpirationDate())
                .activeStatus("Y")
                .build();

        card = cardRepository.save(card);
        return mapToDto(card);
    }

    @Transactional
    public CardDto updateCard(String cardNumber, UpdateCardRequest request) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));

        if (request.getEmbossedName() != null) card.setEmbossedName(request.getEmbossedName().toUpperCase());
        if (request.getExpirationDate() != null) card.setExpirationDate(request.getExpirationDate());
        if (request.getActiveStatus() != null) card.setActiveStatus(request.getActiveStatus());

        card = cardRepository.save(card);
        return mapToDto(card);
    }

    @Transactional
    public CardDto activateCard(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));
        card.setActiveStatus("Y");
        card = cardRepository.save(card);
        return mapToDto(card);
    }

    @Transactional
    public CardDto deactivateCard(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));
        card.setActiveStatus("N");
        card = cardRepository.save(card);
        return mapToDto(card);
    }

    public List<CardDto> getActiveCards() {
        return cardRepository.findByActiveStatus("Y").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<CardDto> getExpiredCards() {
        return cardRepository.findExpiredCards(LocalDate.now()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<CardDto> getExpiringCards(int daysAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);
        return cardRepository.findCardsExpiringBetween(startDate, endDate).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<CardDto> searchByLastFourDigits(String lastFourDigits) {
        return cardRepository.findByLastFourDigits(lastFourDigits).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void validateCardActive(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));
        if (!card.isActive()) {
            throw new CardExpiredException(cardNumber);
        }
        if (card.isExpired()) {
            throw new CardExpiredException(cardNumber);
        }
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        sb.append("4"); // Visa prefix
        for (int i = 0; i < 15; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateCvv() {
        return String.format("%03d", random.nextInt(1000));
    }

    private CardDto mapToDto(Card card) {
        return CardDto.builder()
                .cardNumber(card.getCardNumber())
                .maskedCardNumber(card.getMaskedCardNumber())
                .accountId(card.getAccountId())
                .embossedName(card.getEmbossedName())
                .expirationDate(card.getExpirationDate())
                .activeStatus(card.getActiveStatus())
                .isExpired(card.isExpired())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
