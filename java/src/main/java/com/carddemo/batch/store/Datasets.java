package com.carddemo.batch.store;

import java.util.List;

/** Canonical dataset names (spec/harness/datasets.py) and the tables that hold them. */
public enum Datasets {
    ACCOUNTS("accounts", "account", "acct_id"),
    CARDXREF("cardxref", "card_xref", "xref_card_num"),
    TCATBAL("tcatbal", "category_balance", "trancat_acct_id, trancat_type_cd, trancat_cd"),
    DISCGRP("discgrp", "disclosure_group", "dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd"),
    DAILYTRAN("dailytran", "daily_transaction", "seq"),
    POSTED("posted", "posted_transaction", "tran_id"),
    REJECTS("rejects", "rejected_transaction", "seq"),
    INTEREST("interest", "interest_transaction", "tran_id");

    public static final List<Datasets> INPUTS = List.of(ACCOUNTS, CARDXREF, TCATBAL, DISCGRP, DAILYTRAN);
    public static final List<Datasets> OUTPUTS = List.of(POSTED, REJECTS, ACCOUNTS, TCATBAL, INTEREST);

    private final String fileName;
    private final String table;
    private final String orderBy;

    Datasets(String fileName, String table, String orderBy) {
        this.fileName = fileName;
        this.table = table;
        this.orderBy = orderBy;
    }

    public String fileName() {
        return fileName + ".jsonl";
    }

    public String table() {
        return table;
    }

    public String orderBy() {
        return orderBy;
    }
}
