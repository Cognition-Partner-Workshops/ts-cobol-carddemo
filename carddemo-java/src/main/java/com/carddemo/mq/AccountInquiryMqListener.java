package com.carddemo.mq;

import com.carddemo.dto.AccountRequest;
import com.carddemo.dto.AccountResponse;
import com.carddemo.entity.Account;
import com.carddemo.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * MQ Listener for account inquiry messages - migrated from Phase 5c COACCT01 (CDRA).
 * Original COBOL COACCT01: CICS transaction that responds to account inquiry MQ messages.
 * Reads REQUEST-TYPE='ACCT' + REQUEST-ID + ACCOUNT-NUMBER from MQ,
 * reads VSAM account data, responds with account data (300 bytes).
 */
@Component
public class AccountInquiryMqListener {

    private static final Logger log = LoggerFactory.getLogger(AccountInquiryMqListener.class);

    private final AccountRepository accountRepository;
    private final JmsTemplate jmsTemplate;

    public AccountInquiryMqListener(AccountRepository accountRepository, JmsTemplate jmsTemplate) {
        this.accountRepository = accountRepository;
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = "${carddemo.mq.acct-request-queue:DEV.QUEUE.ACCT.REQ}",
                 containerFactory = "jmsListenerContainerFactory")
    public void onAccountRequest(String message) {
        log.info("Received account inquiry request from MQ");
        try {
            AccountRequest request = AccountRequest.parse(message);

            if (!"ACCT".equals(request.requestType())) {
                log.warn("Invalid request type: {}", request.requestType());
                return;
            }

            Long acctId = Long.parseLong(request.accountNumber().trim());
            Account account = accountRepository.findById(acctId).orElse(null);

            String accountData;
            if (account != null) {
                // Format account data as a fixed-length string (replaces COBOL 300-byte record)
                accountData = formatAccountData(account);
            } else {
                accountData = String.format("%-300s", "ACCOUNT NOT FOUND");
            }

            AccountResponse response = new AccountResponse("ACCT", request.requestId(), accountData);
            jmsTemplate.convertAndSend("DEV.QUEUE.ACCT.RESP", response.toMessage());
            log.info("Sent account response for account: {} request ID: {}",
                    request.accountNumber(), request.requestId());
        } catch (Exception e) {
            log.error("Error processing account inquiry: {}", e.getMessage(), e);
        }
    }

    /**
     * Format account entity as a fixed-length string matching CVACT01Y copybook layout.
     */
    private String formatAccountData(Account account) {
        StringBuilder sb = new StringBuilder(300);
        sb.append(String.format("%-11s", account.getAcctId()));
        sb.append(String.format("%-1s", account.getActiveStatus() != null ? account.getActiveStatus() : " "));
        sb.append(String.format("%-14s", account.getCurrentBalance() != null ? account.getCurrentBalance().toPlainString() : "0"));
        sb.append(String.format("%-14s", account.getCreditLimit() != null ? account.getCreditLimit().toPlainString() : "0"));
        sb.append(String.format("%-14s", account.getCashCreditLimit() != null ? account.getCashCreditLimit().toPlainString() : "0"));
        sb.append(String.format("%-10s", account.getOpenDate() != null ? account.getOpenDate() : ""));
        sb.append(String.format("%-10s", account.getExpirationDate() != null ? account.getExpirationDate() : ""));
        sb.append(String.format("%-10s", account.getReissueDate() != null ? account.getReissueDate() : ""));
        sb.append(String.format("%-14s", account.getCurrentCycleCredit() != null ? account.getCurrentCycleCredit().toPlainString() : "0"));
        sb.append(String.format("%-14s", account.getCurrentCycleDebit() != null ? account.getCurrentCycleDebit().toPlainString() : "0"));
        sb.append(String.format("%-10s", account.getAddressZip() != null ? account.getAddressZip() : ""));
        sb.append(String.format("%-10s", account.getGroupId() != null ? account.getGroupId() : ""));

        // Pad to 300 characters
        while (sb.length() < 300) {
            sb.append(' ');
        }
        return sb.substring(0, 300);
    }
}
