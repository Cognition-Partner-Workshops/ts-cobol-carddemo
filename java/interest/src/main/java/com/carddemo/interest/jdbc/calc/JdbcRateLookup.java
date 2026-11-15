package com.carddemo.interest.jdbc.calc;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

import com.carddemo.interest.port.RateLookup;

/**
 * Yearly rate in percent by disclosure group, transaction type and category from
 * carddemo.disclosure_group (INT-R02, CBACT04C:415-420; data-map rows CVTRA02Y/DIS-GROUP-KEY and
 * DIS-INT-RATE). A missing row is an empty result, never an error (INT-R03).
 */
public final class JdbcRateLookup implements RateLookup {

    private static final String RATE = "SELECT dis_int_rate FROM carddemo.disclosure_group"
            + " WHERE dis_acct_group_id = CAST(? AS CHAR(10)) AND dis_tran_type_cd = CAST(? AS CHAR(2)) AND dis_tran_cat_cd = ?";

    private final JdbcTemplate jdbc;

    public JdbcRateLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<BigDecimal> findRate(String groupId, String typeCode, int categoryCode) {
        return jdbc.query(RATE, (rs, rowNumber) -> rs.getBigDecimal("dis_int_rate"), groupId, typeCode, categoryCode)
                .stream().findFirst();
    }
}
