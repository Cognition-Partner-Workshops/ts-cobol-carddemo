package com.carddemo.interest.jdbc.calc;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.RecordValues;
import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.AccountStore;

/**
 * carddemo.account by account id (CBACT04C:41-45, 372-391) and the update of the account break
 * (INT-R07, CBACT04C:350-356). The UPDATE sets acct_curr_bal, acct_curr_cyc_credit and
 * acct_curr_cyc_debit and no other column (architecture.md section 3, standards S3.6).
 */
public final class JdbcAccountStore implements AccountStore {

    static final String TABLE = "carddemo.account";

    private static final String FIND = "SELECT acct_id, acct_active_status, acct_curr_bal, acct_group_id FROM " + TABLE
            + " WHERE acct_id = ?";
    private static final String POST_INTEREST = "UPDATE " + TABLE
            + " SET acct_curr_bal = ?, acct_curr_cyc_credit = 0, acct_curr_cyc_debit = 0 WHERE acct_id = ?";

    /** Checks the new balance against the picture of CVACT01Y/ACCT-CURR-BAL before it is stored (standards S3.4). */
    private static final RecordCodec PICTURES = RecordFormat.fixture().codec(AccountRecord.LAYOUT);

    private final JdbcTemplate jdbc;

    public JdbcAccountStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Data-map rows CVACT01Y/ACCT-ID, ACCT-ACTIVE-STATUS, ACCT-CURR-BAL, ACCT-GROUP-ID. */
    @Override
    public Optional<Account> find(long accountId) {
        return jdbc.query(FIND, (rs, rowNumber) -> AccountRecord.from(RecordValues.blank(AccountRecord.LAYOUT)
                .with("ACCT-ID", rs.getLong("acct_id"))
                .with("ACCT-ACTIVE-STATUS", rs.getString("acct_active_status"))
                .with("ACCT-CURR-BAL", rs.getBigDecimal("acct_curr_bal"))
                .with("ACCT-GROUP-ID", rs.getString("acct_group_id"))), accountId).stream().findFirst();
    }

    @Override
    public void postInterest(long accountId, BigDecimal newBalance) {
        PICTURES.withField(new byte[AccountRecord.LENGTH], "ACCT-CURR-BAL", newBalance, new RecordOrigin(TABLE, 0));
        if (jdbc.update(POST_INTEREST, newBalance, accountId) != 1) {
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT,
                    "the account of an account break is not in " + TABLE + " (CBACT04C:356-369)");
        }
    }
}
