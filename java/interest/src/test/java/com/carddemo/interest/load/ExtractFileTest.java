package com.carddemo.interest.load;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.carddemo.interest.codec.FieldSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The row mapping against data-map-interest.md: columns, keys, summed amounts, hash totals, record lengths. */
class ExtractFileTest {

    private static List<String> columns(ExtractFile file, List<FieldSpec> fields) {
        return fields.stream().map(file::column).toList();
    }

    @Test
    void dataMap_columnsOfEveryTableInRecordOrder() {
        assertEquals(List.of("acct_id", "acct_active_status", "acct_curr_bal", "acct_credit_limit", "acct_cash_credit_limit",
                "acct_open_date", "acct_expiraion_date", "acct_reissue_date", "acct_curr_cyc_credit", "acct_curr_cyc_debit",
                "acct_addr_zip", "acct_group_id", "acct_filler"), columns(ExtractFile.ACCOUNT, ExtractFile.ACCOUNT.layout().fields()));
        assertEquals(List.of("xref_card_num", "xref_cust_id", "xref_acct_id", "xref_filler"),
                columns(ExtractFile.CARD_XREF, ExtractFile.CARD_XREF.layout().fields()));
        assertEquals(List.of("trancat_acct_id", "trancat_type_cd", "trancat_cd", "tran_cat_bal", "trancat_filler"),
                columns(ExtractFile.TRAN_CAT_BALANCE, ExtractFile.TRAN_CAT_BALANCE.layout().fields()));
        assertEquals(List.of("dis_acct_group_id", "dis_tran_type_cd", "dis_tran_cat_cd", "dis_int_rate", "dis_filler"),
                columns(ExtractFile.DISCLOSURE_GROUP, ExtractFile.DISCLOSURE_GROUP.layout().fields()));
    }

    @Test
    void dataMap_primaryKeysAreTheLeadingFields() {
        assertEquals(List.of("acct_id"), columns(ExtractFile.ACCOUNT, ExtractFile.ACCOUNT.keyFields()));
        assertEquals(List.of("xref_card_num"), columns(ExtractFile.CARD_XREF, ExtractFile.CARD_XREF.keyFields()));
        assertEquals(List.of("trancat_acct_id", "trancat_type_cd", "trancat_cd"),
                columns(ExtractFile.TRAN_CAT_BALANCE, ExtractFile.TRAN_CAT_BALANCE.keyFields()));
        assertEquals(List.of("dis_acct_group_id", "dis_tran_type_cd", "dis_tran_cat_cd"),
                columns(ExtractFile.DISCLOSURE_GROUP, ExtractFile.DISCLOSURE_GROUP.keyFields()));
    }

    @Test
    void rc2_summedAmountsAndHashTotals() {
        assertEquals(List.of("acct_curr_bal", "acct_credit_limit", "acct_cash_credit_limit", "acct_curr_cyc_credit",
                "acct_curr_cyc_debit"), columns(ExtractFile.ACCOUNT, ExtractFile.ACCOUNT.summedFields()));
        assertEquals(List.of(), ExtractFile.CARD_XREF.summedFields());
        assertEquals(List.of("tran_cat_bal"), columns(ExtractFile.TRAN_CAT_BALANCE, ExtractFile.TRAN_CAT_BALANCE.summedFields()));
        assertEquals(List.of("dis_int_rate"), columns(ExtractFile.DISCLOSURE_GROUP, ExtractFile.DISCLOSURE_GROUP.summedFields()));
        assertEquals("acct_id", ExtractFile.ACCOUNT.column(ExtractFile.ACCOUNT.hashTotalField().orElseThrow()));
        assertEquals("xref_acct_id", ExtractFile.CARD_XREF.column(ExtractFile.CARD_XREF.hashTotalField().orElseThrow()));
        assertEquals("trancat_acct_id", ExtractFile.TRAN_CAT_BALANCE.column(ExtractFile.TRAN_CAT_BALANCE.hashTotalField().orElseThrow()));
        assertEquals(true, ExtractFile.DISCLOSURE_GROUP.hashTotalField().isEmpty());
    }

    @Test
    void tl2_recordLengths300And50() {
        assertEquals(300, ExtractFile.ACCOUNT.layout().length());
        assertEquals(50, ExtractFile.CARD_XREF.layout().length());
        assertEquals(50, ExtractFile.TRAN_CAT_BALANCE.layout().length());
        assertEquals(50, ExtractFile.DISCLOSURE_GROUP.layout().length());
    }

    @Test
    void tl2_fileNamesPerFormat() {
        assertEquals("AWS.M2.CARDDEMO.ACCTDATA.PS.txt", ExtractFile.ACCOUNT.fileName(ExtractFormat.FIXTURE));
        assertEquals("AWS.M2.CARDDEMO.ACCTDATA.PS", ExtractFile.ACCOUNT.fileName(ExtractFormat.HOST));
        assertEquals(ExtractFormat.HOST, ExtractFormat.ofParameter("host"));
    }
}
