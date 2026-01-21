package com.carddemo.repository;

import com.carddemo.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for Customer entity operations.
 * Provides CRUD operations and custom queries for customer management.
 *
 * <p>Replaces mainframe VSAM file operations for CUSTFILE.
 *
 * @see Customer
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find customer by SSN.
     *
     * @param ssn the Social Security Number
     * @return optional containing the customer if found
     */
    Optional<Customer> findBySsn(Long ssn);

    /**
     * Find customers by last name.
     *
     * @param lastName the last name to search for
     * @return list of customers with matching last name
     */
    List<Customer> findByLastName(String lastName);

    /**
     * Find customers by last name with pagination.
     *
     * @param lastName the last name to search for
     * @param pageable pagination information
     * @return page of customers with matching last name
     */
    Page<Customer> findByLastName(String lastName, Pageable pageable);

    /**
     * Find customers by state code.
     *
     * @param stateCode the two-letter state code
     * @return list of customers in the specified state
     */
    List<Customer> findByStateCode(String stateCode);

    /**
     * Find customers by ZIP code.
     *
     * @param zipCode the ZIP code to search for
     * @return list of customers with matching ZIP code
     */
    List<Customer> findByZipCode(String zipCode);

    /**
     * Search customers by first name or last name containing the search term.
     *
     * @param firstName the first name search term
     * @param lastName the last name search term
     * @return list of matching customers
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))")
    List<Customer> searchByName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    /**
     * Find customers who are primary card holders.
     *
     * @return list of primary card holder customers
     */
    List<Customer> findByPrimaryCardHolderIndicator(String indicator);

    /**
     * Find customers with FICO score above threshold.
     *
     * @param score the minimum FICO score
     * @return list of customers with FICO score above threshold
     */
    List<Customer> findByFicoCreditScoreGreaterThanEqual(Integer score);

    /**
     * Count customers by state code.
     *
     * @param stateCode the state code to count
     * @return count of customers in the specified state
     */
    long countByStateCode(String stateCode);
}
