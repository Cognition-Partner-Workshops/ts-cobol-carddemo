# POSTTRAN / TRANREPT posting candidate

Java 21 Spring Batch ports CBTRN02C and CBTRN03C. The `posting` PostgreSQL
schema stores one typed column per named source field in account, card_xref,
tran_cat_balance, posted_transaction, transaction_type and transaction_category.
Primary keys match the VSAM keys; card account-id has a nonunique alternate
index. NUMERIC precision/scale match the PICs, and textual key ordering uses
the C collation. Every fixed-length dataset uses `modern/record-codec`.

The five CLI arguments become Batch job parameters:
`posting <input> <output> <clock> <sort-start> <sort-end>`.
The harness derives the frozen clock from chains.json and the SORT dates from
TRANREPT.jcl. DATEPARM supplies CBTRN03C's separate report date range.

The loader reads fixture inputs only. It imports each indexed dataset in input
order, skipping duplicate primary keys as the legacy REPRO loader does. It
initializes an empty transaction file, validates the daily transactions in
input order, writes reason-coded rejects, updates account/category balances,
then sorts accepted transactions by card for the report. Exports use the
recorder's datasets/SYSOUT/rc.json contract; no expected output is read at runtime.

## First-run differences and preserved behavior

The straight port was committed before the recorded container run. p01–p06
were red and p07-empty-day was green. Each cause was explained before changing
code, then fixed in its own commit with a regression test.

| Cases | Field / cause | Matching behavior |
|---|---|---|
| p01–p06 | TRAN-PROC-TS and transaction SYSOUT used a space | CBTRN02C Z-GET-DB2-FORMAT-TIMESTAMP inserts a hyphen: `2022-07-05-10.00.00.000000` |
| p02–p05 | REJ-FAIL-REASON was FILLER in the shared codec | REJTRAN.cpy names bytes 350–353 as PIC 9(4): encode 0100/0101/0102/0103, retaining the daily payload and 76-byte description |
| p01–p06 | Report detail/total amounts were width 14 with a floating sign | CVTRA07Y uses width 15 with a fixed sign: `-ZZZ,ZZZ,ZZZ.ZZ` / `+ZZZ,ZZZ,ZZZ.ZZ` |
| p01–p06 | Page/grand totals and two SYSOUT lines differed | CBTRN03C 1000-TRANFILE-GET-NEXT leaves TRAN-RECORD unchanged at EOF; the main loop's ELSE displays the last amount and pre-add page total, then adds that amount again before printing totals |

The EOF behavior is an intentional reproduced defect. For p01 page/grand
totals are −26.25 rather than the sum −10.50; p02 84.00 rather than 42.00;
p03 40.00 rather than 20.00; p04 100.00 rather than 50.00; p05 60.00 rather
than 30.00; p06 −205.50 rather than −265.50. Account total lines printed
before EOF retain their original sums. Persisted balances are not doubled.
Whether this report defect should survive cutover requires a business decision.

CBTRN02C's limit check is deliberately cycle based:
`cycle credit - signed cycle debit + daily amount <= credit limit`.
Expiration overrides the overlimit reason. Current fixtures do not distinguish
this formula from a current-balance check; an explicit boundary test does.
Reject reasons are 100 invalid card, 101 missing account, 102 overlimit and
103 expired account. Rejects give POSTTRAN RC 4 without failing the Batch job.

Missing report lookups and duplicate transaction writes reproduce file-status
23/22, the ABENDING PROGRAM / local CEE3ABD U+0000000999 diagnostics and RC 12;
the Batch job then fails. Missing lookups have regression tests but these
abend paths are **unproven by fixture parity**. Unexpected I/O/SQL exceptions
also fail the Batch job rather than exporting hard-coded success.

## Layout audit

Every called layout was checked against its source, including inherited ones:

| Source | Layout / length | Named numeric fields |
|---|---|---|
| CVACT01Y | ACCOUNT / 300 | unsigned 11-digit ID; signed zoned 12 digits, scale 2 for all money |
| CVACT03Y | XREF / 50 | unsigned customer 9 and account 11 digits |
| CVTRA01Y | CATEGORY_BALANCE / 50 | account 11, category 4, signed zoned balance 11/2 |
| CVTRA05Y / CVTRA06Y | TRAN / DALYTRAN / 350 | category 4, signed zoned amount 11/2 at 132, merchant 9 at 143; timestamps at 278 and 304 |
| parity REJTRAN | REJTRAN / 430 | daily fields followed by reason 4 at 350 and description at 354 |
| CVTRA03Y / CVTRA04Y | TYPE / CATEGORY / 60 | category 4 at 2; descriptions 50; only true FILLER omitted from SQL |
| CBTRN03C WS-DATEPARM-RECORD | DATE / 80 | start/end X(10) at 0/11 |
| CBTRN03C FD-REPTFILE-REC / CVTRA07Y | REPORT / 133; DETAIL / 114 | edited amount text at 97, width 15; account ID text preserves PIC display |

The codec retains FILLER/padding and EBCDIC overpunch signs. Fixture exports
strip only trailing spaces, as the recorder does. Maven tests cover reason
encoding, numeric editing, timestamps, validation boundaries, report totals,
empty input and an abend. Run all golden cases with `make parity CHAIN=posting
TARGET=java`; run the reactor tests using the command in `modern/README.md`.
