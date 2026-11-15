package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.AccountStore;

/**
 * FI-3 on carddemo.account: the account by its id (CBACT04C:41-45, 372-391) and the update of the
 * account break (350-356). The update names three columns; every other column of the row, the
 * 178-byte acct_filler included, is never written (standards S3.6, rule E4, architecture.md section 3).
 */
public final class JdbcAccountStore implements AccountStore {

    public static final String TABLE = "carddemo.account";

    /** Data-map rows CVACT01Y/ACCT-ID, ACCT-ACTIVE-STATUS, ACCT-CURR-BAL and ACCT-GROUP-ID. */
    private static final String FIND = """
            SELECT acct_id, acct_active_status, acct_curr_bal, acct_group_id
              FROM carddemo.account
             WHERE acct_id = ?""";

    /** INT-R07 (CBACT04C:352-354): data-map rows CVACT01Y/ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT. */
    private static final String POST_INTEREST = """
            UPDATE carddemo.account
               SET acct_curr_bal = ?, acct_curr_cyc_credit = 0, acct_curr_cyc_debit = 0
             WHERE acct_id = ?""";

    private final JdbcOperations jdbc;

    public JdbcAccountStore(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Account> find(long accountId) {
        try {
            List<Account> accounts = jdbc.query(FIND, (row, rowNumber) -> new Account(
                    row.getBigDecimal(1).longValueExact(),
                    Texts.withoutTrailingSpaces(row.getString(2)),
                    row.getBigDecimal(3),
                    Texts.withoutTrailingSpaces(row.getString(4))), accountId);
            return accounts.stream().findFirst();
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read table " + TABLE, e);
        }
    }

    @Override
    public void postInterest(long accountId, BigDecimal newBalance) {
        int updated;
        try {
            updated = jdbc.update(POST_INTEREST, newBalance, accountId);
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot update table " + TABLE, e);
        }
        if (updated != 1) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                    "the update of an account changed " + updated + " rows of " + TABLE + ", not 1");
        }
    }
}
