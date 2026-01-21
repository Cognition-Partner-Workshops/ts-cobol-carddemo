package com.carddemo.repository;

import com.carddemo.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * JPA repository for Card entity operations.
 * Provides CRUD operations and custom queries for card management.
 *
 * <p>Replaces mainframe VSAM file operations for CARDFILE.
 *
 * @see Card
 */
@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    /**
     * Find all cards for a specific account.
     *
     * @param accountId the account identifier
     * @return list of cards associated with the account
     */
    List<Card> findByAccountId(Long accountId);

    /**
     * Find all cards for a specific customer.
     *
     * @param customerId the customer identifier
     * @return list of cards associated with the customer
     */
    List<Card> findByCustomerId(Long customerId);

    /**
     * Find all cards for a specific customer with pagination.
     *
     * @param customerId the customer identifier
     * @param pageable pagination information
     * @return page of cards associated with the customer
     */
    Page<Card> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Find all cards with a specific active status.
     *
     * @param activeStatus the status to filter by ('Y' for active, 'N' for inactive)
     * @return list of cards matching the status
     */
    List<Card> findByActiveStatus(String activeStatus);

    /**
     * Find cards expiring before a specific date.
     *
     * @param date the expiration date threshold
     * @return list of cards expiring before the specified date
     */
    List<Card> findByExpirationDateBefore(LocalDate date);

    /**
     * Find cards expiring between two dates.
     *
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return list of cards expiring within the date range
     */
    List<Card> findByExpirationDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find active cards for a specific account.
     *
     * @param accountId the account identifier
     * @param activeStatus the active status
     * @return list of active cards for the account
     */
    List<Card> findByAccountIdAndActiveStatus(Long accountId, String activeStatus);

    /**
     * Count cards by active status.
     *
     * @param activeStatus the status to count
     * @return count of cards with the specified status
     */
    long countByActiveStatus(String activeStatus);

    /**
     * Count cards for a specific account.
     *
     * @param accountId the account identifier
     * @return count of cards for the account
     */
    long countByAccountId(Long accountId);
}
