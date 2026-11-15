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
import com.carddemo.interest.codec.FixtureCodePage;
import com.carddemo.interest.codec.RecordImage;
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
    /** Balance record images in key order: the one place the key order of the file adapter is fixed (S3.7, E6). */
    private final List<String> balanceImages;
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
        this.balanceImages = readRecords(inputDir.resolve(BALANCES_FILE), CategoryBalanceRecord.LENGTH).stream()
                .sorted(Comparator.comparing(CategoryBalanceRecord::key))
                .toList();
        for (String image : readRecords(inputDir.resolve(ACCOUNTS_FILE), AccountRecord.LENGTH)) {
            AccountRecord.decode(image);
            accountImages.put(AccountRecord.key(image), image);
        }
        for (String image : readRecords(inputDir.resolve(CARDS_FILE), CardXrefRecord.LENGTH)) {
            CardOfAccount card = CardXrefRecord.decode(image);
            cardNumbersByAccount.computeIfAbsent(card.accountId(), id -> new ArrayList<>()).add(card.cardNumber());
        }
        cardNumbersByAccount.values().forEach(numbers -> numbers.sort(Comparator.naturalOrder()));
        for (String image : readRecords(inputDir.resolve(RATES_FILE), DisclosureGroupRecord.LENGTH)) {
            Rate rate = DisclosureGroupRecord.decode(image);
            ratesByKey.put(new Rate(rate.groupId(), rate.typeCode(), rate.categoryCode(), null), rate.yearlyRate());
        }
    }

    /** Opens the run: a missing or unreadable input file stops it before any work (CBACT04C:234-323). */
    public static FileRunWorkspace open(Path inputDir, Path outputDir, String runId) {
        return new FileRunWorkspace(inputDir, outputDir, runId);
    }

    private static List<String> readRecords(Path file, int recordLength) {
        try (Stream<String> lines = Files.lines(file, FixtureCodePage.CHARSET)) {
            return lines.map(line -> RecordImage.fromLine(line, recordLength)).toList();
        } catch (IOException | UncheckedIOException e) {
            throw new InterestRunStoppedException(StopReason.OPEN_INPUT,
                    "cannot read input file " + file.getFileName(), e);
        }
    }

    public BalanceSource balanceSource() {
        return () -> {
            if (nextBalance >= balanceImages.size()) {
                return Optional.empty();
            }
            List<BalanceRead> balances = new ArrayList<>();
            CategoryBalance first = CategoryBalanceRecord.decode(balanceImages.get(nextBalance));
            while (nextBalance < balanceImages.size()) {
                String image = balanceImages.get(nextBalance);
                CategoryBalance balance = CategoryBalanceRecord.decode(image);
                if (balance.accountId() != first.accountId()) {
                    break;
                }
                balances.add(new BalanceRead(balance, image));
                nextBalance++;
            }
            return Optional.of(new AccountBalances(first.accountId(), balances, nextBalance >= balanceImages.size()));
        };
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
        return transaction -> transactionImages.add(TransactionRecord.encode(transaction));
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
            writeLines(stagingDir.resolve(TRANSACTIONS_DATASET),
                    transactionImages.stream().map(RecordImage::toLine).toList());
            writeLines(stagingDir.resolve(ACCOUNTS_DATASET),
                    accountImages.values().stream().map(RecordImage::toLine).toList());
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
