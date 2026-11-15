package com.carddemo.interest.load;

import com.carddemo.interest.jdbc.ReplicaTable;

/** The unloaded file of each replica table (strategy item TL1), in load order. */
public enum ExtractFile {
    ACCOUNTS(ReplicaTable.ACCOUNT, "AWS.M2.CARDDEMO.ACCTDATA.PS"),
    CARDS(ReplicaTable.CARD_XREF, "AWS.M2.CARDDEMO.CARDXREF.PS"),
    BALANCES(ReplicaTable.TRAN_CAT_BALANCE, "AWS.M2.CARDDEMO.TCATBALF.PS"),
    RATES(ReplicaTable.DISCLOSURE_GROUP, "AWS.M2.CARDDEMO.DISCGRP.PS");

    private final ReplicaTable table;
    private final String dataset;

    ExtractFile(ReplicaTable table, String dataset) {
        this.table = table;
        this.dataset = dataset;
    }

    public ReplicaTable table() {
        return table;
    }

    public String fileName(ExtractFormat format) {
        return dataset + format.fileSuffix();
    }
}
