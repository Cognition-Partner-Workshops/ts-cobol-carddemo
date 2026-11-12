package com.carddemo.modern.posting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.carddemo.modern.codec.Layouts;
import org.carddemo.modern.codec.RecordLayout;
import org.carddemo.modern.codec.RejectRecord;
import org.junit.jupiter.api.Test;

class PostingRulesTest {
    private Map<String,Object> account(String balance, String credit, String debit, String limit, String expiry) {
        var r = new HashMap<String,Object>();
        r.put("ACCT-CURR-BAL",new BigDecimal(balance));
        r.put("ACCT-CURR-CYC-CREDIT",new BigDecimal(credit));
        r.put("ACCT-CURR-CYC-DEBIT",new BigDecimal(debit));
        r.put("ACCT-CREDIT-LIMIT",new BigDecimal(limit));
        r.put("ACCT-EXPIRAION-DATE",expiry);
        return r;
    }
    private Map<String,Object> daily(String amount) {
        return Map.of("DALYTRAN-AMT",new BigDecimal(amount),"DALYTRAN-ORIG-TS","2022-07-04-09.15.00.000000");
    }
    @Test void limitUsesCyclesRatherThanBalanceAndAcceptsEquality() {
        var a = account("9999","950","0","1000","2026-12-31");
        assertEquals(0,Cbtrn02cProcessor.validate(1003L,a,daily("50")));
        assertEquals(102,Cbtrn02cProcessor.validate(1003L,a,daily("50.01")));
        a = account("0","950","-100","1000","2026-12-31");
        assertEquals(102,Cbtrn02cProcessor.validate(1003L,a,daily("0")));
    }
    @Test void validationOrderAndExpirationOverride() {
        var a = account("0","950","0","1000","2022-01-31");
        assertEquals(100,Cbtrn02cProcessor.validate(null,a,daily("100")));
        assertEquals(101,Cbtrn02cProcessor.validate(1L,null,daily("100")));
        assertEquals(103,Cbtrn02cProcessor.validate(1L,a,daily("100")));
        a.put("ACCT-EXPIRAION-DATE","2022-07-04");
        assertEquals(0,Cbtrn02cProcessor.validate(1L,a,daily("50")));
    }
    @Test void negativeOverpunchIsPreservedForAllPostingSignedFields() {
        roundTrip(Layouts.DALYTRAN,"DALYTRAN-AMT",142);
        roundTrip(Layouts.TRAN,"TRAN-AMT",142);
        roundTrip(Layouts.TRAN_CAT_BAL,"TRAN-CAT-BAL",27);
        roundTrip(Layouts.ACCOUNT,"ACCT-CURR-CYC-DEBIT",101);
    }
    private void roundTrip(RecordLayout layout, String field, int end) {
        for (String amount : new String[]{"-15.75","-200.00","0.00","15.75"}) {
            var n = new BigDecimal(amount);
            String line = layout.encode(Map.of(field,n));
            assertEquals(n,layout.decode(line).get(field));
            if (amount.equals("-15.75")) assertEquals('N',line.charAt(end));
            if (amount.equals("-200.00")) assertEquals('}',line.charAt(end));
        }
    }
    @Test void rejectCodesAreUnsignedAndDescriptionsMatch() {
        for (int reason : new int[]{100,101,102,103}) {
            String line = new RejectRecord("",reason,Cbtrn02cProcessor.reasonDescription(reason)).toLine();
            assertEquals(Cbtrn02cProcessor.reasonDescription(reason),line.substring(354));
        }
        assertThrows(IllegalArgumentException.class,() -> Cbtrn02cProcessor.reasonDescription(999));
    }
    @Test void eofDoubleAddsLastAmountAndDoesNotFlushLastAccount() {
        var repo = mock(PostingRepository.class);
        when(repo.accountForCard("4000000000001001")).thenReturn(1001L);
        when(repo.typeDescription("01")).thenReturn("Purchase");
        when(repo.categoryDescription("01",1)).thenReturn("Regular Sales Draft");
        var p = new Cbtrn03cProcessor(repo,"2022-01-01","2022-07-06");
        p.process(Layouts.TRAN.encode(Map.of("TRAN-ID","1","TRAN-CARD-NUM","4000000000001001",
                "TRAN-TYPE-CD","01","TRAN-CAT-CD",1,"TRAN-PROC-TS","2022-07-05-10.00.00.000000","TRAN-AMT",new BigDecimal("-15.75"))));
        p.finish();
        assertTrue(p.lines().stream().anyMatch(s -> s.startsWith("Grand Total") && s.endsWith("31.50")));
        assertFalse(p.lines().stream().anyMatch(s -> s.startsWith("Account Total")));
        assertEquals(new BigDecimal("-26.25"),Cbtrn03cProcessor.eofPageTotal(new BigDecimal("-10.50"),new BigDecimal("-15.75")));
    }
    @Test void emptyAndDateExcludedReportsHaveNoHeadersOrTotals() {
        var p = new Cbtrn03cProcessor(mock(PostingRepository.class),"2023-01-01","2023-12-31");
        p.process(Layouts.TRAN.encode(Map.of("TRAN-PROC-TS","2022-07-05-10.00.00.000000")));
        p.finish();
        assertTrue(p.lines().isEmpty());
        var empty = new Cbtrn03cProcessor(mock(PostingRepository.class),"2022-01-01","2022-07-06");
        empty.finish();
        assertTrue(empty.lines().isEmpty());
    }
    @Test void rewrittenFillersAndAbendCloseOrderArePreserved() {
        var fields = Layouts.ACCOUNT.decode(Layouts.ACCOUNT.encode(Map.of("FILLER@122","AUDIT")),true);
        assertTrue(Layouts.ACCOUNT.encode(fields).substring(122).startsWith("AUDIT"));
        var category = Layouts.TRAN_CAT_BAL.decode(Layouts.TRAN_CAT_BAL.encode(Map.of("FILLER@28","AUDIT")),true);
        assertTrue(Layouts.TRAN_CAT_BAL.encode(category).substring(28).startsWith("AUDIT"));
        var out = new java.util.ArrayList<String>();
        Abend.append(out,"ERROR WRITING TO TRANSACTION FILE",22,Abend.POST_FILES);
        assertEquals("CEE3ABD: USER ABEND U+0000000999",out.get(3));
        assertEquals("",out.get(4));
        assertEquals("libcob: warning: implicit CLOSE of TCATBAL-FILE ('TCATBALF')",out.get(5));
        assertEquals("libcob: warning: implicit CLOSE of DALYTRAN-FILE ('DALYTRAN')",out.getLast());
    }

