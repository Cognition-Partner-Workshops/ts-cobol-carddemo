package com.carddemo.config;

import com.carddemo.model.*;
import com.carddemo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing sample data...");
            initializeUsers();
            initializeCustomers();
            initializeAccounts();
            initializeCards();
            initializeCardXrefs();
            initializeTransactions();
            log.info("Sample data initialization complete.");
        }
    }
    
    private void initializeUsers() {
        userRepository.save(User.builder()
                .userId("ADMIN001")
                .firstName("Admin")
                .lastName("User")
                .password(passwordEncoder.encode("ADMIN001"))
                .userType(User.UserType.ADMIN)
                .build());
        
        userRepository.save(User.builder()
                .userId("USER0001")
                .firstName("Regular")
                .lastName("User")
                .password(passwordEncoder.encode("USER0001"))
                .userType(User.UserType.USER)
                .build());
        
        log.info("Created default users: ADMIN001, USER0001");
    }
    
    private void initializeCustomers() {
        customerRepository.save(Customer.builder()
                .customerId("000000001")
                .firstName("John")
                .middleName("A")
                .lastName("Smith")
                .addressLine1("123 Main Street")
                .addressLine2("Apt 4B")
                .addressLine3("")
                .stateCode("NY")
                .countryCode("USA")
                .zipCode("10001")
                .phoneNumber1("212-555-0101")
                .phoneNumber2("212-555-0102")
                .ssn("123456789")
                .govtIssuedId("NY12345678")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .eftAccountId("EFT0000001")
                .primaryCardHolderInd("Y")
                .ficoCreditScore(750)
                .build());
        
        customerRepository.save(Customer.builder()
                .customerId("000000002")
                .firstName("Jane")
                .middleName("B")
                .lastName("Doe")
                .addressLine1("456 Oak Avenue")
                .addressLine2("")
                .addressLine3("")
                .stateCode("CA")
                .countryCode("USA")
                .zipCode("90210")
                .phoneNumber1("310-555-0201")
                .phoneNumber2("")
                .ssn("987654321")
                .govtIssuedId("CA98765432")
                .dateOfBirth(LocalDate.of(1990, 3, 22))
                .eftAccountId("EFT0000002")
                .primaryCardHolderInd("Y")
                .ficoCreditScore(800)
                .build());
        
        log.info("Created sample customers");
    }
    
    private void initializeAccounts() {
        accountRepository.save(Account.builder()
                .accountId("00000000001")
                .activeStatus("Y")
                .currentBalance(new BigDecimal("1500.00"))
                .creditLimit(new BigDecimal("10000.00"))
                .cashCreditLimit(new BigDecimal("2000.00"))
                .openDate(LocalDate.of(2020, 1, 15))
                .expirationDate(LocalDate.of(2025, 1, 15))
                .reissueDate(LocalDate.of(2023, 1, 15))
                .currentCycleCredit(new BigDecimal("500.00"))
                .currentCycleDebit(new BigDecimal("2000.00"))
                .zipCode("10001")
                .groupId("GRP001")
                .build());
        
        accountRepository.save(Account.builder()
                .accountId("00000000002")
                .activeStatus("Y")
                .currentBalance(new BigDecimal("3200.50"))
                .creditLimit(new BigDecimal("15000.00"))
                .cashCreditLimit(new BigDecimal("3000.00"))
                .openDate(LocalDate.of(2019, 6, 1))
                .expirationDate(LocalDate.of(2024, 6, 1))
                .reissueDate(LocalDate.of(2022, 6, 1))
                .currentCycleCredit(new BigDecimal("1000.00"))
                .currentCycleDebit(new BigDecimal("4200.50"))
                .zipCode("90210")
                .groupId("GRP002")
                .build());
        
        log.info("Created sample accounts");
    }
    
    private void initializeCards() {
        cardRepository.save(Card.builder()
                .cardNumber("4111111111111111")
                .accountId("00000000001")
                .cvvCode("123")
                .embossedName("JOHN A SMITH")
                .expirationDate(LocalDate.of(2025, 12, 31))
                .activeStatus("Y")
                .build());
        
        cardRepository.save(Card.builder()
                .cardNumber("4222222222222222")
                .accountId("00000000002")
                .cvvCode("456")
                .embossedName("JANE B DOE")
                .expirationDate(LocalDate.of(2024, 12, 31))
                .activeStatus("Y")
                .build());
        
        log.info("Created sample cards");
    }
    
    private void initializeCardXrefs() {
        cardXrefRepository.save(CardXref.builder()
                .cardNumber("4111111111111111")
                .customerId("000000001")
                .accountId("00000000001")
                .build());
        
        cardXrefRepository.save(CardXref.builder()
                .cardNumber("4222222222222222")
                .customerId("000000002")
                .accountId("00000000002")
                .build());
        
        log.info("Created sample card cross-references");
    }
    
    private void initializeTransactions() {
        transactionRepository.save(Transaction.builder()
                .transactionId("0000000000000001")
                .typeCode("01")
                .categoryCode(1)
                .source("POS TERM")
                .description("GROCERY STORE PURCHASE")
                .amount(new BigDecimal("125.50"))
                .merchantId("123456789")
                .merchantName("WHOLE FOODS MARKET")
                .merchantCity("NEW YORK")
                .merchantZip("10001")
                .cardNumber("4111111111111111")
                .originTimestamp(LocalDateTime.now().minusDays(5))
                .processTimestamp(LocalDateTime.now().minusDays(5))
                .build());
        
        transactionRepository.save(Transaction.builder()
                .transactionId("0000000000000002")
                .typeCode("01")
                .categoryCode(2)
                .source("ONLINE")
                .description("AMAZON PURCHASE")
                .amount(new BigDecimal("89.99"))
                .merchantId("987654321")
                .merchantName("AMAZON.COM")
                .merchantCity("SEATTLE")
                .merchantZip("98101")
                .cardNumber("4111111111111111")
                .originTimestamp(LocalDateTime.now().minusDays(3))
                .processTimestamp(LocalDateTime.now().minusDays(3))
                .build());
        
        transactionRepository.save(Transaction.builder()
                .transactionId("0000000000000003")
                .typeCode("01")
                .categoryCode(3)
                .source("POS TERM")
                .description("RESTAURANT DINNER")
                .amount(new BigDecimal("75.00"))
                .merchantId("456789123")
                .merchantName("THE ITALIAN PLACE")
                .merchantCity("LOS ANGELES")
                .merchantZip("90210")
                .cardNumber("4222222222222222")
                .originTimestamp(LocalDateTime.now().minusDays(2))
                .processTimestamp(LocalDateTime.now().minusDays(2))
                .build());
        
        log.info("Created sample transactions");
    }
}
