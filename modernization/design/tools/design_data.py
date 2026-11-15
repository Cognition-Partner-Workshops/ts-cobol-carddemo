"""Hand-written content of the interest-stream design (step 4).

build_design.py joins this with the stream manifest, the rulebook and the
fixtures, and writes every file under modernization/design/. Nothing here is
approved: every item is PROPOSED and names the role that has to confirm it.
"""

architectureVersion = "0.1.0"
designBranch = "demo/04-design"
baseSha = "18db1a56bbfd3d9c289395337e931af1df852637"
baseBranch = "demo/03-rules"
streamRoot = "INTCALC"
targetStack = "Java 21, Spring Batch 5 (Spring Boot 3), PostgreSQL 15"
targetDir = "java/interest"
schemaFile = "db/postgres/02-interest.sql"

# ------------------------------------------------------------------ tables
# owner = who is allowed to write the table in the target while the rest of the
# estate stays on the mainframe.
tables = [
    {"id": "account", "name": "carddemo.account", "copybook": "CVACT01Y", "legacy": "AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS",
     "kind": "replica", "systemOfRecord": "mainframe (ACCTDATA) until the account writers move",
     "writer": "interest-loader (truncate and load); interest-calc (UPDATE of acct_curr_bal, acct_curr_cyc_credit, acct_curr_cyc_debit only)",
     "readers": "interest-calc, interest-export",
     "primaryKey": "acct_id", "legacyKey": "KSDS KEYS(11 0) - ACCT-ID (app/jcl/ACCTFILE.jcl:36)",
     "indexes": [], "legacyIndexes": "none defined for this file"},
    {"id": "cardXref", "name": "carddemo.card_xref", "copybook": "CVACT03Y", "legacy": "AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS",
     "kind": "replica", "systemOfRecord": "mainframe (CARDXREF)",
     "writer": "interest-loader only", "readers": "interest-calc",
     "primaryKey": "xref_card_num", "legacyKey": "KSDS KEYS(16 0) - XREF-CARD-NUM (app/jcl/XREFFILE.jcl:39)",
     "indexes": ["card_xref_acct_ix ON (xref_acct_id, xref_card_num) - not unique"],
     "legacyIndexes": "alternate index KEYS(11,25) NONUNIQUEKEY UPGRADE on XREF-ACCT-ID, reached through path CARDXREF.VSAM.AIX.PATH (app/jcl/XREFFILE.jcl:72-92)"},
    {"id": "tranCatBalance", "name": "carddemo.tran_cat_balance", "copybook": "CVTRA01Y", "legacy": "AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS",
     "kind": "replica", "systemOfRecord": "mainframe (TCATBALF, written by POSTTRAN)",
     "writer": "interest-loader only", "readers": "interest-calc",
     "primaryKey": "trancat_acct_id, trancat_type_cd, trancat_cd", "legacyKey": "KSDS KEYS(17 0) - TRAN-CAT-KEY (app/jcl/TCATBALF.jcl:36)",
     "indexes": [], "legacyIndexes": "none"},
    {"id": "disclosureGroup", "name": "carddemo.disclosure_group", "copybook": "CVTRA02Y", "legacy": "AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS",
     "kind": "replica", "systemOfRecord": "mainframe (DISCGRP, reloaded by job DISCGRP)",
     "writer": "interest-loader only", "readers": "interest-calc",
     "primaryKey": "dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd", "legacyKey": "KSDS KEYS(16 0) - DIS-GROUP-KEY (app/jcl/DISCGRP.jcl:36)",
     "indexes": [], "legacyIndexes": "none"},
    {"id": "interestTransaction", "name": "carddemo.interest_transaction", "copybook": "CVTRA05Y", "legacy": "AWS.M2.CARDDEMO.SYSTRAN(+1)",
     "kind": "owned", "systemOfRecord": "target (one set of rows per run = one SYSTRAN generation)",
     "writer": "interest-calc only (INSERT)", "readers": "interest-export",
     "primaryKey": "run_id, seq_no", "legacyKey": "none - SYSTRAN is a sequential file in write order (app/jcl/INTCALC.jcl:37-41)",
     "indexes": ["UNIQUE (run_id, tran_id)"],
     "legacyIndexes": "downstream only: TRANSACT.VSAM.KSDS KEYS(16 0) on TRAN-ID and alternate index KEYS(26 304) on TRAN-PROC-TS (app/jcl/TRANFILE.jcl:53, :84); both belong to COMBTRAN's target file, not to this stream"},
    {"id": "interestRun", "name": "carddemo.interest_run", "copybook": None, "legacy": "none (new control table)",
     "kind": "owned", "systemOfRecord": "target",
     "writer": "interest-run-control (one row per run; counts and sums per step)", "readers": "interest-export, reconciliation report",
     "primaryKey": "run_id", "legacyKey": "none", "indexes": [], "legacyIndexes": "none"},
    {"id": "batchMeta", "name": "batch.* (Spring Batch job repository)", "copybook": None, "legacy": "none (framework tables)",
     "kind": "framework", "systemOfRecord": "target",
     "writer": "Spring Batch only", "readers": "Spring Batch, operations",
     "primaryKey": "framework-defined", "legacyKey": "none", "indexes": [], "legacyIndexes": "none"},
]