    @Test void reportLookupFailureKeepsAbendOutputWithoutNormalTotals() {
        var repo = mock(PostingRepository.class);
        when(repo.accountForCard("4000000000001001")).thenReturn(1001L);
        when(repo.typeDescription("01")).thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));
        var p = new Cbtrn03cProcessor(repo,"2022-01-01","2022-07-06");
        p.process(Layouts.TRAN.encode(Map.of("TRAN-ID","1","TRAN-CARD-NUM","4000000000001001",
                "TRAN-TYPE-CD","01","TRAN-CAT-CD",1,"TRAN-PROC-TS","2022-07-05-10.00.00.000000")));
        p.finish();
        assertEquals(12,p.returnCode());
        assertTrue(p.sysout().contains("CEE3ABD: USER ABEND U+0000000999"));
        assertEquals("libcob: warning: implicit CLOSE of TRANSACT-FILE ('TRANFILE')",p.sysout().getLast());
        assertFalse(p.lines().stream().anyMatch(s -> s.startsWith("Grand Total")));
    }

    @Test void failedReportOpenClosesOnlyEarlierOpensInReverseOrder() {
        var repo = mock(PostingRepository.class);
        var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        when(repo.jdbc()).thenReturn(jdbc);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"))
                .when(jdbc).execute("SELECT 1 FROM posting.transaction_type LIMIT 0");
        var p = new Cbtrn03cProcessor(repo,"2022-01-01","2022-07-06");
        p.open();
        p.finish();
        assertEquals(12,p.returnCode());
        assertTrue(p.sysout().contains("ERROR OPENING TRANSACTION TYPE FILE"));
        assertEquals(java.util.List.of("libcob: warning: implicit CLOSE of XREF-FILE ('CARDXREF')",
                "libcob: warning: implicit CLOSE of REPORT-FILE ('TRANREPT')",
                "libcob: warning: implicit CLOSE of TRANSACT-FILE ('TRANFILE')"),
                p.sysout().subList(p.sysout().size()-3,p.sysout().size()));
        assertFalse(p.sysout().stream().anyMatch(s -> s.startsWith("Reporting from")));
    }

    private org.springframework.transaction.PlatformTransactionManager transactions() {
        var tx = mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenAnswer(invocation -> new org.springframework.transaction.support.SimpleTransactionStatus());
        return tx;
    }

    private Cbtrn02cProcessor.Posted posted(boolean create) {
        return new Cbtrn02cProcessor.Posted(1,Map.of(),Map.of(),create,Map.of(),null);
    }

    @Test void categoryWriteAndRewriteFailuresStopBeforeAccountOrTransaction() {
        for (boolean create : new boolean[]{true,false}) {
            var repo = mock(PostingRepository.class);
            var processor = mock(Cbtrn02cProcessor.class);
            var tx = transactions();
            doThrow(new org.springframework.dao.DuplicateKeyException("duplicate"))
                    .when(repo).postCategory(anyMap(),eq(create));
            new PostWriter(repo,processor,tx).write(new org.springframework.batch.item.Chunk<>(java.util.List.of(posted(create))));
            verify(processor).abend(create ? "ERROR WRITING TRANSACTION BALANCE FILE" : "ERROR REWRITING TRANSACTION BALANCE FILE",22);
            verify(repo,never()).rewriteAccount(anyMap());
            verify(repo,never()).transaction(anyMap());
            verify(tx).rollback(any());
        }
    }

    @Test void uncheckedAccountRewriteStatusStillAllowsTransactionWrite() {
        var repo = mock(PostingRepository.class);
        var processor = mock(Cbtrn02cProcessor.class);
        var tx = transactions();
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"))
                .when(repo).rewriteAccount(anyMap());
        new PostWriter(repo,processor,tx).write(new org.springframework.batch.item.Chunk<>(java.util.List.of(posted(false))));
        var order = inOrder(repo);
        order.verify(repo).postCategory(anyMap(),eq(false));
        order.verify(repo).rewriteAccount(anyMap());
        order.verify(repo).transaction(anyMap());
        verify(processor,never()).abend(anyString(),anyInt());
        verify(tx,times(2)).commit(any());
        verify(tx).rollback(any());
    }

    @Test void duplicateTransactionRollsBackOnlyItsOperation() {
        var repo = mock(PostingRepository.class);
        var processor = mock(Cbtrn02cProcessor.class);
        var tx = transactions();
        doThrow(new org.springframework.dao.DuplicateKeyException("duplicate"))
                .when(repo).transaction(anyMap());
        new PostWriter(repo,processor,tx).write(new org.springframework.batch.item.Chunk<>(java.util.List.of(posted(false))));
        verify(processor).abend("ERROR WRITING TO TRANSACTION FILE",22);
        verify(tx,times(2)).commit(any());
        verify(tx).rollback(any());
    }
}
