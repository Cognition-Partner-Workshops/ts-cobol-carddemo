package com.aws.carddemo.service;

import com.aws.carddemo.dto.CustomerDto;
import com.aws.carddemo.entity.Customer;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.mapper.CustomerMapper;
import com.aws.carddemo.repository.CustomerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "#custId")
    public CustomerDto getCustomer(Long custId) {
        Customer customer = customerRepository.findById(custId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "custId", custId));
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerWithCardXrefs(Long custId) {
        Customer customer = customerRepository.findByIdWithCardXrefs(custId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "custId", custId));
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerBySsn(String ssn) {
        Customer customer = customerRepository.findByCustSsn(ssn)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "ssn", ssn));
        return customerMapper.toDto(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerDto> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable).map(customerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> searchByLastName(String lastName) {
        return customerMapper.toDtoList(customerRepository.searchByLastName(lastName));
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> searchByName(String firstName, String lastName) {
        return customerMapper.toDtoList(customerRepository.searchByName(firstName, lastName));
    }

    @Transactional(readOnly = true)
    public Page<CustomerDto> getCustomersByState(String stateCd, Pageable pageable) {
        return customerRepository.findByCustAddrStateCd(stateCd, pageable).map(customerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getCustomersByZip(String zip) {
        return customerMapper.toDtoList(customerRepository.findByCustAddrZip(zip));
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getCustomersByMinFicoScore(Integer minScore) {
        return customerMapper.toDtoList(customerRepository.findByMinFicoScore(minScore));
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> getPrimaryCardHolders() {
        return customerMapper.toDtoList(customerRepository.findPrimaryCardHolders());
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#dto.custId")
    public CustomerDto createCustomer(CustomerDto dto) {
        Customer customer = customerMapper.toEntity(dto);
        customer = customerRepository.save(customer);
        return customerMapper.toDto(customer);
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#custId")
    public CustomerDto updateCustomer(Long custId, CustomerDto dto) {
        Customer customer = customerRepository.findById(custId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "custId", custId));
        customerMapper.updateEntity(dto, customer);
        customer = customerRepository.save(customer);
        return customerMapper.toDto(customer);
    }

    @Transactional
    @CacheEvict(value = "customers", key = "#custId")
    public void deleteCustomer(Long custId) {
        if (!customerRepository.existsById(custId)) {
            throw new ResourceNotFoundException("Customer", "custId", custId);
        }
        customerRepository.deleteById(custId);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long custId) {
        return customerRepository.existsById(custId);
    }
}
