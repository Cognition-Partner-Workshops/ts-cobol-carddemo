package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.port.CardLookup;

/**
 * FI-3 on carddemo.card_xref through card_xref_acct_ix, the replacement of the alternate key
 * (CBACT04C:34-39, 393-413; G12). Every card of the account, in card-number order.
 */
public final class JdbcCardLookup implements CardLookup {

    private static final String CARDS_OF_ACCOUNT = """
            SELECT xref_card_num
              FROM carddemo.card_xref
             WHERE xref_acct_id = ?
             ORDER BY xref_card_num""";

    private final JdbcTemplate jdbc;

    public JdbcCardLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> cardNumbersOf(long accountId) {
        return jdbc.query(CARDS_OF_ACCOUNT, (row, rowNumber) -> row.getString("xref_card_num").stripTrailing(),
                BigDecimal.valueOf(accountId));
    }
}
