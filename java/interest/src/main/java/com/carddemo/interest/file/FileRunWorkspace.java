package com.carddemo.interest.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.carddemo.interest.codec.AccountRecord;
import com.carddemo.interest.codec.CardXrefRecord;
import com.carddemo.interest.codec.CardXrefRecord.CardOfAccount;
import com.carddemo.interest.codec.CategoryBalanceRecord;
import com.carddemo.interest.codec.DisclosureGroupRecord;
import com.carddemo.interest.codec.DisclosureGroupRecord.Rate;
import com.carddemo.interest.codec.CodePage;
import com.carddemo.interest.codec.RecordCodec;
import com.carddemo.interest.codec.RecordImage;
import com.carddemo.interest.codec.RecordSource;
import com.carddemo.interest.codec.TransactionRecord;
import com.carddemo.interest.domain.Account;
import com.carddemo.interest.domain.AccountBalances;
import com.carddemo.interest.domain.AccountBalances.BalanceRead;
import com.carddemo.interest.domain.CategoryBalance;
import com.carddemo.interest.domain.InterestRunStoppedException;
import com.carddemo.interest.domain.InterestTransaction;
import com.carddemo.interest.domain.StopReason;
import com.carddemo.interest.port.AccountStore;
import com.carddemo.interest.port.BalanceSource;
import com.carddemo.interest.port.CardLookup;
import com.carddemo.interest.port.RateLookup;
import com.carddemo.interest.port.RunLog;
import com.carddemo.interest.port.TransactionSink;

/**
 * File adapters of the six ports (FI-3) for one run, in the recorded fixture format (FI-2).
 * The four input files of a case are read when the run opens; the outputs are staged next to the
 * output directory and moved into place only by {@link #commit}, so a failed run leaves no output
 * (INTCALC.jcl:37 DISP=(NEW,CATLG,DELETE); standards S6.4). No business table is used (ticket 01).
 */
public final class FileRunWorkspace {

    static final String BALANCES_FILE = "AWS.M2.CARDDEMO.TCATBALF.PS.txt";
    static final String ACCOUNTS_FILE = "AWS.M2.CARDDEMO.ACCTDATA.PS.txt";
    static final String CARDS_FILE = "AWS.M2.CARDDEMO.CARDXREF.PS.txt";
    static final String RATES_FILE = "AWS.M2.CARDDEMO.DISCGRP.PS.txt";
    static final String TRANSACTIONS_DATASET = "datasets/AWS.M2.CARDDEMO.SYSTRAN.G0001V00.txt";
    static final String ACCOUNTS_DATASET = "datasets/AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS.txt";
    static final String RUN_LOG_FILE = "sysout/INTCALC.STEP15.txt";
    static final String RETURN_CODES_FILE = "rc.json";

    private final Path outputDir;
    private final Path stagingDir;
    /** The one place the file adapters choose the code page (S3.7); the host format is not wired to the job yet. */
    private final CodePage codePage = CodePage.fixture();
    private final CategoryBalanceRecord balanceRecord = new CategoryBalanceRecord(codePage);
    private final AccountRecord accountRecord = new AccountRecord(codePage);
    private final CardXrefRecord cardRecord = new CardXrefRecord(codePage);
    private final DisclosureGroupRecord rateRecord = new DisclosureGroupRecord(codePage);
    private final TransactionRecord transactionRecord = new TransactionRecord(codePage);

    /** A record as read and where it was read, for the rejection of a later decode. */
    private record Read(byte[] record, RecordSource source) {
    }

    /** Balance records in key order: the one place the key order of the file adapter is fixed (S3.7, E6). */
    private final List<Read> balanceRecords;
    private int nextBalance;
    /** Account records by ACCT-ID, in key order (the KSDS image). */
    private final TreeMap<Long, Read> accountRecords = new TreeMap<>();
    private final Map<Long, List<String>> cardNumbersByAccount = new HashMap<>();
    private final Map<Rate, BigDecimal> ratesByKey = new HashMap<>();
    private final List<byte[]> transactionRecords = new ArrayList<>();
    private final List<String> runLogLines = new ArrayList<>();

    private FileRunWorkspace(Path inputDir, Path outputDir, String runId) {
        this.outputDir = outputDir;
        this.stagingDir = outputDir.resolveSibling(outputDir.getFileName() + ".staging-" + runId);
        this.balanceRecords = readRecords(inputDir.resolve(BALANCES_FILE), balanceRecord.codec()).stream()
                .sorted(Comparator.comparing(Read::record, CategoryBalanceRecord.KEY_ORDER))
                .toList();
        for (Read read : readRecords(inputDir.resolve(ACCOUNTS_FILE), accountRecord.codec())) {
            accountRecords.put(accountRecord.decode(read.record(), read.source()).accountId(), read);
        }
        for (Read read : readRecords(inputDir.resolve(CARDS_FILE), cardRecord.codec())) {
            CardOfAccount card = cardRecord.decode(read.record(), read.source());
            cardNumbersByAccount.computeIfAbsent(card.accountId(), id -> new ArrayList<>()).add(card.cardNumber());
        }
        cardNumbersByAccount.values().forEach(numbers -> numbers.sort(Comparator.naturalOrder()));
        for (Read read : readRecords(inputDir.resolve(RATES_FILE), rateRecord.codec())) {
            Rate rate = rateRecord.decode(read.record(), read.source());
            ratesByKey.put(new Rate(rate.groupId(), rate.typeCode(), rate.categoryCode(), null), rate.yearlyRate());
        }
    }

