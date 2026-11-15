package com.carddemo.interest.jdbc.calc;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.port.CardLookup;

/**
 * Card numbers by account id from carddemo.card_xref, through card_xref_acct_ix, the replacement
 * of the alternate key of the cross-reference (CBACT04C:34-39, 393-413; data-map rows
 * CVACT03Y/XREF-ACCT-ID and XREF-CARD-NUM, gap G08).
 */
public final class JdbcCardLookup implements CardLookup {

    private static final String CARDS_OF_ACCOUNT = "SELECT xref_card_num FROM carddemo.card_xref"
            + " WHERE xref_acct_id = ? ORDER BY xref_card_num COLLATE \"C\"";

    private final JdbcTemplate jdbc;

    public JdbcCardLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> cardNumbersOf(long accountId) {
        return jdbc.query(CARDS_OF_ACCOUNT, (rs, rowNumber) -> rs.getString("xref_card_num").stripTrailing(), accountId);
    }
}
