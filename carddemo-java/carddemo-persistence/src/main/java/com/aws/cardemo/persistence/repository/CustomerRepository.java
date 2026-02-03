package com.aws.cardemo.persistence.repository;

import com.aws.cardemo.domain.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Customer entity persistence operations.
 * 
 * This repository provides data access methods for credit card customers in the CardDemo application.
 * It extends JpaRepository to inherit standard CRUD operations and adds custom query methods
 * for customer-specific business requirements.
 * 
 * The repository supports:
 * - Standard CRUD operations (inherited from JpaRepository)
 * - Customer search by name (partial matching)
 * - Customer filtering by state and last name
 * - SSN-based customer lookup for identity verification
 * - Credit score-based customer segmentation
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Finds all customers with a specific last name.
     * 
     * @param lastName The last name to search for (exact match)
     * @return List of customers with the specified last name
     */
    List<Customer> findByLastName(String lastName);

    /**
     * Finds all customers residing in a specific state.
     * 
     * @param stateCode The two-letter state code (e.g., CA, NY, TX)
     * @return List of customers in the specified state
     */
    List<Customer> findByStateCode(String stateCode);

    /**
     * Finds a customer by their Social Security Number.
     * 
     * This method is useful for identity verification and duplicate detection.
     * SSN should be handled securely and access should be restricted.
     * 
     * @param ssn The Social Security Number (9 digits)
     * @return Optional containing the Customer if found, empty Optional otherwise
     */
    Optional<Customer> findBySsn(String ssn);

    /**
     * Searches for customers by name using partial matching.
     * 
     * This query performs a case-sensitive search on both first and last name fields.
     * 
     * @param name The search term to match against customer names
     * @return List of customers whose first or last name contains the search term
     */
    @Query("SELECT c FROM Customer c WHERE c.firstName LIKE %:name% OR c.lastName LIKE %:name%")
    List<Customer> searchByName(@Param("name") String name);

    /**
     * Finds customers with a FICO credit score at or above the specified minimum.
     * 
     * This query is useful for credit limit reviews, promotional offers,
     * and customer segmentation based on creditworthiness.
     * 
     * @param minScore The minimum FICO credit score threshold (typically 300-850)
     * @return List of customers meeting the credit score criteria
     */
    @Query("SELECT c FROM Customer c WHERE c.ficoCreditScore >= :minScore")
    List<Customer> findByMinimumCreditScore(@Param("minScore") Integer minScore);
}
