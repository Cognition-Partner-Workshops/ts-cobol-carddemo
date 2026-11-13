# posting: POSTTRAN + TRANREPT

Java 21 Spring Batch port of CBTRN02C and CBTRN03C. The `posting` job runs
`POSTTRAN.STEP15` then `TRANREPT.STEP10R`; the report is attempted even when
posting returns a condition code, like the recorder's two JCL jobs. Rejects
produce POSTTRAN RC 4 without failing the Batch job. An abend produces RC 12,
its diagnostic and a failed Batch job.

All fixed datasets are decoded/encoded through `modern/record-codec`.
`schema.sql` declares typed columns for every named field in account, card
xref, category balance, disclosure group, transaction type/category, daily and
initial transactions, rejects and date parameters. Keyed datasets use their VSAM
primary keys. Nonunique indexes match XREFFILE's account-id alternate key and
TRANFILE's processing-timestamp alternate key. Sequential inputs retain file
order, including duplicate daily IDs, using an identity ordinal.

The transaction master starts empty, matching CBTRN02C's OPEN OUTPUT, even if
DALYTRAN.PS.INIT contained transactions. Posting validates the card/account,
credit limit and inclusive account-expiry date, updates account/category rows,
and creates missing category balances. Report transactions are key-ordered
before the stable card-number sort; the JCL SORT date window and DATEPARM
window are separate filters. Report fields follow CVTRA07Y and the 133-byte FD.

Entry point (parameters must come from chains.json and TRANREPT.jcl):
```sh
java -jar modern/posting/target/posting-0.1.0.jar INPUT OUTPUT CLOCK SORT_START SORT_END
```

Exports follow the recorder contract: five `datasets/` files (transaction,
account, category balance, reject GDG and report GDG), two `sysout/` files and
step-by-step `rc.json`. Setup-loader warnings are written separately under
`setup/`. GDGs start at G0001V00 for a fresh case. No DB snapshots belong to
the posting chain's manifest.

## First candidate and verification boundaries

This is the straight business-intent port required by **Convert one COBOL chain
to Spring Batch**, not a parity claim. Credit-limit validation initially uses
the current balance, and report totals do not deliberately double-count the EOF
record. Do not treat green unit tests as **COBOL batch parity standard**
evidence. The post-review container run must establish field-level equivalence
and document every legacy cause before any parity correction.

Unit tests exercise all seven posting fixture inputs without reading golden
outputs, plus duplicate IDs/setup keys, frozen-clock parameters, both date
filters, missing type/category abends, DATEPARM EOF, numeric schema precision,
codec copybook audits and Spring Batch condition-code/failure handling.
These extra error and pagination paths are not proven by fixture parity.
The off-host CICS, HLASM and PL/I paths remain analysis-only.
