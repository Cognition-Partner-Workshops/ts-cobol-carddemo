package com.carddemo.posting.batch;

import com.carddemo.posting.ChainRun;
import com.carddemo.posting.LegacyAbend;
import com.carddemo.posting.record.Records.CardXref;
import com.carddemo.posting.record.Records.TranCategory;
import com.carddemo.posting.record.Records.TranType;
import com.carddemo.posting.record.Records.Transaction;
import com.carddemo.posting.record.Text;
import com.carddemo.posting.record.Zoned;
import com.carddemo.posting.store.PostingStore;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TRANREPT: STEP05R (REPRO + SORT of TRANSACT) and STEP10R (CBTRN03C), the 133-byte daily
 * transaction report. The control flow of CBTRN03C is reproduced statement by statement,
 * quirks included; see java/posting/README.md.
 */
@Component
public class TransactionReportTasklet implements Tasklet {

    static final String JOB = "TRANREPT";
    static final String STEP = "STEP10R";
    static final String DATEPARM = "AWS.M2.CARDDEMO.DATEPARM";
    static final String TRANREPT = "AWS.M2.CARDDEMO.TRANREPT.G0001V00";
    static final int LRECL = 133;

    private static final String HEADER_2 = "-".repeat(LRECL);

    private final ChainRun run;
    private final PostingStore store;
    private final String sortStartDate;
    private final String sortEndDate;
    private final int pageSize;

    private List<String> sysout;
    private List<String> report;
    private String startDate;
    private String endDate;
    private long lineCounter;
    private BigDecimal pageTotal;
    private BigDecimal accountTotal;
    private BigDecimal grandTotal;

    public TransactionReportTasklet(ChainRun run, PostingStore store,
                                    @Value("${posting.report.sort-start-date}") String sortStartDate,
                                    @Value("${posting.report.sort-end-date}") String sortEndDate,
                                    @Value("${posting.report.page-size}") int pageSize) {
        this.run = run;
        this.store = store;
        this.sortStartDate = sortStartDate;
        this.sortEndDate = sortEndDate;
        this.pageSize = pageSize;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        sysout = run.sysout(JOB, STEP);
        report = new ArrayList<>();
        lineCounter = 0;
        pageTotal = BigDecimal.ZERO;
        accountTotal = BigDecimal.ZERO;
        grandTotal = BigDecimal.ZERO;
        try {
            produceReport(store.reportExtract(sortStartDate, sortEndDate));
        } finally {
            writeReport();
        }
        return RepeatStatus.FINISHED;
    }

    private void produceReport(List<Transaction> sorted) {
        sysout.add("START OF EXECUTION OF PROGRAM CBTRN03C");
        boolean endOfFile = !readDateParm();
        boolean firstTime = true;
        String currentCard = "";
        CardXref xref = null;
        // TRAN-RECORD starts as spaces and keeps the last record read once the file hits EOF.
        Transaction tranRecord = null;
        int next = 0;
        while (!endOfFile) {
            if (next < sorted.size()) {
                tranRecord = sorted.get(next++);
            } else {
                endOfFile = true;
            }
            String procDate = tranRecord == null ? Text.pad("", 10) : Text.slice(tranRecord.procTs(), 0, 10);
            if (procDate.compareTo(startDate) < 0 || procDate.compareTo(endDate) > 0) {
                // Quirk: NEXT SENTENCE leaves the whole PERFORM loop, so the report stops here
                // without totals (an empty TRANSACT file ends the report the same way).
                break;
            }
            if (!endOfFile) {
                sysout.add(tranRecord.format());
                if (!currentCard.equals(Text.pad(tranRecord.cardNum(), 16))) {
                    if (!firstTime) {
                        writeAccountTotals();
                    }
                    currentCard = Text.pad(tranRecord.cardNum(), 16);
                    xref = lookupXref(tranRecord.cardNum());
                }
                TranType type = lookupTranType(tranRecord.typeCd());
                TranCategory category = lookupTranCategory(tranRecord.typeCd(), tranRecord.catCd());
                if (firstTime) {
                    firstTime = false;
                    writeHeaders();
                }
                if (lineCounter % pageSize == 0) {
                    writePageTotals();
                    writeHeaders();
                }
                addToTotals(tranRecord.amount());
                writeDetail(tranRecord, xref, type, category);
            } else {
                sysout.add("TRAN-AMT " + Zoned.display(tranRecord.amount(), 9, 2));
                sysout.add("WS-PAGE-TOTAL" + Zoned.display(pageTotal, 9, 2));
                // Quirk: the last transaction is added to the page/account totals a second time.
                addToTotals(tranRecord.amount());
                writePageTotals();
                writeGrandTotals();
            }
        }
        sysout.add("END OF EXECUTION OF PROGRAM CBTRN03C");
    }

