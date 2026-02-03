package com.aws.cardemo.services;

import com.aws.cardemo.domain.entity.Customer;
import com.aws.cardemo.persistence.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing Customer business logic.
 * 
 * This service provides the business layer for customer operations in the CardDemo application.
 * It encapsulates all customer-related business rules and coordinates with the persistence layer
 * through the CustomerRepository. All methods are transactional to ensure data consistency.
 * 
 * Key responsibilities include:
 * - CRUD operations for customer management
 * - Customer search and filtering by various criteria
 * - Credit score-based customer segmentation
 * - SSN-based customer lookup for identity verification
 * 
 * @author CardDemo Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Retrieves all customers from the database.
     * 
     * @return List of all Customer entities in the system
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    /**
     * Retrieves a customer by their unique identifier.
     * 
     * @param customerId The unique customer identifier (9 characters max)
     * @return Optional containing the Customer if found, empty Optional otherwise
     */
    public Optional<Customer> getCustomerById(String customerId) {
        return customerRepository.findById(customerId);
    }

    /**
     * Creates a new customer in the system.
     * 
     * @param customer The Customer entity to create
     * @return The created Customer with any generated values populated
     */
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    /**
     * Updates an existing customer in the system.
     * 
     * @param customer The Customer entity with updated values
     * @return The updated Customer entity
     */
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    /**
     * Deletes a customer from the system.
     * 
     * This is a hard delete operation. Consider implementing soft delete for production use.
     * Associated accounts and cards should be handled appropriately before deletion.
     * 
     * @param customerId The unique identifier of the customer to delete
     */
    public void deleteCustomer(String customerId) {
        customerRepository.deleteById(customerId);
    }

    /**
     * Retrieves all customers with a specific last name.
     * 
     * @param lastName The last name to search for (exact match)
     * @return List of customers with the specified last name
     */
    public List<Customer> getCustomersByLastName(String lastName) {
        return customerRepository.findByLastName(lastName);
    }

    /**
     * Retrieves all customers residing in a specific state.
     * 
     * @param stateCode The two-letter state code (e.g., CA, NY, TX)
     * @return List of customers in the specified state
     */
    public List<Customer> getCustomersByState(String stateCode) {
        return customerRepository.findByStateCode(stateCode);
    }

    /**
     * Retrieves a customer by their Social Security Number.
     * 
     * This method is useful for identity verification and duplicate detection.
     * SSN should be handled securely and access should be restricted.
     * 
     * @param ssn The Social Security Number (9 digits)
     * @return Optional containing the Customer if found, empty Optional otherwise
     */
    public Optional<Customer> getCustomerBySsn(String ssn) {
        return customerRepository.findBySsn(ssn);
    }

    /**
     * Searches for customers by name using partial matching.
     * 
     * This method performs a case-insensitive search on both first and last name fields.
     * 
     * @param name The search term to match against customer names
     * @return List of customers whose first or last name contains the search term
     */
    public List<Customer> searchCustomersByName(String name) {
        return customerRepository.searchByName(name);
    }

    /**
     * Retrieves customers with a FICO credit score at or above the specified minimum.
     * 
     * This method is useful for credit limit reviews, promotional offers,
     * and customer segmentation based on creditworthiness.
     * 
     * @param minScore The minimum FICO credit score threshold (typically 300-850)
     * @return List of customers meeting the credit score criteria
     */
    public List<Customer> getCustomersByMinimumCreditScore(Integer minScore) {
        return customerRepository.findByMinimumCreditScore(minScore);
    }
}
