package com.carddemo.interest.load;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The loader's part of interest-run-control: the interest_run row of a run starts here, with
 * status LOADED and the figures of RC1 and RC2, or with status FAILED when the load stopped.
 */
@Repository
class LoadRunJournal {

    private static final String INSERT_LOADED = """
            INSERT INTO carddemo.interest_run (parm_date, started_at, status,
                loaded_account_count, loaded_card_xref_count, loaded_tran_cat_balance_count, loaded_disclosure_group_count,
                loaded_acct_curr_bal_sum, loaded_acct_credit_limit_sum, loaded_acct_cash_credit_limit_sum,
                loaded_acct_curr_cyc_credit_sum, loaded_acct_curr_cyc_debit_sum,
                loaded_tran_cat_bal_sum, loaded_dis_int_rate_sum,
                loaded_acct_id_sum, loaded_xref_acct_id_sum, loaded_trancat_acct_id_sum)
            VALUES (?, ?, 'LOADED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING run_id""";

    private static final String INSERT_FAILED = """
            INSERT INTO carddemo.interest_run (parm_date, started_at, finished_at, status)
            VALUES (?, ?, ?, 'FAILED')
            RETURNING run_id""";

    private final JdbcTemplate jdbc;

    LoadRunJournal(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    long loaded(String parmDate, Instant startedAt, LoadFigures figures) {
        return jdbc.queryForObject(INSERT_LOADED, Long.class, parmDate, Timestamp.from(startedAt),
                figures.accountCount(), figures.cardXrefCount(), figures.tranCatBalanceCount(), figures.disclosureGroupCount(),
                figures.acctCurrBalSum(), figures.acctCreditLimitSum(), figures.acctCashCreditLimitSum(),
                figures.acctCurrCycCreditSum(), figures.acctCurrCycDebitSum(),
                figures.tranCatBalSum(), figures.disIntRateSum(),
                figures.acctIdSum(), figures.xrefAcctIdSum(), figures.trancatAcctIdSum());
    }

    long failed(String parmDate, Instant startedAt, Instant finishedAt) {
        return jdbc.queryForObject(INSERT_FAILED, Long.class, parmDate, Timestamp.from(startedAt), Timestamp.from(finishedAt));
    }
}
