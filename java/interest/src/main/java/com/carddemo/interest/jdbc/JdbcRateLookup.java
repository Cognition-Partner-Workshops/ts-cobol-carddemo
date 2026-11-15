package com.carddemo.interest.jdbc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.RateLookup;

/**
 * FI-3 on carddemo.disclosure_group: the yearly rate by group, transaction type and category
 * (INT-R02, CBACT04C:415-420). No row is an empty result, never an error (INT-R03).
 */
public final class JdbcRateLookup implements RateLookup {

    public static final String TABLE = "carddemo.disclosure_group";

    /** Data-map row CVTRA02Y/DIS-INT-RATE by the three key rows; the key texts are compared as CHAR(n). */
    private static final String RATE = """
            SELECT dis_int_rate
              FROM carddemo.disclosure_group
             WHERE dis_acct_group_id = CAST(? AS CHAR(10))
               AND dis_tran_type_cd = CAST(? AS CHAR(2))
               AND dis_tran_cat_cd = ?""";

    private final JdbcOperations jdbc;

    public JdbcRateLookup(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<BigDecimal> findRate(String groupId, String typeCode, int categoryCode) {
        try {
            List<BigDecimal> rates = jdbc.query(RATE, (row, rowNumber) -> row.getBigDecimal(1),
                    groupId, typeCode, categoryCode);
            return rates.stream().findFirst();
        } catch (DataAccessException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read table " + TABLE, e);
        }
    }
}
