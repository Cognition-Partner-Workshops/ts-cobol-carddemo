package com.carddemo.repository;

import com.carddemo.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testTransaction = new Transaction();
        testTransaction.setTransactionId("TXN0000000000001");
        testTransaction.setTypeCode("SA");
        testTransaction.setCategoryCode(1001);
        testTransaction.setSource("ONLINE");
        testTransaction.setDescription("Test transaction");
        testTransaction.setAmount(new BigDecimal("150.00"));
        testTransaction.setMerchantId(123456789L);
        testTransaction.setMerchantName("Test Merchant");
        testTransaction.setMerchantCity("New York");
        testTransaction.setMerchantZip("10001");
        testTransaction.setCardNumber("4111111111111111");
        testTransaction.setOriginationTimestamp(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        testTransaction.setProcessingTimestamp(LocalDateTime.of(2024, 1, 15, 10, 30, 5));
    }

    @Test
    void testSaveAndFindById() {
        Transaction saved = transactionRepository.save(testTransaction);
        
        Optional<Transaction> found = transactionRepository.findById(saved.getTransactionId());
        
        assertTrue(found.isPresent());
        assertEquals(testTransaction.getDescription(), found.get().getDescription());
    }

    @Test
    void testFindByCardNumber() {
        transactionRepository.save(testTransaction);
        
        List<Transaction> transactions = transactionRepository.findByCardNumber("4111111111111111");
        
        assertEquals(1, transactions.size());
    }

    @Test
    void testFindByCardNumberWithPagination() {
        transactionRepository.save(testTransaction);
        
        Page<Transaction> page = transactionRepository.findByCardNumber(
                "4111111111111111", PageRequest.of(0, 10));
        
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void testFindByTypeCode() {
        transactionRepository.save(testTransaction);
        
        List<Transaction> transactions = transactionRepository.findByTypeCode("SA");
        
        assertEquals(1, transactions.size());
    }

    @Test
    void testFindByCategoryCode() {
        transactionRepository.save(testTransaction);
        
        List<Transaction> transactions = transactionRepository.findByCategoryCode(1001);
        
        assertEquals(1, transactions.size());
    }

    @Test
    void testFindByOriginationTimestampBetween() {
        transactionRepository.save(testTransaction);
        
        List<Transaction> transactions = transactionRepository.findByOriginationTimestampBetween(
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59, 59)
        );
        
        assertEquals(1, transactions.size());
    }

    @Test
    void testFindByCardNumberAndOriginationTimestampBetween() {
        transactionRepository.save(testTransaction);
        
        List<Transaction> transactions = transactionRepository.findByCardNumberAndOriginationTimestampBetween(
                "4111111111111111",
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59, 59)
        );
        
        assertEquals(1, transactions.size());
    }

    @Test
    void testFindByMerchantId() {
        transactionRepository.save(testTransaction);
        
        List<Transaction> transactions = transactionRepository.findByMerchantId(123456789L);
        
        assertEquals(1, transactions.size());
    }

    @Test
    void testFindByAmountGreaterThan() {
        transactionRepository.save(testTransaction);
        
        List<Transaction> transactions = transactionRepository.findByAmountGreaterThan(
                new BigDecimal("100.00"));
        
        assertEquals(1, transactions.size());
    }

    @Test
    void testSumAmountByCardNumber() {
        transactionRepository.save(testTransaction);
        
        BigDecimal sum = transactionRepository.sumAmountByCardNumber("4111111111111111");
        
        assertEquals(0, new BigDecimal("150.00").compareTo(sum));
    }

    @Test
    void testCountByTypeCode() {
        transactionRepository.save(testTransaction);
        
        long count = transactionRepository.countByTypeCode("SA");
        
        assertEquals(1, count);
    }

    @Test
    void testFindByCardNumberOrderByOriginationTimestampDesc() {
        transactionRepository.save(testTransaction);
        
        Page<Transaction> page = transactionRepository.findByCardNumberOrderByOriginationTimestampDesc(
                "4111111111111111", PageRequest.of(0, 10));
        
        assertEquals(1, page.getTotalElements());
    }
}