    private boolean readDateParm() {
        List<String> records = run.readInput(DATEPARM);
        if (records.isEmpty()) {
            return false;
        }
        String record = Text.pad(records.get(0), 80);
        startDate = record.substring(0, 10);
        endDate = record.substring(11, 21);
        sysout.add("Reporting from " + startDate + " to " + endDate);
        return true;
    }

    private void addToTotals(BigDecimal amount) {
        pageTotal = Zoned.fit(pageTotal.add(amount), 9, 2);
        accountTotal = Zoned.fit(accountTotal.add(amount), 9, 2);
    }

    private CardXref lookupXref(String cardNum) {
        return store.findXref(cardNum).orElseThrow(() -> abend("INVALID CARD NUMBER : " + Text.pad(cardNum, 16)));
    }

    private TranType lookupTranType(String typeCd) {
        return store.findTranType(typeCd).orElseThrow(() -> abend("INVALID TRANSACTION TYPE : "
                + Text.pad(typeCd, 2)));
    }

    private TranCategory lookupTranCategory(String typeCd, int catCd) {
        return store.findTranCategory(typeCd, catCd).orElseThrow(() -> abend("INVALID TRAN CATG KEY : "
                + Text.pad(typeCd, 2) + Text.digits(catCd, 4)));
    }

    private LegacyAbend abend(String message) {
        sysout.add(message);
        sysout.add("ABENDING PROGRAM");
        return new LegacyAbend(JOB + "/" + STEP + ": " + message);
    }

    private void write(String line) {
        report.add(Text.pad(line, LRECL));
    }

    private void writeHeaders() {
        write(Text.pad("DALYREPT", 38) + Text.pad("Daily Transaction Report", 41) + "Date Range: "
                + Text.pad(startDate, 10) + " to " + Text.pad(endDate, 10));
        write("");
        write(Text.pad("Transaction ID", 17) + Text.pad("Account ID", 12) + Text.pad("Transaction Type", 19)
                + Text.pad("Tran Category", 35) + Text.pad("Tran Source", 14) + " "
                + Text.pad("        Amount", 16));
        write(HEADER_2);
        lineCounter += 4;
    }

    private void writePageTotals() {
        write(Text.pad("Page Total", 11) + ".".repeat(86) + Zoned.edit(pageTotal, true));
        grandTotal = Zoned.fit(grandTotal.add(pageTotal), 9, 2);
        pageTotal = BigDecimal.ZERO;
        write(HEADER_2);
        lineCounter += 2;
    }

    private void writeAccountTotals() {
        write(Text.pad("Account Total", 13) + ".".repeat(84) + Zoned.edit(accountTotal, true));
        accountTotal = BigDecimal.ZERO;
        write(HEADER_2);
        lineCounter += 2;
    }

    private void writeGrandTotals() {
        write(Text.pad("Grand Total", 11) + ".".repeat(86) + Zoned.edit(grandTotal, true));
    }

    private void writeDetail(Transaction tran, CardXref xref, TranType type, TranCategory category) {
        write(Text.pad(tran.tranId(), 16) + " " + Text.digits(xref.acctId(), 11) + " " + Text.pad(tran.typeCd(), 2)
                + "-" + Text.pad(type.description(), 15) + " " + Text.digits(tran.catCd(), 4) + "-"
                + Text.pad(category.description(), 29) + " " + Text.pad(tran.source(), 10) + "    "
                + Zoned.edit(tran.amount(), false) + "  ");
        lineCounter++;
    }

    private void writeReport() throws IOException {
        Path target = run.dataset(TRANREPT);
        Files.createDirectories(target.getParent());
        StringBuilder text = new StringBuilder();
        report.forEach(line -> text.append(Text.rtrim(line)).append('\n'));
        Files.writeString(target, text, StandardCharsets.ISO_8859_1);
    }
}