# extra columns that have no copybook field
extraColumns = {
    "interestTransaction": [
        ("run_id", "BIGINT NOT NULL REFERENCES carddemo.interest_run", "which run (SYSTRAN generation) wrote the row"),
        ("seq_no", "INTEGER NOT NULL", "write order inside the run, 1..n; the export writes records in this order"),
    ],
    "interestRun": [
        ("run_id", "BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY", "one row per run"),
        ("parm_date", "CHAR(10) NOT NULL", "the PARM of INTCALC.STEP15 (INT-Q02)"),
        ("started_at / finished_at", "TIMESTAMPTZ", "wall clock of the target run (not the legacy run clock)"),
        ("status", "VARCHAR(12) NOT NULL", "LOADED, CALCULATED, EXPORTED, FAILED or ABANDONED"),
        ("loaded_* / written_* counts and sums", "BIGINT / NUMERIC(18,2)", "the reconciliation figures of strategy items RC1-RC6"),
    ],
}

# ------------------------------------------------------- field -> column map
# keyed by copybook then COBOL field name. FILLER and group items are included.
def col(column, pgType, role, note=""):
    return {"column": column, "pgType": pgType, "role": role, "note": note}

fieldMap = {
    "CVACT01Y": {
        "ACCT-ID": col("acct_id", "NUMERIC(11,0) NOT NULL CHECK (acct_id >= 0)", "key", "primary key; read by key (manifest edge D4)"),
        "ACCT-ACTIVE-STATUS": col("acct_active_status", "CHAR(1) NOT NULL", "carried", "never read by CBACT04C (INT-H04)"),
        "ACCT-CURR-BAL": col("acct_curr_bal", "NUMERIC(12,2) NOT NULL", "written", "interest added at the account break (INT-R07, INT-Q04)"),
        "ACCT-CREDIT-LIMIT": col("acct_credit_limit", "NUMERIC(12,2) NOT NULL", "carried"),
        "ACCT-CASH-CREDIT-LIMIT": col("acct_cash_credit_limit", "NUMERIC(12,2) NOT NULL", "carried"),
        "ACCT-OPEN-DATE": col("acct_open_date", "CHAR(10) NOT NULL", "carried", "kept as text (gap G04)"),
        "ACCT-EXPIRAION-DATE": col("acct_expiraion_date", "CHAR(10) NOT NULL", "carried", "legacy spelling kept so the name maps 1:1; kept as text (G04)"),
        "ACCT-REISSUE-DATE": col("acct_reissue_date", "CHAR(10) NOT NULL", "carried", "kept as text (G04)"),
        "ACCT-CURR-CYC-CREDIT": col("acct_curr_cyc_credit", "NUMERIC(12,2) NOT NULL", "written", "set to zero at the account break (INT-R07)"),
        "ACCT-CURR-CYC-DEBIT": col("acct_curr_cyc_debit", "NUMERIC(12,2) NOT NULL", "written", "set to zero at the account break (INT-R07)"),
        "ACCT-ADDR-ZIP": col("acct_addr_zip", "CHAR(10) NOT NULL", "carried"),
        "ACCT-GROUP-ID": col("acct_group_id", "CHAR(10) NOT NULL", "read", "first part of the rate key (INT-R02)"),
        "FILLER": col("acct_filler", "BYTEA NOT NULL CHECK (octet_length(acct_filler) = 178)", "carried", "raw bytes, written back unchanged"),
    },
    "CVACT03Y": {
        "XREF-CARD-NUM": col("xref_card_num", "CHAR(16) NOT NULL", "key", "primary key; copied to TRAN-CARD-NUM (INT-R06)"),
        "XREF-CUST-ID": col("xref_cust_id", "NUMERIC(9,0) NOT NULL CHECK (xref_cust_id >= 0)", "carried"),
        "XREF-ACCT-ID": col("xref_acct_id", "NUMERIC(11,0) NOT NULL CHECK (xref_acct_id >= 0)", "key", "alternate key, not unique (G08, INT-H03)"),
        "FILLER": col("xref_filler", "BYTEA NOT NULL CHECK (octet_length(xref_filler) = 14)", "carried", "raw bytes"),
    },
    "CVTRA01Y": {
        "TRAN-CAT-KEY": col("(primary key)", "PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd)", "key", "group item: the three fields below, in this order; read in key order (INT-R01, G06)"),
        "TRANCAT-ACCT-ID": col("trancat_acct_id", "NUMERIC(11,0) NOT NULL CHECK (trancat_acct_id >= 0)", "key"),
        "TRANCAT-TYPE-CD": col("trancat_type_cd", 'CHAR(2) COLLATE "C" NOT NULL', "key", "second part of the rate key"),
        "TRANCAT-CD": col("trancat_cd", "NUMERIC(4,0) NOT NULL CHECK (trancat_cd >= 0)", "key", "third part of the rate key (G09)"),
        "TRAN-CAT-BAL": col("tran_cat_bal", "NUMERIC(11,2) NOT NULL", "read", "the balance interest is computed on (INT-R05)"),
        "FILLER": col("trancat_filler", "BYTEA NOT NULL CHECK (octet_length(trancat_filler) = 22)", "carried", "raw bytes; the whole record is echoed to SYSOUT (INT-T02)"),
    },
    "CVTRA02Y": {
        "DIS-GROUP-KEY": col("(primary key)", "PRIMARY KEY (dis_acct_group_id, dis_tran_type_cd, dis_tran_cat_cd)", "key", "group item: the three fields below; read by full key (INT-R02, INT-R03)"),
        "DIS-ACCT-GROUP-ID": col("dis_acct_group_id", 'CHAR(10) COLLATE "C" NOT NULL', "key", "the literal DEFAULT is the fallback group (INT-R03)"),
        "DIS-TRAN-TYPE-CD": col("dis_tran_type_cd", 'CHAR(2) COLLATE "C" NOT NULL', "key"),
        "DIS-TRAN-CAT-CD": col("dis_tran_cat_cd", "NUMERIC(4,0) NOT NULL CHECK (dis_tran_cat_cd >= 0)", "key", "(G09)"),
        "DIS-INT-RATE": col("dis_int_rate", "NUMERIC(6,2) NOT NULL", "read", "yearly rate in percent; zero means no interest (INT-R04)"),
        "FILLER": col("dis_filler", "BYTEA NOT NULL CHECK (octet_length(dis_filler) = 28)", "carried", "raw bytes"),
    },
    "CVTRA05Y": {
        "TRAN-ID": col("tran_id", "CHAR(16) NOT NULL", "written", "PARM date (10) + six-digit counter (INT-Q02, INT-H05); unique inside a run only"),
        "TRAN-TYPE-CD": col("tran_type_cd", "CHAR(2) NOT NULL", "written", "always 01 (INT-R06)"),
        "TRAN-CAT-CD": col("tran_cat_cd", "NUMERIC(4,0) NOT NULL CHECK (tran_cat_cd >= 0)", "written", "always 0005 (INT-R06)"),
        "TRAN-SOURCE": col("tran_source", "CHAR(10) NOT NULL", "written", "always System (INT-R06)"),
        "TRAN-DESC": col("tran_desc", "CHAR(100) NOT NULL", "written", "Int. for a/c + 11-digit account id; the rest is not set by the program (G05, INT-T04)"),
        "TRAN-AMT": col("tran_amt", "NUMERIC(11,2) NOT NULL", "written", "monthly interest, cut off at the cent (INT-R05, INT-Q01)"),
        "TRAN-MERCHANT-ID": col("tran_merchant_id", "NUMERIC(9,0) NOT NULL CHECK (tran_merchant_id >= 0)", "written", "always zero"),
        "TRAN-MERCHANT-NAME": col("tran_merchant_name", "CHAR(50) NOT NULL", "written", "always spaces"),
        "TRAN-MERCHANT-CITY": col("tran_merchant_city", "CHAR(50) NOT NULL", "written", "always spaces"),
        "TRAN-MERCHANT-ZIP": col("tran_merchant_zip", "CHAR(10) NOT NULL", "written", "always spaces"),
        "TRAN-CARD-NUM": col("tran_card_num", "CHAR(16) NOT NULL", "written", "card number of the account from card_xref (INT-R06, INT-H03)"),
        "TRAN-ORIG-TS": col("tran_orig_ts", "CHAR(26) NOT NULL", "written", "YYYY-MM-DD-HH.MM.SS.hh0000 (INT-Q03, INT-T05, G10)"),
        "TRAN-PROC-TS": col("tran_proc_ts", "CHAR(26) NOT NULL", "written", "same value as TRAN-ORIG-TS (INT-Q03, G10)"),
        "FILLER": col("tran_filler", "BYTEA NOT NULL CHECK (octet_length(tran_filler) = 20)", "written", "not set by the program (G05, INT-T04)"),
    },
}

