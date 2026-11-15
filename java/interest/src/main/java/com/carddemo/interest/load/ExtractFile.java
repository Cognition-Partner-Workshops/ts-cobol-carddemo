package com.carddemo.interest.load;

import java.util.List;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.DisclosureGroupRecord;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordLayout.FieldLayout;
import com.carddemo.interest.codec.RecordLayout.Kind;

/**
 * The four unloaded files and the replica table each one replaces (architecture.md section 3),
 * in the order they are loaded. A column has the name of its copybook field in lower case with
 * underscores (db/postgres/02-interest.sql, data-map-interest.md); only the FILLER column is named here.
 */
public enum ExtractFile {
    ACCOUNTS("AWS.M2.CARDDEMO.ACCTDATA.PS.txt", AccountRecord.LAYOUT, "account", "acct_filler",
            List.of("ACCT-ID"), "ACCT-ID"),
    CARD_XREFS("AWS.M2.CARDDEMO.CARDXREF.PS.txt", CardXrefRecord.LAYOUT, "card_xref", "xref_filler",
            List.of("XREF-CARD-NUM"), "XREF-ACCT-ID"),
    CATEGORY_BALANCES("AWS.M2.CARDDEMO.TCATBALF.PS.txt", CategoryBalanceRecord.LAYOUT, "tran_cat_balance",
            "trancat_filler", List.of("TRANCAT-ACCT-ID", "TRANCAT-TYPE-CD", "TRANCAT-CD"), "TRANCAT-ACCT-ID"),
    DISCLOSURE_GROUPS("AWS.M2.CARDDEMO.DISCGRP.PS.txt", DisclosureGroupRecord.LAYOUT, "disclosure_group",
            "dis_filler", List.of("DIS-ACCT-GROUP-ID", "DIS-TRAN-TYPE-CD", "DIS-TRAN-CAT-CD"), null);

    private final String fileName;
    private final RecordLayout layout;
    private final String table;
    private final String fillerColumn;
    private final List<String> keyFields;
    private final String hashTotalField;

    ExtractFile(String fileName, RecordLayout layout, String table, String fillerColumn, List<String> keyFields,
            String hashTotalField) {
        this.fileName = fileName;
        this.layout = layout;
        this.table = table;
        this.fillerColumn = fillerColumn;
        this.keyFields = keyFields;
        this.hashTotalField = hashTotalField;
    }

    public String fileName() {
        return fileName;
    }

    public RecordLayout layout() {
        return layout;
    }

    /** The replica table, qualified: the connection's search_path is the job repository's schema. */
    public String table() {
        return "carddemo." + table;
    }

    public List<String> keyFields() {
        return keyFields;
    }

    public List<String> keyColumns() {
        return keyFields.stream().map(name -> column(layout.field(name))).toList();
    }

    /** RC2: the account id whose sum is the hash total of the file; the rates have none. */
    public String hashTotalField() {
        return hashTotalField;
    }

    public String column(FieldLayout field) {
        return field.kind() == Kind.FILLER ? fillerColumn : field.name().toLowerCase().replace('-', '_');
    }

    public List<String> columns() {
        return layout.fields().stream().map(this::column).toList();
    }
}
