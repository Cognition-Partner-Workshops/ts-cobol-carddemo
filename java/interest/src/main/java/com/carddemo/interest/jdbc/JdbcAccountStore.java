package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.domain.Account;
import com.carddemo.interest.port.AccountStore;

/**
 * FI-3 on carddemo.account: the read by account id (CBACT04C:372-391) and the update of the
 * account break (350-356). The update names three columns and no other (architecture.md section 3).
 */
public final class JdbcAccountStore implements AccountStore {

    private static final String FIND = """
            SELECT acct_id, acct_curr_bal, acct_group_id, acct_active_status
              FROM carddemo.account
             WHERE acct_id = ?""";
    /** INT-R07: the new current balance; cycle credit and cycle debit to zero. */
    private static final String POST_INTEREST = """
            UPDATE carddemo.account
               SET acct_curr_bal = ?, acct_curr_cyc_credit = 0, acct_curr_cyc_debit = 0
             WHERE acct_id = ?""";

    private final JdbcTemplate jdbc;
    private int accountsUpdated;

    public JdbcAccountStore(JdbcTemplate jdbc, int accountsUpdatedBefore) {
        this.jdbc = jdbc;
        this.accountsUpdated = accountsUpdatedBefore;
    }

    @Override
    public Optional<Account> find(long accountId) {
        List<Account> found = jdbc.query(FIND, (row, rowNumber) -> new Account(
                row.getBigDecimal("acct_id").longValueExact(),
                row.getBigDecimal("acct_curr_bal"),
                row.getString("acct_group_id").stripTrailing(),
                row.getString("acct_active_status").stripTrailing()), BigDecimal.valueOf(accountId));
        return found.stream().findFirst();
    }

    @Override
    public void postInterest(long accountId, BigDecimal newBalance) {
        accountsUpdated += jdbc.update(POST_INTEREST, newBalance, BigDecimal.valueOf(accountId));
    }

    /** Accounts updated by the run so far; part of the step's restart state and of interest_run. */
    public int accountsUpdated() {
        return accountsUpdated;
    }
}