encodingRules = [
    {"id": "E1", "applies": "PIC 9(n) - unsigned zoned decimal, USAGE DISPLAY", "decode": "n digit characters (EBCDIC X'F0'-X'F9'; in the fixture files the characters 0-9). Anything else rejects the record.", "encode": "zero-padded to n digits"},
    {"id": "E2", "applies": "PIC S9(n)V99 - signed zoned decimal, sign in the last byte (trailing overpunch, no SIGN SEPARATE)", "decode": "last byte carries digit and sign: X'C0'-X'C9' ({ A-I) positive, X'D0'-X'D9' (} J-R) negative, X'F0'-X'F9' accepted as positive; V is an implied decimal point, two places", "encode": "always C for zero or positive and D for negative (what the golden files hold). An F-signed source digit therefore comes back as C (G02); minus zero comes back as plus zero (G03)"},
    {"id": "E3", "applies": "PIC X(n) - text", "decode": "host: EBCDIC code page to be confirmed (G01), fixtures: ISO-8859-1. Stored as CHAR(n), so trailing spaces are kept by padding and ignored in comparisons, as in COBOL. A byte that is not a character of the code page (for example X'00') rejects the record (G07)", "encode": "padded with spaces to n bytes in the output code page"},
    {"id": "E4", "applies": "FILLER", "decode": "kept as the n raw bytes in a BYTEA column, no translation", "encode": "written back byte for byte; for a new interest transaction the filler is n spaces in the output code page (G05)"},
    {"id": "E5", "applies": "COMP-3 (packed) and COMP (binary)", "decode": "none of the {fieldCount} fields of the five copybooks is COMP-3 or COMP; every field is USAGE DISPLAY. The only binary item the program sees is PARM-LENGTH S9(04) COMP in its LINKAGE SECTION, which is not on any dataset", "encode": "not needed. The codec must refuse a copybook field whose USAGE is not DISPLAY instead of guessing"},
    {"id": "E6", "applies": "Key order", "decode": "VSAM returns TCATBALF in the byte order of the EBCDIC key. The target reads ORDER BY trancat_acct_id, trancat_type_cd COLLATE \"C\", trancat_cd", "encode": "equal for all-digit or all-letter type codes; differs when letters and digits are mixed, because EBCDIC sorts letters before digits (G06)"},
]

