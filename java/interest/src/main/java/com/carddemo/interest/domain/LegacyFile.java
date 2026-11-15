package com.carddemo.interest.domain;

/**
 * The five files CBACT04C opens (CBACT04C:234-323, INT-T01) with the line it displays when the
 * open fails. A stop for OPEN_INPUT carries one of them as data; in the target it stands for the
 * table or location that replaces the file.
 */
public enum LegacyFile {
    BALANCES("ERROR OPENING TRANSACTION CATEGORY BALANCE"),
    CARDS("ERROR OPENING CROSS REF FILE"),
    /** INT-T10: the text for the disclosure-group file is a copied one, kept as coded (CBACT04C:281). */
    RATES("ERROR OPENING DALY REJECTS FILE"),
    ACCOUNTS("ERROR OPENING ACCOUNT MASTER FILE"),
    TRANSACTIONS("ERROR OPENING TRANSACTION FILE");

    private final String openFailedMessage;

    LegacyFile(String openFailedMessage) {
        this.openFailedMessage = openFailedMessage;
    }

    String openFailedMessage() {
        return openFailedMessage;
    }
}
