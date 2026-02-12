package com.aws.carddemo.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.aws.carddemo.customer.dto.CustomerResponse;
import com.aws.carddemo.customer.dto.CustomerUpdateRequest;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.exception.ValidationException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTests {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setFirstName("Jane");
        testCustomer.setMiddleName("M");
        testCustomer.setLastName("Smith");
        testCustomer.setAddressLine1("456 Oak Ave");
        testCustomer.setCity("Chicago");
        testCustomer.setState("IL");
        testCustomer.setZipCode("60601");
        testCustomer.setCountryCode("US");
        testCustomer.setPhoneNumber("3125551234");
        testCustomer.setSsn("987654321");
        testCustomer.setFicoScore((short) 750);
        testCustomer.setAccounts(new ArrayList<>());
    }

    @Test
    void getCustomerReturnsResponse() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        CustomerResponse response = customerService.getCustomer(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.state()).isEqualTo("IL");
        assertThat(response.accounts()).isEmpty();
    }

    @Test
    void getCustomerNotFoundThrows() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void updateCustomerNameFields() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "Janet", null, "Johnson", null, null, null, null, null, null, null);
        customerService.updateCustomer(1L, request);

        assertThat(testCustomer.getFirstName()).isEqualTo("Janet");
        assertThat(testCustomer.getLastName()).isEqualTo("Johnson");
        assertThat(testCustomer.getMiddleName()).isEqualTo("M");
    }

    @Test
    void updateCustomerAddressFields() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, "789 Elm St", "Apt 4B", "Boston", "MA", "02101", null, null);
        customerService.updateCustomer(1L, request);

        assertThat(testCustomer.getAddressLine1()).isEqualTo("789 Elm St");
        assertThat(testCustomer.getAddressLine2()).isEqualTo("Apt 4B");
        assertThat(testCustomer.getCity()).isEqualTo("Boston");
        assertThat(testCustomer.getState()).isEqualTo("MA");
        assertThat(testCustomer.getZipCode()).isEqualTo("02101");
    }

    @Test
    void updateCustomerInvalidStateCodeThrows() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, null, null, null, "XX", null, null, null);
        assertThatThrownBy(() -> customerService.updateCustomer(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid state code");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomerInvalidZipCodeThrows() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, null, null, null, null, "ABCDE", null, null);
        assertThatThrownBy(() -> customerService.updateCustomer(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid ZIP code");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomerValidZipWithExtension() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, null, null, null, null, "02101-4321", null, null);
        customerService.updateCustomer(1L, request);

        assertThat(testCustomer.getZipCode()).isEqualTo("02101-4321");
    }

    @Test
    void updateCustomerPhoneNumber() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                null, null, null, null, null, null, null, null, null, "2125559999");
        customerService.updateCustomer(1L, request);

        assertThat(testCustomer.getPhoneNumber()).isEqualTo("2125559999");
    }

    @Test
    void listCustomersWithLastNameFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(java.util.List.of(testCustomer), pageable, 1);
        when(customerRepository.findByLastNameContainingIgnoreCase(eq("Smith"), eq(pageable))).thenReturn(page);

        Page<CustomerResponse> result = customerService.listCustomers("Smith", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).lastName()).isEqualTo("Smith");
    }

    @Test
    void listCustomersWithoutFilterReturnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(java.util.List.of(testCustomer), pageable, 1);
        when(customerRepository.findAll(pageable)).thenReturn(page);

        Page<CustomerResponse> result = customerService.listCustomers(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listCustomersWithBlankFilterReturnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(java.util.List.of(testCustomer), pageable, 1);
        when(customerRepository.findAll(pageable)).thenReturn(page);

        Page<CustomerResponse> result = customerService.listCustomers("  ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updateCustomerNotFoundThrows() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "New", null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> customerService.updateCustomer(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void updateCustomerAllFieldsAtOnce() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        CustomerUpdateRequest request = new CustomerUpdateRequest(
                "Alice", "B", "Wonder", "1 Rabbit Ln", "Suite 100",
                "Wonderland", "CA", "90001", "US", "8005551234");
        customerService.updateCustomer(1L, request);

        assertThat(testCustomer.getFirstName()).isEqualTo("Alice");
        assertThat(testCustomer.getMiddleName()).isEqualTo("B");
        assertThat(testCustomer.getLastName()).isEqualTo("Wonder");
        assertThat(testCustomer.getAddressLine1()).isEqualTo("1 Rabbit Ln");
        assertThat(testCustomer.getAddressLine2()).isEqualTo("Suite 100");
        assertThat(testCustomer.getCity()).isEqualTo("Wonderland");
        assertThat(testCustomer.getState()).isEqualTo("CA");
        assertThat(testCustomer.getZipCode()).isEqualTo("90001");
        assertThat(testCustomer.getCountryCode()).isEqualTo("US");
        assertThat(testCustomer.getPhoneNumber()).isEqualTo("8005551234");
    }
}