gaps = [
    {"id": "G01", "title": "Host code page is not stated", "detail": "No CCSID is in the JCL, the copybooks or the scheduler export. The parity tool assumes cp037 for EBCDIC. Needed to decode every PIC X field and the brace and letter sign bytes.", "affects": "all five layouts", "owner": "mainframe data owner", "closes": "a statement of the CCSID, or a hex dump of one record per file"},
    {"id": "G02", "title": "Sign of positive zoned numbers on the host", "detail": "The golden files hold C-signed positives. Host data written by other programs or loaded by IDCAMS may hold F-signed (unsigned) digits. The value is the same, the byte is not, so a re-encoded record can differ by one byte per amount.", "affects": "every S9 field ({signedCount} fields)", "owner": "mainframe data owner", "closes": "the compile options (NUMPROC) and a sample of production records"},
    {"id": "G03", "title": "Minus zero", "detail": "A zoned field can hold -0 (X'D0' in the last byte). NUMERIC has no minus zero; it is written back as +0.", "affects": "every S9 field", "owner": "mainframe data owner", "closes": "trial-load profile: count of -0 values (expected 0)"},
    {"id": "G04", "title": "Dates kept as text", "detail": "ACCT-OPEN-DATE, ACCT-EXPIRAION-DATE and ACCT-REISSUE-DATE are PIC X(10). The fixtures hold YYYY-MM-DD, but nothing in the stream reads them, so their content on the host is unverified. They stay CHAR(10) so that no record is rejected or changed.", "affects": "CVACT01Y, 3 fields", "owner": "data owner", "closes": "trial-load profile of the three fields; then a decision whether to type them as DATE"},
    {"id": "G05", "title": "Bytes the program never sets in an interest transaction", "detail": "TRAN-DESC is filled by STRING, which does not pad, and the 20-byte FILLER is never moved to. Off-host both hold spaces. On the host they hold whatever working storage held at program start (INT-T04). The target writes spaces.", "affects": "CVTRA05Y TRAN-DESC (bytes 25-100) and FILLER", "owner": "mainframe application owner", "closes": "one SYSTRAN generation from the host in hex"},
    {"id": "G06", "title": "Key order of mixed type codes", "detail": "EBCDIC sorts letters before digits, the C collation sorts digits before letters. If one account has type codes of both kinds, the target reads its balances in a different order and the six-digit transaction counter is handed out differently.", "affects": "CVTRA01Y TRANCAT-TYPE-CD", "owner": "data owner", "closes": "trial-load profile: distinct TRANCAT-TYPE-CD values (fixtures: digits only)"},
    {"id": "G07", "title": "Text fields with bytes that are not characters", "detail": "A PIC X field holding low-values or packed data cannot be stored in CHAR. The loader rejects the record and the trial load fails; it does not substitute a character.", "affects": "every PIC X field ({textCount} fields)", "owner": "data owner", "closes": "trial-load reject count (expected 0) or a rule per field"},
    {"id": "G08", "title": "Accounts with more than one card", "detail": "The alternate index on XREF-ACCT-ID is not unique. Which record CBACT04C gets, or whether it stops with status 02, is not determined by the source (INT-H03). The target stops the run (policy switch multiCard = STOP) until the register row is answered.", "affects": "CVACT03Y XREF-ACCT-ID", "owner": "business owner", "closes": "INT-H03 answered; trial-load profile: number of accounts with several cards"},
    {"id": "G09", "title": "Category code is numeric here and CHAR(4) in the Db2 tables", "detail": "TRANCAT-CD, DIS-TRAN-CAT-CD and TRAN-CAT-CD are PIC 9(04) and map to NUMERIC(4,0). carddemo.transaction_type_categor.trc_type_category is CHAR(4) (db/postgres/01-transaction-type.sql). No foreign key is defined between them; the legacy files have none either.", "affects": "CVTRA01Y, CVTRA02Y, CVTRA05Y category code", "owner": "data architect", "closes": "a decision on one type for the category code across the schema"},
    {"id": "G10", "title": "Timestamps kept as text", "detail": "TRAN-ORIG-TS and TRAN-PROC-TS are PIC X(26) in the form YYYY-MM-DD-HH.MM.SS.hh0000, which is not a PostgreSQL timestamp literal. They stay CHAR(26) so the exported record is exact.", "affects": "CVTRA05Y, 2 fields", "owner": "data architect", "closes": "a decision when the transaction history itself moves"},
    {"id": "G11", "title": "Volumes are unknown", "detail": "The estate holds no record counts for the production files. Chunk size, load time and whether the whole job fits the window between CLOSEFIL and OPENFIL cannot be sized.", "affects": "all tables", "owner": "mainframe operations owner", "closes": "LISTCAT record counts of the four input files and the run time of INTCALC"},
    {"id": "G12", "title": "Which DD serves the read by account id", "detail": "CBACT04C reads XREF-FILE by its alternate key but never names DD XREFFIL1; the edge is a candidate (manifest D3, ask A5). The target reads card_xref through card_xref_acct_ix either way.", "affects": "CVACT03Y", "owner": "mainframe application owner", "closes": "stream ask A5"},
]

