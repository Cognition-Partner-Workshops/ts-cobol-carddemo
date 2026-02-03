package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Card;
import com.aws.cardemo.persistence.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing credit Card business logic.
 * 
 * This service provides the business layer for card operations in the CardDemo application.
 * It encapsulates all card-related business rules and coordinates with the persistence layer
 * through the CardRepository. All methods are transactional to ensure data consistency.
 * 
 * Key responsibilities include:
 * - CRUD operations for card management
 * - Card lifecycle management (activation/deactivation)
 * - Card filtering by account and status
 * - Card search functionality
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final CardRepository cardRepository;

    /**
     * Retrieves all cards from the database.
     * 
     * @return List of all Card entities in the system
     */
    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    /**
     * Retrieves a card by its card number.
     * 
     * @param cardNumber The 16-digit card number
     * @return Optional containing the Card if found, empty Optional otherwise
     */
    public Optional<Card> getCardByNumber(String cardNumber) {
        return cardRepository.findById(cardNumber);
    }

    /**
     * Creates a new card in the system.
     * 
     * @param card The Card entity to create
     * @return The created Card with any generated values populated
     */
    public Card createCard(Card card) {
        return cardRepository.save(card);
    }

    /**
     * Updates an existing card in the system.
     * 
     * @param card The Card entity with updated values
     * @return The updated Card entity
     */
    public Card updateCard(Card card) {
        return cardRepository.save(card);
    }

    /**
     * Deletes a card from the system.
     * 
     * This is a hard delete operation. Consider implementing soft delete for production use.
     * Associated transactions should be handled appropriately before deletion.
     * 
     * @param cardNumber The card number of the card to delete
     */
    public void deleteCard(String cardNumber) {
        cardRepository.deleteById(cardNumber);
    }

    /**
     * Retrieves all cards associated with a specific account.
     * 
     * @param accountId The account identifier to filter cards by
     * @return List of cards linked to the specified account
     */
    public List<Card> getCardsByAccountId(String accountId) {
        return cardRepository.findByAccountId(accountId);
    }

    /**
     * Retrieves only active cards for a specific account.
     * 
     * @param accountId The account identifier to filter active cards by
     * @return List of active cards (status = 'Y') for the specified account
     */
    public List<Card> getActiveCardsByAccountId(String accountId) {
        return cardRepository.findActiveCardsByAccountId(accountId);
    }

    /**
     * Retrieves all cards with a specific active status.
     * 
     * @param status The card active status (Y=Active, N=Inactive)
     * @return List of cards matching the specified status
     */
    public List<Card> getCardsByStatus(String status) {
        return cardRepository.findByCardActiveStatus(status);
    }

    /**
     * Activates a credit card, enabling it for transactions.
     * 
     * This method sets the card's active status to 'Y', allowing the card
     * to be used for purchases and other transactions.
     * 
     * @param cardNumber The 16-digit card number to activate
     * @return The activated Card entity with updated status
     * @throws IllegalArgumentException if the card is not found
     */
    public Card activateCard(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardNumber));
        card.setCardActiveStatus("Y");
        return cardRepository.save(card);
    }

    /**
     * Deactivates a credit card, preventing it from being used for transactions.
     * 
     * This method sets the card's active status to 'N'. Useful for lost/stolen cards
     * or temporary suspension of card privileges.
     * 
     * @param cardNumber The 16-digit card number to deactivate
     * @return The deactivated Card entity with updated status
     * @throws IllegalArgumentException if the card is not found
     */
    public Card deactivateCard(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardNumber));
        card.setCardActiveStatus("N");
        return cardRepository.save(card);
    }

    /**
     * Searches for cards by the embossed name on the card.
     * 
     * This method performs a partial match search on the embossed name field.
     * 
     * @param name The search term to match against embossed names
     * @return List of cards whose embossed name contains the search term
     */
    public List<Card> searchByEmbossedName(String name) {
        return cardRepository.findByEmbossedNameContaining(name);
    }
}
