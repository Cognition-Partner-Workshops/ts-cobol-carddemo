package com.carddemo.interest.load;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.DisclosureGroupRecord;
import com.carddemo.interest.codec.FieldKind;
import com.carddemo.interest.codec.FieldSpec;
import com.carddemo.interest.codec.RecordLayout;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The four unloaded files and the replica table each one replaces (architecture.md section 3,
 * data-map-interest.md). The row mapping is the data map's: a column is named as its copybook
 * field in lower case with underscores, the FILLER goes to the table's BYTEA column, the key is
 * the leading fields of the record (every file is a KSDS unload with its key at offset 0).
 * Load order: accounts, cards, balances, rates.
 */
public enum ExtractFile {

    /** CVACT01Y -> carddemo.account. Hash total: ACCT-ID. */
    ACCOUNT("AWS.M2.CARDDEMO.ACCTDATA.PS", AccountRecord.LAYOUT, "account", "acct_filler", 1, "ACCT-ID"),
    /** CVACT03Y -> carddemo.card_xref. Hash total: XREF-ACCT-ID. */
    CARD_XREF("AWS.M2.CARDDEMO.CARDXREF.PS", CardXrefRecord.LAYOUT, "card_xref", "xref_filler", 1, "XREF-ACCT-ID"),
    /** CVTRA01Y -> carddemo.tran_cat_balance. Hash total: TRANCAT-ACCT-ID. */
    TRAN_CAT_BALANCE("AWS.M2.CARDDEMO.TCATBALF.PS", CategoryBalanceRecord.LAYOUT, "tran_cat_balance", "trancat_filler", 3,
            "TRANCAT-ACCT-ID"),
    /** CVTRA02Y -> carddemo.disclosure_group. No account id, so no hash total (RC2). */
    DISCLOSURE_GROUP("AWS.M2.CARDDEMO.DISCGRP.PS", DisclosureGroupRecord.LAYOUT, "disclosure_group", "dis_filler", 3, null);

    public static final String SCHEMA = "carddemo";

    private final String datasetName;
    private final RecordLayout layout;
    private final String table;
    private final String fillerColumn;
    private final int keyFieldCount;
    private final String hashTotalField;

    ExtractFile(String datasetName, RecordLayout layout, String table, String fillerColumn, int keyFieldCount,
            String hashTotalField) {
        this.datasetName = datasetName;
        this.layout = layout;
        this.table = table;
        this.fillerColumn = fillerColumn;
        this.keyFieldCount = keyFieldCount;
        this.hashTotalField = hashTotalField;
    }

    public String fileName(ExtractFormat format) {
        return format.fileName(datasetName);
    }

    public RecordLayout layout() {
        return layout;
    }

    public String table() {
        return table;
    }

    public String qualifiedTable() {
        return SCHEMA + "." + table;
    }

    public String column(FieldSpec field) {
        return field.kind() == FieldKind.FILLER ? fillerColumn : field.name().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public List<FieldSpec> keyFields() {
        return layout.fields().subList(0, keyFieldCount);
    }

    /** RC2: the amounts summed on both sides - every signed field of the layout. */
    public List<FieldSpec> summedFields() {
        return layout.fields().stream().filter(field -> field.kind() == FieldKind.SIGNED_ZONED).toList();
    }

    /** RC2: the account id of the file, summed as a hash total. */
    public Optional<FieldSpec> hashTotalField() {
        return layout.fields().stream().filter(field -> field.name().equals(hashTotalField)).findFirst();
    }
}