# ------------------------------------------------------------- components
components = [
    {"id": "interest-loader", "step": "loadExtractsStep", "role": "Decodes the four unloaded files and replaces the four replica tables. Exists only while the inputs are owned by the mainframe.", "writes": "account, card_xref, tran_cat_balance, disclosure_group (truncate and insert), interest_run (loaded counts and sums)", "reads": "extract files", "coexistenceOnly": True},
    {"id": "interest-calc", "step": "calcInterestStep", "role": "The replacement of CBACT04C: one item is one account with all its category balances; computes interest, writes the interest transactions, updates the account and writes the run log (SYSOUT).", "writes": "interest_transaction (insert), account (three columns), interest_run (written counts and sums), the run log file", "reads": "tran_cat_balance, account, card_xref, disclosure_group", "coexistenceOnly": False},
    {"id": "interest-export", "step": "exportDatasetsStep", "role": "Encodes the run's interest transactions as the SYSTRAN generation and the account table as the ACCTDATA image, with the return codes, in the recorded fixture format or the host format.", "writes": "output files only", "reads": "interest_transaction, account, interest_run", "coexistenceOnly": True},
    {"id": "interest-run-control", "step": "(listener on all steps)", "role": "Creates the interest_run row, records counts and sums after each step and the final status. Feeds the reconciliation report.", "writes": "interest_run", "reads": "-", "coexistenceOnly": False},
    {"id": "record-codec", "step": "(library)", "role": "Copybook-driven encoder and decoder for fixed-length records: zoned decimals with overpunch signs, text in the host or the fixture code page, raw fillers.", "writes": "-", "reads": "app/cpy/*.cpy (read-only)", "coexistenceOnly": True},
    {"id": "interest-policy", "step": "(configuration)", "role": "One switch per open decision of the register. The default of each switch is the legacy behaviour; the code that depends on a decision reads the switch and cites the decision id.", "writes": "-", "reads": "application properties", "coexistenceOnly": False},
]

policySwitches = [
    {"decision": "INT-Q01", "property": "interest.policy.rounding", "default": "TRUNCATE", "other": "HALF_UP, HALF_EVEN", "where": "interest-calc, amount calculation"},
    {"decision": "INT-Q02", "property": "interest.policy.idPrefix", "default": "PARM", "other": "RUN_DATE", "where": "interest-calc, transaction id"},
    {"decision": "INT-Q03", "property": "interest.policy.timestampSource", "default": "RUN_CLOCK", "other": "CYCLE_DATE", "where": "interest-calc, both timestamps"},
    {"decision": "INT-Q04", "property": "interest.policy.updateLastAccount", "default": "false", "other": "true", "where": "interest-calc, account break at end of input"},
    {"decision": "INT-Q05", "property": "interest.policy.negativeBalance", "default": "CHARGE", "other": "SKIP", "where": "interest-calc, per balance"},
    {"decision": "INT-Q06", "property": "interest.policy.zeroInterest", "default": "WRITE", "other": "SKIP", "where": "interest-calc, per balance"},
    {"decision": "INT-H01", "property": "(none - empty fee hook)", "default": "no fee", "other": "a fee rule from the business", "where": "interest-calc, after the interest of a balance"},
    {"decision": "INT-H02", "property": "interest.policy.rerunGuard", "default": "OFF", "other": "REFUSE_SAME_PARM", "where": "job launch"},
    {"decision": "INT-H03", "property": "interest.policy.multiCard", "default": "STOP (proposed - the source does not determine it)", "other": "FIRST_BY_CARD_NUM", "where": "interest-calc, card lookup"},
    {"decision": "INT-H04", "property": "interest.policy.inactiveAccounts", "default": "CHARGE", "other": "SKIP", "where": "interest-calc, per account"},
    {"decision": "INT-H05", "property": "interest.policy.overflow", "default": "STOP (proposed - the source does not determine it)", "other": "WIDEN", "where": "interest-calc, amount and counter"},
    {"decision": "INT-H06", "property": "(none - scheduler order)", "default": "the order of the Control-M folder as exported", "other": "another order stated by operations", "where": "outside the job"},
]