    /** Opens the run: a missing or unreadable input file stops it before any work (CBACT04C:234-323). */
    public static FileRunWorkspace open(Path inputDir, Path outputDir, String runId) {
        return new FileRunWorkspace(inputDir, outputDir, runId);
    }

    private List<Read> readRecords(Path file, RecordCodec codec) {
        String fileName = file.getFileName().toString();
        List<String> lines;
        try (Stream<String> stream = Files.lines(file, codePage.charset())) {
            lines = stream.toList();
        } catch (IOException | UncheckedIOException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read input file " + fileName, e);
        }
        List<Read> records = new ArrayList<>();
        for (String line : lines) {
            RecordSource source = new RecordSource(fileName, records.size() + 1);
            records.add(new Read(RecordImage.fromLine(line, codec.recordLength(source), codePage), source));
        }
        return records;
    }

    public BalanceSource balanceSource() {
        return () -> {
            if (nextBalance >= balanceRecords.size()) {
                return Optional.empty();
            }
            List<BalanceRead> balances = new ArrayList<>();
            CategoryBalance first = decodeBalance(balanceRecords.get(nextBalance));
            while (nextBalance < balanceRecords.size()) {
                Read read = balanceRecords.get(nextBalance);
                CategoryBalance balance = decodeBalance(read);
                if (balance.accountId() != first.accountId()) {
                    break;
                }
                balances.add(new BalanceRead(balance, codePage.display(read.record())));
                nextBalance++;
            }
            return Optional.of(new AccountBalances(first.accountId(), balances, nextBalance >= balanceRecords.size()));
        };
    }

    private CategoryBalance decodeBalance(Read read) {
        return balanceRecord.decode(read.record(), read.source());
    }

    public AccountStore accountStore() {
        return new AccountStore() {
            @Override
            public Optional<Account> find(long accountId) {
                return Optional.ofNullable(accountRecords.get(accountId))
                        .map(read -> accountRecord.decode(read.record(), read.source()));
            }

            @Override
            public void postInterest(long accountId, BigDecimal newBalance) {
                accountRecords.computeIfPresent(accountId, (id, read) -> new Read(
                        accountRecord.withInterestPosted(read.record(), newBalance, read.source()), read.source()));
            }
        };
    }

    public CardLookup cardLookup() {
        return accountId -> List.copyOf(cardNumbersByAccount.getOrDefault(accountId, List.of()));
    }

    public RateLookup rateLookup() {
        return (groupId, typeCode, categoryCode) ->
                Optional.ofNullable(ratesByKey.get(new Rate(groupId, typeCode, categoryCode, null)));
    }

    public TransactionSink transactionSink() {
        return transaction -> transactionRecords.add(transactionRecord.encode(transaction,
                new RecordSource(Path.of(TRANSACTIONS_DATASET).getFileName().toString(), transactionRecords.size() + 1)));
    }

    public RunLog runLog() {
        return new RunLog() {
            @Override
            public void line(String text) {
                runLogLines.add(text);
            }

            @Override
            public void echo(String recordImage) {
                runLogLines.add(recordImage);
            }
        };
    }

    public int transactionsWritten() {
        return transactionRecords.size();
    }

    /** Normal end: writes the outputs to the staging directory and moves it into place. */
    public void commit(int stepReturnCode) {
        try {
            deleteTree(stagingDir);
            Files.createDirectories(stagingDir.resolve("datasets"));
            Files.createDirectories(stagingDir.resolve("sysout"));
            writeLines(stagingDir.resolve(TRANSACTIONS_DATASET),
                    transactionRecords.stream().map(this::toLine).toList());
            writeLines(stagingDir.resolve(ACCOUNTS_DATASET),
                    accountRecords.values().stream().map(read -> toLine(read.record())).toList());
            writeLines(stagingDir.resolve(RUN_LOG_FILE), runLogLines);
            Files.writeString(stagingDir.resolve(RETURN_CODES_FILE),
                    RecordedReturnCodes.json(stepReturnCode), codePage.charset());
            deleteTree(outputDir);
            Files.createDirectories(outputDir.toAbsolutePath().getParent());
            Files.move(stagingDir, outputDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            discard();
            throw new InterestRunStoppedException(StopReason.WRITE_OUTPUT, "cannot write the outputs", e);
        }
    }

    /** Abnormal end: nothing of this run stays on disk. */
    public void discard() {
        try {
            deleteTree(stagingDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String toLine(byte[] record) {
        return RecordImage.toLine(record, codePage);
    }

    private void writeLines(Path file, List<String> lines) throws IOException {
        StringBuilder text = new StringBuilder();
        lines.forEach(line -> text.append(line).append('\n'));
        Files.writeString(file, text, codePage.charset());
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
