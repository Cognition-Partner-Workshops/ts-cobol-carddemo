package com.carddemo.config;

import com.carddemo.entity.Account;
import com.carddemo.entity.Card;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.Customer;
import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.TransactionCategory;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.entity.TransactionType;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.CardRepository;
import com.carddemo.repository.CustomerRepository;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.TransactionTypeRepository;
import com.carddemo.repository.UserSecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final TransactionCategoryBalanceRepository tranCatBalRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final UserSecurityRepository userSecurityRepository;

    public DataLoader(AccountRepository accountRepository,
                      CardRepository cardRepository,
                      CustomerRepository customerRepository,
                      CardCrossReferenceRepository cardCrossReferenceRepository,
                      TransactionTypeRepository transactionTypeRepository,
                      TransactionCategoryRepository transactionCategoryRepository,
                      TransactionCategoryBalanceRepository tranCatBalRepository,
                      DisclosureGroupRepository disclosureGroupRepository,
                      UserSecurityRepository userSecurityRepository) {
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.customerRepository = customerRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
        this.tranCatBalRepository = tranCatBalRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.userSecurityRepository = userSecurityRepository;
    }

    @Override
    public void run(String... args) {
        if (userSecurityRepository.count() > 0) {
            log.info("Data already loaded, skipping.");
            return;
        }

        log.info("Loading sample data from ASCII files...");
        loadDefaultUsers();
        loadTransactionTypes();
        loadTransactionCategories();
        loadAccounts();
        loadCards();
        loadCustomers();
        loadCardCrossReferences();
        loadTransactionCategoryBalances();
        loadDisclosureGroups();
        log.info("Sample data loading complete.");
    }

    private void loadDefaultUsers() {
        log.info("Loading default users...");
        List<UserSecurity> users = new ArrayList<>();

        UserSecurity admin = new UserSecurity();
        admin.setUsrId("ADMIN");
        admin.setUsrFname("ADMIN");
        admin.setUsrLname("USER");
        admin.setUsrPwd("ADMIN");
        admin.setUsrType("A");
        users.add(admin);

        UserSecurity user = new UserSecurity();
        user.setUsrId("USER0001");
        user.setUsrFname("FIRST");
        user.setUsrLname("USER");
        user.setUsrPwd("USER0001");
        user.setUsrType("U");
        users.add(user);

        userSecurityRepository.saveAll(users);
        log.info("Loaded {} users", users.size());
    }

    private void loadTransactionTypes() {
        log.info("Loading transaction types...");
        List<String> lines = readLines("data/trantype.txt");
        List<TransactionType> types = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 4) continue;
            TransactionType type = new TransactionType();
            type.setTypeCd(line.substring(0, 2));
            type.setTypeDesc(line.substring(2, Math.min(52, line.length())).trim());
            types.add(type);
        }

        transactionTypeRepository.saveAll(types);
        log.info("Loaded {} transaction types", types.size());
    }

    private void loadTransactionCategories() {
        log.info("Loading transaction categories...");
        List<String> lines = readLines("data/trancatg.txt");
        List<TransactionCategory> categories = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 8) continue;
            TransactionCategory cat = new TransactionCategory();
            cat.setTypeCd(line.substring(0, 2));
            cat.setCatCd(Integer.parseInt(line.substring(2, 6).trim()));
            cat.setCatTypeDesc(line.substring(6, Math.min(56, line.length())).trim());
            categories.add(cat);
        }

        transactionCategoryRepository.saveAll(categories);
        log.info("Loaded {} transaction categories", categories.size());
    }

    private void loadAccounts() {
        log.info("Loading accounts...");
        List<String> lines = readLines("data/acctdata.txt");
        List<Account> accounts = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 100) continue;
            Account acct = new Account();
            acct.setAcctId(Long.parseLong(line.substring(0, 11).trim()));
            acct.setActiveStatus(line.substring(11, 12));
            acct.setCurrBal(parseSignedDecimal(line.substring(12, 25)));
            acct.setCreditLimit(parseSignedDecimal(line.substring(25, 38)));
            acct.setCashCreditLimit(parseSignedDecimal(line.substring(38, 51)));
            acct.setOpenDate(parseDate(line.substring(51, 61)));
            acct.setExpirationDate(parseDate(line.substring(61, 71)));
            acct.setReissueDate(parseDate(line.substring(71, 81)));
            acct.setCurrCycCredit(parseSignedDecimal(line.substring(81, 94)));
            acct.setCurrCycDebit(parseSignedDecimal(line.substring(94, 107)));
            acct.setAddrZip(line.substring(107, 117).trim());
            acct.setGroupId(line.substring(117, 127).trim());
            accounts.add(acct);
        }

        accountRepository.saveAll(accounts);
        log.info("Loaded {} accounts", accounts.size());
    }

    private void loadCards() {
        log.info("Loading cards...");
        List<String> lines = readLines("data/carddata.txt");
        List<Card> cards = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 30) continue;
            Card card = new Card();
            card.setCardNum(line.substring(0, 16));
            card.setAcctId(Long.parseLong(line.substring(16, 27).trim()));
            card.setCvvCd(Integer.parseInt(line.substring(27, 30).trim()));
            card.setEmbossedName(line.substring(30, 80).trim());
            card.setExpirationDate(parseDate(line.substring(80, 90)));
            card.setActiveStatus(line.substring(90, 91));
            cards.add(card);
        }

        cardRepository.saveAll(cards);
        log.info("Loaded {} cards", cards.size());
    }

    private void loadCustomers() {
        log.info("Loading customers...");
        List<String> lines = readLines("data/custdata.txt");
        List<Customer> customers = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 200) continue;
            Customer cust = new Customer();
            cust.setCustId(Long.parseLong(line.substring(0, 9).trim()));
            cust.setFirstName(line.substring(9, 34).trim());
            cust.setMiddleName(line.substring(34, 59).trim());
            cust.setLastName(line.substring(59, 84).trim());
            cust.setAddrLine1(line.substring(84, 134).trim());
            cust.setAddrLine2(line.substring(134, 184).trim());
            cust.setAddrLine3(line.substring(184, 234).trim());
            cust.setAddrStateCd(line.substring(234, 236).trim());
            cust.setAddrCountryCd(line.substring(236, 239).trim());
            cust.setAddrZip(line.substring(239, 249).trim());
            cust.setPhoneNum1(line.substring(249, 264).trim());
            cust.setPhoneNum2(line.substring(264, 279).trim());
            cust.setSsn(Long.parseLong(line.substring(279, 288).trim()));
            cust.setGovtIssuedId(line.substring(288, 308).trim());
            cust.setDob(parseDate(line.substring(308, 318)));
            cust.setEftAccountId(line.substring(318, 328).trim());
            cust.setPriCardHolderInd(line.substring(328, 329));
            cust.setFicoCreditScore(Integer.parseInt(line.substring(329, 332).trim()));
            customers.add(cust);
        }

        customerRepository.saveAll(customers);
        log.info("Loaded {} customers", customers.size());
    }

    private void loadCardCrossReferences() {
        log.info("Loading card cross references...");
        List<String> lines = readLines("data/cardxref.txt");
        List<CardCrossReference> xrefs = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 36) continue;
            CardCrossReference xref = new CardCrossReference();
            xref.setCardNum(line.substring(0, 16));
            xref.setCustId(Long.parseLong(line.substring(16, 25).trim()));
            xref.setAcctId(Long.parseLong(line.substring(25, 36).trim()));
            xrefs.add(xref);
        }

        cardCrossReferenceRepository.saveAll(xrefs);
        log.info("Loaded {} card cross references", xrefs.size());
    }

    private void loadTransactionCategoryBalances() {
        log.info("Loading transaction category balances...");
        List<String> lines = readLines("data/tcatbal.txt");
        List<TransactionCategoryBalance> balances = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 24) continue;
            TransactionCategoryBalance bal = new TransactionCategoryBalance();
            bal.setAcctId(Long.parseLong(line.substring(0, 11).trim()));
            bal.setTypeCd(line.substring(11, 13));
            bal.setCatCd(Integer.parseInt(line.substring(13, 17).trim()));
            bal.setBalance(parseSignedDecimal(line.substring(17, 28)));
            balances.add(bal);
        }

        tranCatBalRepository.saveAll(balances);
        log.info("Loaded {} transaction category balances", balances.size());
    }

    private void loadDisclosureGroups() {
        log.info("Loading disclosure groups...");
        List<String> lines = readLines("data/discgrp.txt");
        List<DisclosureGroup> groups = new ArrayList<>();

        for (String line : lines) {
            if (line.length() < 20) continue;
            DisclosureGroup dg = new DisclosureGroup();
            dg.setAcctGroupId(line.substring(0, 10));
            dg.setTranTypeCd(line.substring(10, 12));
            dg.setTranCatCd(Integer.parseInt(line.substring(12, 16).trim()));
            dg.setIntRate(parseSignedDecimal(line.substring(16, 22)));
            groups.add(dg);
        }

        disclosureGroupRepository.saveAll(groups);
        log.info("Loaded {} disclosure groups", groups.size());
    }

    static BigDecimal parseSignedDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return BigDecimal.ZERO;
        }

        char lastChar = raw.charAt(raw.length() - 1);
        String digits = raw.substring(0, raw.length() - 1);
        int sign = 1;
        int lastDigit;

        switch (lastChar) {
            case '{' -> lastDigit = 0;
            case 'A' -> lastDigit = 1;
            case 'B' -> lastDigit = 2;
            case 'C' -> lastDigit = 3;
            case 'D' -> lastDigit = 4;
            case 'E' -> lastDigit = 5;
            case 'F' -> lastDigit = 6;
            case 'G' -> lastDigit = 7;
            case 'H' -> lastDigit = 8;
            case 'I' -> lastDigit = 9;
            case '}' -> { lastDigit = 0; sign = -1; }
            case 'J' -> { lastDigit = 1; sign = -1; }
            case 'K' -> { lastDigit = 2; sign = -1; }
            case 'L' -> { lastDigit = 3; sign = -1; }
            case 'M' -> { lastDigit = 4; sign = -1; }
            case 'N' -> { lastDigit = 5; sign = -1; }
            case 'O' -> { lastDigit = 6; sign = -1; }
            case 'P' -> { lastDigit = 7; sign = -1; }
            case 'Q' -> { lastDigit = 8; sign = -1; }
            case 'R' -> { lastDigit = 9; sign = -1; }
            default -> {
                try {
                    return new BigDecimal(raw).movePointLeft(2);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            }
        }

        String fullDigits = digits + lastDigit;
        BigDecimal value = new BigDecimal(fullDigits).movePointLeft(2);
        return sign < 0 ? value.negate() : value;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty() || raw.equals("0000-00-00")) return null;
        try {
            return LocalDate.parse(raw, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<String> readLines(String resourcePath) {
        List<String> lines = new ArrayList<>();
        try {
            InputStream is = new ClassPathResource(resourcePath).getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            log.warn("Could not read resource: {}", resourcePath);
        }
        return lines;
    }
}