frozenInterfaces = [
    {"id": "FI-1", "what": "Record layouts", "where": "app/cpy/CVACT01Y.cpy, CVACT03Y.cpy, CVTRA01Y.cpy, CVTRA02Y.cpy, CVTRA05Y.cpy", "frozenBy": "the estate"},
    {"id": "FI-2", "what": "Recorded output format and comparator", "where": "fixtures/interest/<case>/expected (datasets/<DSN>.txt, sysout/INTCALC.STEP15.txt, rc.json, manifest.json) and tools/parity/compare.py", "frozenBy": "the estate"},
    {"id": "FI-3", "what": "Ports of the calculation", "where": "java/interest: package com.carddemo.interest.port (BalanceSource, AccountStore, CardLookup, RateLookup, TransactionSink, RunLog)", "frozenBy": "ticket 01, once merged"},
    {"id": "FI-4", "what": "Schema", "where": "db/postgres/02-interest.sql", "frozenBy": "ticket 02, once merged"},
    {"id": "FI-5", "what": "Policy switch names", "where": "architecture.md, section Policy switches (interest.policy.*)", "frozenBy": "architecture.md " + architectureVersion},
    {"id": "FI-6", "what": "Parity entry point", "where": "make parity-java [CASE=<case>], output under work/parity-java/<case>/actual", "frozenBy": "ticket 01, once merged"},
]

# ---------------------------------------------- release decisions (register)
releaseDecisions = [
    {"id": "REL-01", "question": "How does the new job share data with the jobs that stay on the mainframe?", "options": ["A file bridge at the dataset boundary: unload, load, run, export, write back (proposed)", "B replicate the VSAM files continuously into PostgreSQL", "C move every reader and writer of the four files first"], "proposed": "A", "owner": "enterprise architect"},
    {"id": "REL-02", "question": "Which copy of the account data is the system of record while POSTTRAN and the CICS programs stay?", "options": ["A the mainframe file ACCTDATA; the PostgreSQL table is a copy refreshed for each run (proposed)", "B the PostgreSQL table"], "proposed": "A", "owner": "data owner"},
    {"id": "REL-03", "question": "What is written back to ACCTDATA after a run?", "options": ["A the whole record of every account the run updated, while the CICS files are closed (proposed)", "B the whole file"], "proposed": "A", "owner": "mainframe application owner"},
    {"id": "REL-04", "question": "How many cycles run side by side before cutover?", "options": ["A two consecutive cycles with no unexplained difference (proposed)", "B another number"], "proposed": "A", "owner": "business owner"},
    {"id": "REL-05", "question": "What is the bar for 'equal' in the side-by-side run?", "options": ["A every field of every record of SYSTRAN and ACCTDATA equal, timestamps compared under the rule of SS3 (proposed)", "B totals only"], "proposed": "A", "owner": "business owner"},
    {"id": "REL-06", "question": "In which cycle does the new job take over?", "options": ["A the first cycle after REL-04 is met and every gate is green (proposed)", "B a fixed date"], "proposed": "A", "owner": "release manager"},
    {"id": "REL-07", "question": "Until which point can a cutover run be abandoned without a business correction?", "options": ["A until COMBTRAN starts; after that only a forward fix (proposed)", "B another point"], "proposed": "A", "owner": "mainframe operations owner"},
    {"id": "REL-08", "question": "Which code page and sign convention do the unloaded files use?", "options": ["A as the parity tool assumes: cp037, C/D signs (proposed, unconfirmed)", "B other, stated by the data owner"], "proposed": "A", "owner": "mainframe data owner"},
    {"id": "REL-09", "question": "Who starts the new job?", "options": ["A the existing Control-M folder; the INTCALC job definition is replaced and keeps its condition names (proposed)", "B a new scheduler"], "proposed": "A", "owner": "scheduler owner"},
    {"id": "REL-10", "question": "What does a failed run report to the scheduler?", "options": ["A a non-zero exit code mapped to NOTOK, with the legacy message text in the log (proposed)", "B a specific code agreed with operations in place of user abend 999"], "proposed": "A", "owner": "mainframe operations owner"},
    {"id": "REL-11", "question": "How is a run that failed part-way continued?", "options": ["A restart of the same run from the last committed account (proposed; the legacy procedure is INT-H02)", "B restore and rerun from the start, as on the mainframe"], "proposed": "A", "owner": "mainframe operations owner"},
    {"id": "REL-12", "question": "How long are a run's rows kept in interest_transaction and interest_run?", "options": ["A five runs, as GDG LIMIT(5) keeps five generations (proposed)", "B another retention"], "proposed": "A", "owner": "data owner"},
    {"id": "REL-13", "question": "How long does the legacy INTCALC job stay available as a fallback after cutover?", "options": ["A two cycles after the cutover cycle (proposed)", "B another period"], "proposed": "A", "owner": "mainframe application owner"},
]

