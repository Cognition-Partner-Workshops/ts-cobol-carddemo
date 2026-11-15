package com.carddemo.interest.recon;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordOrigin;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.InterestRunStoppedException;

/**
 * The file side of RC5: the SYSTRAN generation and the ACCTDATA image a run exported (FI-2), read
 * from its output directory. A file that is missing or does not decode gives no figure.
 */
public final class ExportedFiles {

    static final String TRANSACTIONS = "AWS.M2.CARDDEMO.SYSTRAN.G0001V00";
    static final String ACCOUNTS = "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS";

    private final Path directory;
    private final Figure transactionCount;
    private final Figure transactionAmountSum;
    private final Figure accountBalanceSum;

    private ExportedFiles(Path directory, Figure transactionCount, Figure transactionAmountSum, Figure accountBalanceSum) {
        this.directory = directory;
        this.transactionCount = transactionCount;
        this.transactionAmountSum = transactionAmountSum;
        this.accountBalanceSum = accountBalanceSum;
    }

    public static ExportedFiles read(Path directory, RecordFormat format) {
        Figure[] transactions = countAndSum(directory, format, TRANSACTIONS, TransactionRecord.LAYOUT, "TRAN-AMT");
        Figure[] accounts = countAndSum(directory, format, ACCOUNTS, AccountRecord.LAYOUT, "ACCT-CURR-BAL");
        return new ExportedFiles(directory, transactions[0], transactions[1], accounts[1]);
    }

    private static Figure[] countAndSum(Path directory, RecordFormat format, String dataset, RecordLayout layout, String field) {
        String fileName = dataset + ".txt";
        Path path = directory.resolve("datasets").resolve(fileName);
        if (!Files.isRegularFile(path)) {
            Figure missing = Figure.notAvailable("no exported file datasets/" + fileName);
            return new Figure[] {missing, missing};
        }
        try {
            List<byte[]> records = format.records(Files.readAllBytes(path), layout, fileName);
            RecordCodec codec = format.codec(layout);
            BigDecimal sum = BigDecimal.ZERO.setScale(layout.field(field).scale());
            for (int number = 1; number <= records.size(); number++) {
                sum = sum.add(codec.decode(records.get(number - 1), new RecordOrigin(fileName, number)).signed(field));
            }
            return new Figure[] {Figure.of(records.size()), Figure.of(sum)};
        } catch (IOException | InterestRunStoppedException e) {
            Figure unreadable = Figure.notAvailable("datasets/" + fileName + " cannot be decoded");
            return new Figure[] {unreadable, unreadable};
        }
    }

    public Path directory() {
        return directory;
    }

    public Figure transactionCount() {
        return transactionCount;
    }

    public Figure transactionAmountSum() {
        return transactionAmountSum;
    }

    public Figure accountBalanceSum() {
        return accountBalanceSum;
    }
}
