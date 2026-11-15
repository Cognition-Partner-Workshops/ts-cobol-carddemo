package com.carddemo.interest.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
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
import com.carddemo.interest.codec.FixtureCodePage;
import com.carddemo.interest.codec.RecordFormat;
import com.carddemo.interest.codec.RecordLayout;
import com.carddemo.interest.codec.RecordOrigin;
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
 * Every record is split, decoded and encoded by the record codec (ticket 03); an input record that
 * breaks a rule of the codec stops the run with the file, the record number and the field named.
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

    /** The format of the files of a case; the host format is not wired to the job (ticket 03 report). */
    private static final RecordFormat FORMAT = RecordFormat.fixture();

    /** A record image and where it was read. */
    private record Sourced(String image, RecordOrigin origin) {
    }

    private final Path outputDir;
    private final Path stagingDir;
    /** Balance record images in key order: the one place the key order of the file adapter is fixed (S3.7, E6). */
    private final List<Sourced> balanceImages;
    private int nextBalance;
    /** Account record images by ACCT-ID text, in key order (the KSDS image). */
    private final TreeMap<String, String> accountImages = new TreeMap<>();
    private final Map<Long, List<String>> cardNumbersByAccount = new HashMap<>();
    private final Map<Rate, BigDecimal> ratesByKey = new HashMap<>();
    private final List<String> transactionImages = new ArrayList<>();
    private final List<String> runLogLines = new ArrayList<>();

    private FileRunWorkspace(Path inputDir, Path outputDir, String runId) {
        this.outputDir = outputDir;
        this.stagingDir = outputDir.resolveSibling(outputDir.getFileName() + ".staging-" + runId);
        this.balanceImages = readRecords(inputDir.resolve(BALANCES_FILE), CategoryBalanceRecord.LAYOUT).stream()
                .sorted(Comparator.comparing(record -> CategoryBalanceRecord.key(record.image())))
                .toList();
        for (Sourced record : readRecords(inputDir.resolve(ACCOUNTS_FILE), AccountRecord.LAYOUT)) {
            AccountRecord.decode(record.image(), record.origin());
            accountImages.put(AccountRecord.key(record.image()), record.image());
        }
        for (Sourced record : readRecords(inputDir.resolve(CARDS_FILE), CardXrefRecord.LAYOUT)) {
            CardOfAccount card = CardXrefRecord.decode(record.image(), record.origin());
            cardNumbersByAccount.computeIfAbsent(card.accountId(), id -> new ArrayList<>()).add(card.cardNumber());
        }
        cardNumbersByAccount.values().forEach(numbers -> numbers.sort(Comparator.naturalOrder()));
        for (Sourced record : readRecords(inputDir.resolve(RATES_FILE), DisclosureGroupRecord.LAYOUT)) {
            Rate rate = DisclosureGroupRecord.decode(record.image(), record.origin());
            ratesByKey.put(new Rate(rate.groupId(), rate.typeCode(), rate.categoryCode(), null), rate.yearlyRate());
        }
    }

    /** Opens the run: a missing or unreadable input file stops it before any work (CBACT04C:234-323). */
    public static FileRunWorkspace open(Path inputDir, Path outputDir, String runId) {
        return new FileRunWorkspace(inputDir, outputDir, runId);
    }

    private static List<Sourced> readRecords(Path file, RecordLayout layout) {
        String fileName = file.getFileName().toString();
        byte[] content;
        try {
            content = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT, "cannot read input file " + fileName, e);
        }
        List<Sourced> records = new ArrayList<>();
        for (byte[] record : FORMAT.records(content, layout, fileName)) {
            records.add(new Sourced(FixtureCodePage.image(record), new RecordOrigin(fileName, records.size() + 1)));
        }
        return records;
    }

    public BalanceSource balanceSource() {
        return () -> {
            if (nextBalance >= balanceImages.size()) {
                return Optional.empty();
            }
            List<BalanceRead> balances = new ArrayList<>();
            CategoryBalance first = decode(balanceImages.get(nextBalance));
            while (nextBalance < balanceImages.size()) {
                Sourced record = balanceImages.get(nextBalance);
                CategoryBalance balance = decode(record);
                if (balance.accountId() != first.accountId()) {
                    break;
                }
                balances.add(new BalanceRead(balance, record.image()));
                nextBalance++;
            }
            return Optional.of(new AccountBalances(first.accountId(), balances, nextBalance >= balanceImages.size()));
        };
    }

    private static CategoryBalance decode(Sourced record) {
        return CategoryBalanceRecord.decode(record.image(), record.origin());
    }

    public AccountStore accountStore() {
        return new AccountStore() {
            @Override
            public Optional<Account> find(long accountId) {
                return Optional.ofNullable(accountImages.get(accountKey(accountId))).map(AccountRecord::decode);
            }

            @Override
            public void postInterest(long accountId, BigDecimal newBalance) {
                accountImages.computeIfPresent(accountKey(accountId),
                        (key, image) -> AccountRecord.withInterestPosted(image, newBalance));
            }
        };
    }

    private static String accountKey(long accountId) {
        return String.format("%011d", accountId);
    }

    public CardLookup cardLookup() {
        return accountId -> List.copyOf(cardNumbersByAccount.getOrDefault(accountId, List.of()));
    }

    public RateLookup rateLookup() {
        return (groupId, typeCode, categoryCode) ->
                Optional.ofNullable(ratesByKey.get(new Rate(groupId, typeCode, categoryCode, null)));
    }

    public TransactionSink transactionSink() {
        return transaction -> transactionImages.add(TransactionRecord.encode(transaction,
                new RecordOrigin(Path.of(TRANSACTIONS_DATASET).getFileName().toString(), transactionImages.size() + 1)));
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
        return transactionImages.size();
    }

    /** Normal end: writes the outputs to the staging directory and moves it into place. */
    public void commit(int stepReturnCode) {
        try {
            deleteTree(stagingDir);
            Files.createDirectories(stagingDir.resolve("datasets"));
            Files.createDirectories(stagingDir.resolve("sysout"));
            writeRecords(stagingDir.resolve(TRANSACTIONS_DATASET), transactionImages);
            writeRecords(stagingDir.resolve(ACCOUNTS_DATASET), accountImages.values());
            writeLines(stagingDir.resolve(RUN_LOG_FILE), runLogLines);
            Files.writeString(stagingDir.resolve(RETURN_CODES_FILE),
                    RecordedReturnCodes.json(stepReturnCode), FixtureCodePage.CHARSET);
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

    private static void writeRecords(Path file, Collection<String> images) throws IOException {
        Files.write(file, FORMAT.content(images.stream().map(FixtureCodePage::bytes).toList()));
    }

    private static void writeLines(Path file, List<String> lines) throws IOException {
        StringBuilder text = new StringBuilder();
        lines.forEach(line -> text.append(line).append('\n'));
        Files.writeString(file, text, FixtureCodePage.CHARSET);
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