# ----------------------------------------------------------------- move map
moveMap = [
    {"id": "M01", "legacy": "INTCALC.STEP15 running CBACT04C", "target": "Spring Batch job interestCalcJob, step calcInterestStep", "disposition": "MOVES", "owner": "engineering lead", "note": "652 lines of COBOL; rules INT-R01 to INT-R08, quirks as coded"},
    {"id": "M02", "legacy": "TCATBALF (category balances), read in key order", "target": "carddemo.tran_cat_balance, refreshed before each run", "disposition": "BRIDGED", "owner": "data owner", "note": "POSTTRAN keeps writing the file on the mainframe"},
    {"id": "M03", "legacy": "DISCGRP (interest rates per disclosure group)", "target": "carddemo.disclosure_group, refreshed before each run", "disposition": "BRIDGED", "owner": "data owner", "note": "job DISCGRP keeps reloading the file"},
    {"id": "M04", "legacy": "CARDXREF and its alternate index path", "target": "carddemo.card_xref with index card_xref_acct_ix", "disposition": "BRIDGED", "owner": "data owner", "note": "read only; {xrefUses} + {pathUses} outside-stream uses of the file and the path stay"},
    {"id": "M05", "legacy": "ACCTDATA, read and rewritten", "target": "carddemo.account; three columns updated and written back to the file", "disposition": "BRIDGED", "owner": "data owner", "note": "POSTTRAN and the CICS programs COACTUPC and COBIL00C also rewrite it"},
    {"id": "M06", "legacy": "SYSTRAN(+1), the new generation of interest transactions", "target": "carddemo.interest_transaction, exported as the generation COMBTRAN reads", "disposition": "BRIDGED", "owner": "mainframe application owner", "note": "COMBTRAN stays and reads SYSTRAN(0)"},
    {"id": "M07", "legacy": "PARM='2022071800' on the EXEC statement", "target": "job parameter parmDate", "disposition": "MOVES", "owner": "scheduler owner", "note": "who changes it per run is stream ask A2"},
    {"id": "M08", "legacy": "System clock (FUNCTION CURRENT-DATE)", "target": "an injected clock; frozen in tests", "disposition": "MOVES", "owner": "engineering lead", "note": "INT-Q03"},
    {"id": "M09", "legacy": "DISPLAY output on SYSOUT", "target": "run log written in the same lines", "disposition": "MOVES", "owner": "mainframe operations owner", "note": "where DISPLAY goes on the host is ask A10"},
    {"id": "M10", "legacy": "CEE3ABD user abend 999 on any file error", "target": "job status FAILED and a non-zero exit code", "disposition": "MOVES", "owner": "mainframe operations owner", "note": "INT-R08; the code is REL-10"},
    {"id": "M11", "legacy": "Control-M folder MONTHLY-InterestCalculation, job INTCALC", "target": "same folder; the job definition calls the new job", "disposition": "STAYS", "owner": "scheduler owner", "note": "REL-09"},
    {"id": "M12", "legacy": "CLOSEFIL, COMBTRAN, WAITSTEP, OPENFIL", "target": "unchanged", "disposition": "STAYS", "owner": "mainframe operations owner", "note": "predecessor and successors of the stream"},
    {"id": "M13", "legacy": "Setup jobs DEFGDGB, ACCTFILE, XREFFILE, TCATBALF, DISCGRP", "target": "unchanged", "disposition": "STAYS", "owner": "mainframe operations owner", "note": "define and reload the files"},
    {"id": "M14", "legacy": "Other users of the same datasets: POSTTRAN, CREASTMT, CBEXPORT, FEECALC, TRANREPT, READACCT, READXREF, PRTCATBL and the CICS programs", "target": "unchanged", "disposition": "STAYS", "owner": "mainframe application owner", "note": "the outside-stream rows of the manifest"},
]

