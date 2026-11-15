package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.port.RateLookup;

/**
 * FI-3 on carddemo.disclosure_group: the read by full key (INT-R02, CBACT04C:415-420). The group
 * id and type code are CHAR columns, so a shorter value such as DEFAULT compares equal to its
 * padded form, as in COBOL.
 */
public final class JdbcRateLookup implements RateLookup {

    private static final String RATE = """
            SELECT dis_int_rate
              FROM carddemo.disclosure_group
             WHERE dis_acct_group_id = CAST(? AS CHAR(10))
               AND dis_tran_type_cd = CAST(? AS CHAR(2))
               AND dis_tran_cat_cd = ?""";

    private final JdbcTemplate jdbc;

    public JdbcRateLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<BigDecimal> findRate(String groupId, String typeCode, int categoryCode) {
        List<BigDecimal> found = jdbc.query(RATE, (row, rowNumber) -> row.getBigDecimal("dis_int_rate"),
                groupId, typeCode, BigDecimal.valueOf(categoryCode));
        return found.stream().findFirst();
    }
}
