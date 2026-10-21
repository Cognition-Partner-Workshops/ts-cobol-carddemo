package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.carddemo.batch.domain.PurchasePromotion;
import com.carddemo.batch.jobs.IntCalcTasklet;
import com.carddemo.batch.store.BatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(properties = {
        "spring.batch.job.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:promotion-cycles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class PromotionCycleTest {

    private static final long ACCOUNT_ID = 51;
    private static final String PURCHASE_INTEREST_ID = "2022071800000001";
    private static final String OTHER_PURCHASE_INTEREST_ID = "2022071800000002";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BatchRepository repo;

    @Autowired
    private RunProperties runProperties;

    @BeforeEach
    void clearPromotionCycles() {
        jdbc.update("DELETE FROM promotion_cycle");
    }

    @Test
    void lastAccountGetsPromotionForItsFirstThreeRuns() {
        List<String> purchaseAmounts = new ArrayList<>();
        for (int cycle = 1; cycle <= 5; cycle++) {
            InterestAmounts amounts = runIntCalc(PurchasePromotion.GROUP);
            purchaseAmounts.add(amounts.purchase().toPlainString());
            assertThat(amounts.otherPurchase()).isEqualByComparingTo("31.07");
        }

        assertThat(purchaseAmounts).containsExactly("6.20", "6.20", "6.20", "18.64", "18.64");
        assertThat(repo.promotionCyclesUsed(ACCOUNT_ID)).isEqualTo(5);
    }

    @Test
    void leavingAndReenteringUsesTheExistingPromotionCycle() {
        List<String> groups = List.of(PurchasePromotion.GROUP, "", PurchasePromotion.GROUP,
                PurchasePromotion.GROUP, PurchasePromotion.GROUP);
        List<String> purchaseAmounts = new ArrayList<>();
        for (String group : groups) {
            purchaseAmounts.add(runIntCalc(group).purchase().toPlainString());
        }

        assertThat(purchaseAmounts).containsExactly("6.20", "18.64", "6.20", "6.20", "18.64");
        assertThat(repo.promotionCyclesUsed(ACCOUNT_ID)).isEqualTo(4);
    }

    @Test
    void runsOutsideTheGroupDoNotAdvanceOrResetTheCount() {
        runIntCalc(PurchasePromotion.GROUP);
        runIntCalc(PurchasePromotion.GROUP);
        runIntCalc(PurchasePromotion.GROUP);
        runIntCalc("");
        InterestAmounts finalRun = runIntCalc(PurchasePromotion.GROUP);

        assertThat(finalRun.purchase()).isEqualByComparingTo("18.64");
        assertThat(repo.promotionCyclesUsed(ACCOUNT_ID)).isEqualTo(4);
    }

    private InterestAmounts runIntCalc(String group) {
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(jdbc.getDataSource());
        jdbc.update("""
                INSERT INTO account (
                    acct_id, acct_active_status, acct_curr_bal, acct_credit_limit, acct_cash_credit_limit,
                    acct_open_date, acct_expiraion_date, acct_reissue_date, acct_curr_cyc_credit,
                    acct_curr_cyc_debit, acct_addr_zip, acct_group_id
                ) VALUES (?, 'Y', 1000.00, 5000.00, 2500.00, '2019-04-01', '2027-03-31',
                          '2027-03-31', 0.00, 0.00, '10001', ?)
                """, ACCOUNT_ID, group);
        jdbc.update("INSERT INTO card_xref (xref_card_num, xref_cust_id, xref_acct_id) VALUES (?, ?, ?)",
                "4000002610000051", 951, ACCOUNT_ID);
        jdbc.update("INSERT INTO category_balance VALUES (?, '01', 1, 1491.82)", ACCOUNT_ID);
        jdbc.update("INSERT INTO category_balance VALUES (?, '01', 2, 1491.82)", ACCOUNT_ID);
        jdbc.update("INSERT INTO disclosure_group VALUES ('DEFAULT', '01', 1, 15.00)");
        jdbc.update("INSERT INTO disclosure_group VALUES ('DEFAULT', '01', 2, 25.00)");

        new IntCalcTasklet(repo, runProperties).execute(null, null);
        BigDecimal purchase = jdbc.queryForObject("SELECT tran_amt FROM interest_transaction WHERE tran_id = ?",
                BigDecimal.class, PURCHASE_INTEREST_ID);
        BigDecimal otherPurchase = jdbc.queryForObject(
                "SELECT tran_amt FROM interest_transaction WHERE tran_id = ?", BigDecimal.class,
                OTHER_PURCHASE_INTEREST_ID);
        return new InterestAmounts(purchase, otherPurchase);
    }

    private record InterestAmounts(BigDecimal purchase, BigDecimal otherPurchase) {
    }
}
