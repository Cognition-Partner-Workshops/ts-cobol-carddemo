package com.carddemo.batch;

import com.carddemo.entity.Account;
import com.carddemo.entity.CardCrossReference;
import com.carddemo.entity.DisclosureGroup;
import com.carddemo.entity.DisclosureGroupId;
import com.carddemo.entity.Transaction;
import com.carddemo.entity.TransactionCategoryBalance;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardCrossReferenceRepository;
import com.carddemo.repository.DisclosureGroupRepository;
import com.carddemo.repository.TransactionCategoryBalanceRepository;
import com.carddemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InterestCalculationJob {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationJob.class);
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    private final TransactionCategoryBalanceRepository tranCatBalRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public InterestCalculationJob(TransactionCategoryBalanceRepository tranCatBalRepository,
                                  CardCrossReferenceRepository cardCrossReferenceRepository,
                                  DisclosureGroupRepository disclosureGroupRepository,
                                  AccountRepository accountRepository,
                                  TransactionRepository transactionRepository) {
        this.tranCatBalRepository = tranCatBalRepository;
        this.cardCrossReferenceRepository = cardCrossReferenceRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public int execute(LocalDate calculationDate) {
        log.info("START OF EXECUTION OF INTEREST CALCULATION JOB");

        List<TransactionCategoryBalance> allBalances = tranCatBalRepository.findAll();
        int recordCount = 0;
        Long lastAcctId = null;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (TransactionCategoryBalance catBal : allBalances) {
            recordCount++;

            if (lastAcctId != null && !catBal.getAcctId().equals(lastAcctId)) {
                updateAccountWithInterest(lastAcctId, totalInterest);
                totalInterest = BigDecimal.ZERO;
            }

            lastAcctId = catBal.getAcctId();

            Account account = accountRepository.findById(catBal.getAcctId()).orElse(null);
            if (account == null) {
                continue;
            }

            String groupId = account.getGroupId() != null ? account.getGroupId().trim() : "DEFAULT";
            if (groupId.isEmpty()) {
                groupId = "DEFAULT";
            }

            DisclosureGroupId dgId = new DisclosureGroupId(
                    String.format("%-10s", groupId),
                    catBal.getTypeCd(),
                    catBal.getCatCd());
            Optional<DisclosureGroup> discGroup = disclosureGroupRepository.findById(dgId);

            if (discGroup.isEmpty()) {
                dgId = new DisclosureGroupId(
                        String.format("%-10s", "DEFAULT"),
                        catBal.getTypeCd(),
                        catBal.getCatCd());
                discGroup = disclosureGroupRepository.findById(dgId);
            }

            if (discGroup.isPresent() && discGroup.get().getIntRate().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal monthlyRate = discGroup.get().getIntRate()
                        .divide(MONTHS_PER_YEAR, 10, RoundingMode.HALF_UP)
                        .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
                BigDecimal interest = catBal.getBalance()
                        .multiply(monthlyRate)
                        .setScale(2, RoundingMode.HALF_UP);

                totalInterest = totalInterest.add(interest);

                if (interest.compareTo(BigDecimal.ZERO) != 0) {
                    createInterestTransaction(catBal, interest, calculationDate);
                }
            }
        }

        if (lastAcctId != null) {
            updateAccountWithInterest(lastAcctId, totalInterest);
        }

        log.info("Records processed: {}", recordCount);
        log.info("END OF EXECUTION OF INTEREST CALCULATION JOB");
        return recordCount;
    }

    private void updateAccountWithInterest(Long acctId, BigDecimal totalInterest) {
        Account account = accountRepository.findById(acctId).orElse(null);
        if (account != null) {
            account.setCurrBal(account.getCurrBal().add(totalInterest));
            account.setCurrCycCredit(BigDecimal.ZERO);
            account.setCurrCycDebit(BigDecimal.ZERO);
            accountRepository.save(account);
        }
    }

    private void createInterestTransaction(TransactionCategoryBalance catBal,
                                           BigDecimal interest,
                                           LocalDate calcDate) {
        String tranId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findByAcctId(catBal.getAcctId());
        String cardNum = xrefs.isEmpty() ? "0000000000000000" : xrefs.get(0).getCardNum();

        Transaction interestTran = new Transaction();
        interestTran.setTranId(tranId);
        interestTran.setTypeCd("01");
        interestTran.setCatCd(5);
        interestTran.setSource("SYSTEM");
        interestTran.setDescription("Interest charge");
        interestTran.setAmount(interest);
        interestTran.setCardNum(cardNum);
        interestTran.setOrigTs(calcDate.atStartOfDay());
        interestTran.setProcTs(LocalDateTime.now());

        transactionRepository.save(interestTran);
    }
}
