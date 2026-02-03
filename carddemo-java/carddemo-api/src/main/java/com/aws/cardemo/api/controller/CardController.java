package com.aws.cardemo.api.controller;

import com.aws.cardemo.domain.entity.Card;
import com.aws.cardemo.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for managing Card resources.
 * 
 * This controller provides endpoints for CRUD operations on credit cards,
 * as well as card lifecycle management including activation and deactivation.
 * It handles all card-related operations in the CardDemo application, supporting
 * the modernized mainframe credit card management system.
 * 
 * All endpoints are prefixed with /api/v1/cards and return JSON responses.
 * The controller delegates business logic to the CardService layer.
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card", description = "Card management APIs for credit card operations")
public class CardController {

    private final CardService cardService;

    /**
     * Retrieves all cards from the system.
     * 
     * This endpoint returns a complete list of all credit cards stored in the database.
     * Use with caution in production environments as it may return a large dataset.
     * Consider implementing pagination for production use.
     * 
     * @return ResponseEntity containing a list of all Card entities with HTTP 200 status
     */
    @GetMapping
    @Operation(summary = "Get all cards", 
               description = "Retrieves a complete list of all credit cards in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all cards")
    })
    public ResponseEntity<List<Card>> getAllCards() {
        return ResponseEntity.ok(cardService.getAllCards());
    }

    /**
     * Retrieves a specific card by its card number.
     * 
     * This endpoint fetches a single card based on the provided 16-digit card number.
     * Returns HTTP 404 if the card is not found.
     * 
     * @param cardNumber The 16-digit card number to retrieve
     * @return ResponseEntity containing the Card if found, or HTTP 404 if not found
     */
    @GetMapping("/{cardNumber}")
    @Operation(summary = "Get card by number", 
               description = "Retrieves a specific card using its 16-digit card number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card found and returned successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found with the given number")
    })
    public ResponseEntity<Card> getCardByNumber(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber) {
        return cardService.getCardByNumber(cardNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new credit card.
     * 
     * This endpoint accepts card details in the request body and creates a new card
     * in the system. The card data is validated before creation, including the
     * associated account ID and card details.
     * 
     * @param card The Card entity containing the details for the new card
     * @return ResponseEntity containing the created Card with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new card", 
               description = "Creates a new credit card with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Card created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid card data provided")
    })
    public ResponseEntity<Card> createCard(
            @Parameter(description = "Card details for creation", required = true)
            @Valid @RequestBody Card card) {
        Card created = cardService.createCard(card);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing credit card.
     * 
     * This endpoint updates the card identified by the cardNumber path parameter
     * with the data provided in the request body. The card number in the path takes
     * precedence over any number in the request body.
     * 
     * @param cardNumber The 16-digit card number of the card to update
     * @param card The Card entity containing the updated details
     * @return ResponseEntity containing the updated Card with HTTP 200 status
     */
    @PutMapping("/{cardNumber}")
    @Operation(summary = "Update an existing card", 
               description = "Updates an existing card with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid card data provided"),
        @ApiResponse(responseCode = "404", description = "Card not found with the given number")
    })
    public ResponseEntity<Card> updateCard(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber,
            @Parameter(description = "Updated card details", required = true)
            @Valid @RequestBody Card card) {
        card.setCardNumber(cardNumber);
        Card updated = cardService.updateCard(card);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a credit card from the system.
     * 
     * This endpoint permanently removes the card identified by the cardNumber.
     * This operation cannot be undone. Consider implementing soft delete for production use.
     * Note: Associated transactions should be handled appropriately before deletion.
     * 
     * @param cardNumber The 16-digit card number of the card to delete
     * @return ResponseEntity with HTTP 204 (No Content) status on successful deletion
     */
    @DeleteMapping("/{cardNumber}")
    @Operation(summary = "Delete a card", 
               description = "Permanently removes a card from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found with the given number")
    })
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber) {
        cardService.deleteCard(cardNumber);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all cards associated with a specific account.
     * 
     * This endpoint returns all cards (both active and inactive) linked to the
     * specified account ID. Useful for viewing all cards issued under an account.
     * 
     * @param accountId The account identifier to filter cards by
     * @return ResponseEntity containing a list of cards for the specified account
     */
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get cards by account ID", 
               description = "Retrieves all cards associated with a specific account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved cards for account")
    })
    public ResponseEntity<List<Card>> getCardsByAccountId(
            @Parameter(description = "Account identifier", required = true)
            @PathVariable String accountId) {
        return ResponseEntity.ok(cardService.getCardsByAccountId(accountId));
    }

    /**
     * Retrieves only active cards for a specific account.
     * 
     * This endpoint returns cards that are currently active (status = 'Y') for the
     * specified account ID. Useful for determining which cards can be used for transactions.
     * 
     * @param accountId The account identifier to filter active cards by
     * @return ResponseEntity containing a list of active cards for the specified account
     */
    @GetMapping("/account/{accountId}/active")
    @Operation(summary = "Get active cards by account ID", 
               description = "Retrieves only active cards for a specific account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved active cards for account")
    })
    public ResponseEntity<List<Card>> getActiveCardsByAccountId(
            @Parameter(description = "Account identifier", required = true)
            @PathVariable String accountId) {
        return ResponseEntity.ok(cardService.getActiveCardsByAccountId(accountId));
    }

    /**
     * Activates a credit card.
     * 
     * This endpoint changes the card status to active ('Y'), allowing the card
     * to be used for transactions. Typically called after card issuance or
     * when reactivating a previously deactivated card.
     * 
     * @param cardNumber The 16-digit card number of the card to activate
     * @return ResponseEntity containing the activated Card with updated status
     */
    @PostMapping("/{cardNumber}/activate")
    @Operation(summary = "Activate a card", 
               description = "Activates a card, enabling it for transactions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card activated successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found with the given number")
    })
    public ResponseEntity<Card> activateCard(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber) {
        Card activated = cardService.activateCard(cardNumber);
        return ResponseEntity.ok(activated);
    }

    /**
     * Deactivates a credit card.
     * 
     * This endpoint changes the card status to inactive ('N'), preventing the card
     * from being used for transactions. Useful for lost/stolen cards or temporary
     * suspension of card privileges.
     * 
     * @param cardNumber The 16-digit card number of the card to deactivate
     * @return ResponseEntity containing the deactivated Card with updated status
     */
    @PostMapping("/{cardNumber}/deactivate")
    @Operation(summary = "Deactivate a card", 
               description = "Deactivates a card, preventing it from being used for transactions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found with the given number")
    })
    public ResponseEntity<Card> deactivateCard(
            @Parameter(description = "16-digit card number", required = true)
            @PathVariable String cardNumber) {
        Card deactivated = cardService.deactivateCard(cardNumber);
        return ResponseEntity.ok(deactivated);
    }
}
