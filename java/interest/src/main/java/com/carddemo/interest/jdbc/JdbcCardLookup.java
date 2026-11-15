package com.carddemo.interest.jdbc;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.CardLookup;

/**
 * FI-3 on carddemo.card_xref: the card numbers of an account through the index card_xref_acct_ix,
 * the replacement of the alternate key of the cross-reference (CBACT04C:34-39, 393-413; gap G08).
 */
public final class JdbcCardLookup implements CardLookup {

    public static final String TABLE = "carddemo.card_xref";

    /** Data-map rows CVACT03Y/XREF-CARD-NUM by CVACT03Y/XREF-ACCT-ID; card-number order is byte order. */
    private static final String CARDS_OF_ACCOUNT = """
            SELECT xref_card_num
              FROM carddemo.card_xref
             WHERE xref_acct_id = ?
             ORDER BY xref_card_num COLLATE "C\"""";

    private final JdbcOperations jdbc;

    public JdbcCardLookup(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> cardNumbersOf(long accountId) {
        try {
            return jdbc.query(CARDS_OF_ACCOUNT,
                    (row, rowNumber) -> Texts.withoutTrailingSpaces(row.getString(1)), accountId);
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read table " + TABLE, e);
        }
    }
}
