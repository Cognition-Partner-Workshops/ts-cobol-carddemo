package com.carddemo.modern.trantype;

/** The three EXEC SQL statements of COBTUPDT on CARDDEMO.TRANSACTION_TYPE; each returns SQLCODE. */
public interface TransactionTypeSql {

    /** 10031-INSERT-DB: {@code INSERT INTO CARDDEMO.TRANSACTION_TYPE (TR_TYPE, TR_DESCRIPTION) VALUES (...)}. */
    int insert(String trType, String description);

    /** 10032-UPDATE-DB: {@code UPDATE ... SET TR_DESCRIPTION = :INPUT-REC-DESC WHERE TR_TYPE = :INPUT-REC-NUMBER}. */
    int update(String trType, String description);

    /** 10033-DELETE-DB: {@code DELETE FROM CARDDEMO.TRANSACTION_TYPE WHERE TR_TYPE = :INPUT-REC-NUMBER}. */
    int delete(String trType);
}