# --------------------------------------------------------------- data moves
dataMoves = [
    {"phase": "trial", "title": "Trial load", "intro": "Copy the four input files into PostgreSQL once, outside any production run, to find data the mapping cannot hold.",
     "items": [
        {"id": "TL1", "text": "Unload TCATBALF, DISCGRP, CARDXREF and ACCTDATA to flat files with IDCAMS REPRO while the CICS files are closed, so the four files are from the same moment.", "owner": "mainframe operations owner"},
        {"id": "TL2", "text": "Transfer the files in binary with their record length (50, 50, 50, 300); no character translation on the way.", "owner": "mainframe operations owner"},
        {"id": "TL3", "text": "Load each file into its empty table in one transaction. A record that cannot be decoded goes to a reject list with file, record number and field; one reject fails the trial.", "owner": "data engineering lead"},
        {"id": "TL4", "text": "Profile what the mapping gaps ask for: accounts with several cards (G08), mixed type codes (G06), minus zero (G03), F-signed amounts (G02), fillers that are not blank, accounts that are not active (INT-H04), balances without a rate and without a DEFAULT rate (INT-R08).", "owner": "data owner"},
        {"id": "TL5", "text": "Run the trial on data approved for the environment; this repository holds synthetic data only.", "owner": "data owner"},
     ]},
    {"phase": "recon", "title": "Reconciliation counts and sums", "intro": "Figures that must be equal on both sides. Each is computed by the loader or the exporter and stored in interest_run.",
     "items": [
        {"id": "RC1", "text": "Row count of each of the four tables equals the record count of its unloaded file.", "owner": "data engineering lead"},
        {"id": "RC2", "text": "Sums equal on both sides: ACCT-CURR-BAL, ACCT-CREDIT-LIMIT, ACCT-CASH-CREDIT-LIMIT, ACCT-CURR-CYC-CREDIT, ACCT-CURR-CYC-DEBIT over accounts; TRAN-CAT-BAL over balances; DIS-INT-RATE over rates; and the sum of the account ids of each file as a hash total.", "owner": "data engineering lead"},
        {"id": "RC3", "text": "Round trip: every loaded row, encoded again, equals its source record byte for byte. Differences are counted per gap id (G02, G03); any other difference fails.", "owner": "data engineering lead"},
        {"id": "RC4", "text": "Keys: the number of distinct keys equals the row count in each table, and the lowest and highest key equal the first and last record of the file.", "owner": "data engineering lead"},
        {"id": "RC5", "text": "After the calculation: the number of rows written to interest_transaction equals the records in the exported SYSTRAN generation, and the sum of TRAN-AMT equals the change of the sum of ACCT-CURR-BAL plus the interest of the last account (which is not added, INT-Q04).", "owner": "business owner"},
        {"id": "RC6", "text": "After the write-back: ACCTDATA unloaded again equals the account table in count and in the five sums of RC2.", "owner": "mainframe operations owner"},
     ]},
    {"phase": "sidebyside", "title": "Side-by-side running", "intro": "Both jobs process the same inputs; only the mainframe result is used.",
     "items": [
        {"id": "SS1", "text": "Off-host first: make parity-java passes the seven fixture cases field by field (ticket 09).", "owner": "engineering lead"},
        {"id": "SS2", "text": "Shadow run per cycle: unload the inputs just before INTCALC, let INTCALC run as today, run the new job on the unload, and compare SYSTRAN, the ACCTDATA after-image and SYSOUT with the field-level comparator. The new job's output is not given to COMBTRAN.", "owner": "test lead"},
        {"id": "SS3", "text": "Timestamps: the two runs happen at different moments (INT-Q03), so TRAN-ORIG-TS and TRAN-PROC-TS are compared for format only, or the new job is given the mainframe run's clock. The rule used is written into the comparison report.", "owner": "test lead"},
        {"id": "SS4", "text": "Every difference is classified as a defect, an open decision (its id) or a mapping gap (its id). Exit needs REL-04 cycles with no defect and no unclassified difference.", "owner": "test lead"},
        {"id": "SS5", "text": "The number of cycles and the bar for 'equal' are release decisions REL-04 and REL-05.", "owner": "business owner"},
     ]},
    {"phase": "cutover", "title": "Cutover", "intro": "The cycle in which the new job's result is the one that is used.",
     "items": [
        {"id": "CO1", "text": "Preconditions: tickets 09 and 10 are closed, REL-01 to REL-13 are answered, and each row of the decision register is answered or accepted as coded.", "owner": "release manager"},
        {"id": "CO2", "text": "Order inside the window: CLOSEFIL, back up ACCTDATA, unload the four files, load and reconcile (RC1-RC4), calculate, export, reconcile (RC5), write back ACCTDATA and catalog the SYSTRAN generation, reconcile (RC6), COMBTRAN, WAITSTEP, OPENFIL.", "owner": "mainframe operations owner"},
        {"id": "CO3", "text": "Scheduler: the INTCALC definition in the folder is replaced by the job that starts the new run; it still adds the condition MONTHLY-InterestCalculation-INTCALC, so COMBTRAN is released as before.", "owner": "scheduler owner"},
        {"id": "CO4", "text": "Go or no-go is taken after RC5 and before the write-back. Until then nothing on the mainframe has changed.", "owner": "release manager"},
        {"id": "CO5", "text": "The legacy job and load module stay in place, unscheduled, for the period agreed in REL-13.", "owner": "mainframe application owner"},
     ]},
    {"phase": "fallback", "title": "Fallback", "intro": "What happens if the cutover run is not accepted, by the point reached.",
     "items": [
        {"id": "FB1", "text": "Before the write-back: abandon the new run and run the legacy INTCALC in the same window. Nothing on the mainframe needs restoring.", "owner": "mainframe operations owner"},
        {"id": "FB2", "text": "After the write-back and before COMBTRAN: restore ACCTDATA from the backup of CO2, delete the cataloged SYSTRAN generation, run the legacy INTCALC.", "owner": "mainframe operations owner"},
        {"id": "FB3", "text": "After COMBTRAN or OPENFIL: no technical fallback. Corrections are posted as transactions; that is a business decision (REL-07).", "owner": "business owner"},
        {"id": "FB4", "text": "FB1 and FB2 are rehearsed once during the side-by-side period.", "owner": "test lead"},
        {"id": "FB5", "text": "On the PostgreSQL side the run is marked ABANDONED in interest_run; the replica tables are replaced by the next load.", "owner": "engineering lead"},
     ]},
]

phaseOrder = ["trial", "recon", "sidebyside", "cutover", "fallback"]
