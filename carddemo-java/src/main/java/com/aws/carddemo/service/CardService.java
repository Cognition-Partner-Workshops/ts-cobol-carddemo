package com.aws.carddemo.service;

import com.aws.carddemo.dto.CardDto;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.Card;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.mapper.CardMapper;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CardMapper cardMapper;

    public CardService(CardRepository cardRepository, AccountRepository accountRepository, CardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "cards", key = "#cardNum")
    public CardDto getCard(String cardNum) {
        Card card = cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNum", cardNum));
        return cardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public CardDto getCardWithAccount(String cardNum) {
        Card card = cardRepository.findByCardNumWithAccount(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNum", cardNum));
        return cardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public CardDto getCardWithTransactions(String cardNum) {
        Card card = cardRepository.findByCardNumWithTransactions(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNum", cardNum));
        return cardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    public Page<CardDto> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(cardMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<CardDto> getCardsByAccount(Long acctId) {
        return cardMapper.toDtoList(cardRepository.findByAccountAcctId(acctId));
    }

    @Transactional(readOnly = true)
    public Page<CardDto> getActiveCards(Pageable pageable) {
        return cardRepository.findByCardActiveStatus("Y", pageable).map(cardMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<CardDto> getExpiredCards() {
        return cardMapper.toDtoList(cardRepository.findExpiredCards(LocalDate.now()));
    }

    @Transactional(readOnly = true)
    public List<CardDto> getCardsExpiringBetween(LocalDate startDate, LocalDate endDate) {
        return cardMapper.toDtoList(cardRepository.findCardsExpiringBetween(startDate, endDate));
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#dto.cardNum")
    public CardDto createCard(CardDto dto) {
        Account account = accountRepository.findById(dto.getCardAcctId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "acctId", dto.getCardAcctId()));
        
        Card card = cardMapper.toEntity(dto);
        card.setAccount(account);
        card = cardRepository.save(card);
        return cardMapper.toDto(card);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#cardNum")
    public CardDto updateCard(String cardNum, CardDto dto) {
        Card card = cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNum", cardNum));
        cardMapper.updateEntity(dto, card);
        card = cardRepository.save(card);
        return cardMapper.toDto(card);
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#cardNum")
    public void deactivateCard(String cardNum) {
        Card card = cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNum", cardNum));
        card.setCardActiveStatus("N");
        cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public long countActiveCards() {
        return cardRepository.countByStatus("Y");
    }

    @Transactional(readOnly = true)
    public boolean existsByCardNum(String cardNum) {
        return cardRepository.existsByCardNum(cardNum);
    }
}
