# CardDemo Application - Data Dictionary

> Comprehensive mapping of COBOL copybook layouts to sample data files, including field definitions, data types, relationships, and data quality observations.

---

## Table of Contents

1. [Overview](#overview)
2. [Entity Relationship Diagram](#entity-relationship-diagram)
3. [Entity: Account (acctdata.txt)](#entity-account)
4. [Entity: Customer (custdata.txt)](#entity-customer)
5. [Entity: Card (carddata.txt)](#entity-card)
6. [Entity: Card Cross-Reference (cardxref.txt)](#entity-card-cross-reference)
7. [Entity: Daily Transaction (dailytran.txt)](#entity-daily-transaction)
8. [Entity: Transaction Type (trantype.txt)](#entity-transaction-type)
9. [Entity: Transaction Category (trancatg.txt)](#entity-transaction-category)
10. [Entity: Transaction Category Balance (tcatbal.txt)](#entity-transaction-category-balance)
11. [Entity: Disclosure Group (discgrp.txt)](#entity-disclosure-group)
12. [Supporting Copybooks](#supporting-copybooks)
13. [COBOL Numeric Encoding Notes](#cobol-numeric-encoding-notes)
14. [Data Quality Issues](#data-quality-issues)

---

## Overview

The CardDemo application is an AWS Mainframe Modernization reference application simulating a credit card management system. It manages customer accounts, credit cards, and financial transactions through VSAM datasets defined by COBOL copybooks.

### File Summary

| Data File | Copybook | Record Length | Record Count | Description |
|---|---|---|---|---|
| `acctdata.txt` | `CVACT01Y.cpy` | 300 | 50 | Credit card accounts |
| `custdata.txt` | `CVCUS01Y.cpy` / `CUSTREC.cpy` | 500 | 50 | Customer master records |
| `carddata.txt` | `CVACT02Y.cpy` | 150 | 50 | Credit card details |
| `cardxref.txt` | `CVACT03Y.cpy` | 50 (padded to 36) | 50 | Card-to-customer-account cross-reference |
| `dailytran.txt` | `CVTRA06Y.cpy` | 350 | 300 | Daily transaction feed |
| `trantype.txt` | `CVTRA03Y.cpy` | 60 | 7 | Transaction type lookup |
| `trancatg.txt` | `CVTRA04Y.cpy` | 60 | 18 | Transaction category lookup |
| `tcatbal.txt` | `CVTRA01Y.cpy` | 50 | 50 | Transaction category balances per account |
| `discgrp.txt` | `CVTRA02Y.cpy` | 50 | 51 | Disclosure group interest rates |

---

## Entity Relationship Diagram

```
                    +------------------+
                    |    CUSTOMER      |
                    |  (custdata.txt)  |
                    |  CVCUS01Y.cpy   |
                    +--------+---------+
                             |
                             | 1:N  (CUST-ID)
                             |
                    +--------+---------+
                    | CARD CROSS-REF   |
                    | (cardxref.txt)   |
                    |  CVACT03Y.cpy    |
                    +--+----------+----+
                       |          |
          CARD-NUM     |          |  ACCT-ID
          (1:1)        |          |  (N:1)
                       |          |
              +--------+--+   +---+-----------+        +------------------+
              |    CARD    |   |   ACCOUNT     |------->| DISCLOSURE GROUP |
              |(carddata)  |   | (acctdata)    | GRP-ID | (discgrp.txt)   |
              | CVACT02Y   |   |  CVACT01Y     |        |  CVTRA02Y.cpy   |
              +------------+   +---+-----------+        +------------------+
                                   |
                                   | ACCT-ID (1:N)
                                   |
                          +--------+-----------+
                          | TRAN CAT BALANCE   |
                          |  (tcatbal.txt)     |
                          |   CVTRA01Y.cpy     |
                          +--------------------+

              +-------------------+
              | DAILY TRANSACTION |       +------------------+      +-------------------+
              | (dailytran.txt)   |------>| TRANSACTION TYPE |      | TRANSACTION CAT   |
              |  CVTRA06Y.cpy    |  TYPE  | (trantype.txt)   |      | (trancatg.txt)    |
              +---------+---------+  CD   |  CVTRA03Y.cpy    |      |  CVTRA04Y.cpy     |
                        |                 +------------------+      +-------------------+
                        |                         ^                        ^
                        |  CARD-NUM               |  TYPE-CD               |  TYPE+CAT CD
                        +--- links to CARD -------+--- joins to -----------+
```

### Key Relationships

| Relationship | From Entity | To Entity | Join Key(s) | Cardinality |
|---|---|---|---|---|
| Customer owns Cards | Customer | Card Cross-Ref | `CUST-ID` = `XREF-CUST-ID` | 1:N |
| Account has Cards | Account | Card Cross-Ref | `ACCT-ID` = `XREF-ACCT-ID` | 1:N |
| Card belongs to Account | Card | Card Cross-Ref | `CARD-NUM` = `XREF-CARD-NUM` | 1:1 |
| Card also in Card Detail | Card Cross-Ref | Card | `XREF-CARD-NUM` = `CARD-NUM` | 1:1 |
| Account has Card Detail | Card | Account | `CARD-ACCT-ID` = `ACCT-ID` | N:1 |
| Transaction uses Card | Daily Transaction | Card | `DALYTRAN-CARD-NUM` = `CARD-NUM` | N:1 |
| Transaction has Type | Daily Transaction | Transaction Type | `DALYTRAN-TYPE-CD` = `TRAN-TYPE` | N:1 |
| Transaction has Category | Daily Transaction | Transaction Category | `DALYTRAN-TYPE-CD`+`DALYTRAN-CAT-CD` = `TRAN-TYPE-CD`+`TRAN-CAT-CD` | N:1 |
| Account has Category Balances | Account | Tran Cat Balance | `ACCT-ID` = `TRANCAT-ACCT-ID` | 1:N |
| Account belongs to Disc Group | Account | Disclosure Group | `ACCT-GROUP-ID` = `DIS-ACCT-GROUP-ID` | N:1 |

---

## Entity: Account

**Data File:** `app/data/ASCII/acctdata.txt`
**Copybook:** `app/cpy/CVACT01Y.cpy`
**Record Name:** `ACCOUNT-RECORD`
**Record Length:** 300 bytes
**Record Count:** 50

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `ACCT-ID` | `9(11)` | Numeric (zoned decimal) | 1 | 11 | Account identifier (primary key) |
| 2 | `ACCT-ACTIVE-STATUS` | `X(01)` | Alphanumeric | 12 | 1 | Account active status flag |
| 3 | `ACCT-CURR-BAL` | `S9(10)V99` | Signed numeric with 2 implied decimals | 13 | 12 | Current account balance |
| 4 | `ACCT-CREDIT-LIMIT` | `S9(10)V99` | Signed numeric with 2 implied decimals | 25 | 12 | Credit limit |
| 5 | `ACCT-CASH-CREDIT-LIMIT` | `S9(10)V99` | Signed numeric with 2 implied decimals | 37 | 12 | Cash advance credit limit |
| 6 | `ACCT-OPEN-DATE` | `X(10)` | Alphanumeric (date) | 49 | 10 | Account open date (YYYY-MM-DD) |
| 7 | `ACCT-EXPIRAION-DATE` | `X(10)` | Alphanumeric (date) | 59 | 10 | Account expiration date (YYYY-MM-DD) |
| 8 | `ACCT-REISSUE-DATE` | `X(10)` | Alphanumeric (date) | 69 | 10 | Card reissue date (YYYY-MM-DD) |
| 9 | `ACCT-CURR-CYC-CREDIT` | `S9(10)V99` | Signed numeric with 2 implied decimals | 79 | 12 | Current cycle credits |
| 10 | `ACCT-CURR-CYC-DEBIT` | `S9(10)V99` | Signed numeric with 2 implied decimals | 91 | 12 | Current cycle debits |
| 11 | `ACCT-ADDR-ZIP` | `X(10)` | Alphanumeric | 103 | 10 | Account billing ZIP code |
| 12 | `ACCT-GROUP-ID` | `X(10)` | Alphanumeric | 113 | 10 | Disclosure/rate group ID |
| 13 | `FILLER` | `X(178)` | Filler | 123 | 178 | Reserved space |

### Sample Data Mapping (Record 1)

```
Raw: 00000000001Y00000001940{00000020200{00000010200{2014-11-202025-05-202025-05-2000000000000{00000000000{A000000000
```

| Field | Raw Value | Decoded Value | Business Meaning |
|---|---|---|---|
| `ACCT-ID` | `00000000001` | 1 | Account #1 |
| `ACCT-ACTIVE-STATUS` | `Y` | Active | Account is active |
| `ACCT-CURR-BAL` | `00000001940{` | +$194.00 | Current balance is $194.00 (`{` = positive sign, +0) |
| `ACCT-CREDIT-LIMIT` | `00000020200{` | +$2,020.00 | Credit limit $2,020.00 |
| `ACCT-CASH-CREDIT-LIMIT` | `00000010200{` | +$1,020.00 | Cash advance limit $1,020.00 |
| `ACCT-OPEN-DATE` | `2014-11-20` | Nov 20, 2014 | Account opened |
| `ACCT-EXPIRAION-DATE` | `2025-05-20` | May 20, 2025 | Expiration date |
| `ACCT-REISSUE-DATE` | `2025-05-20` | May 20, 2025 | Last reissue date |
| `ACCT-CURR-CYC-CREDIT` | `00000000000{` | $0.00 | No credits this cycle |
| `ACCT-CURR-CYC-DEBIT` | `00000000000{` | $0.00 | No debits this cycle |
| `ACCT-ADDR-ZIP` | `A000000000` | A000000000 | Billing ZIP / group identifier |
| `ACCT-GROUP-ID` | (spaces in filler region) | (see notes) | Disclosure rate group |

### Value Ranges Observed

| Field | Min | Max | Notes |
|---|---|---|---|
| `ACCT-ID` | 00000000001 | 00000000050 | Sequential 1-50 |
| `ACCT-ACTIVE-STATUS` | Y | Y | All accounts active |
| `ACCT-CURR-BAL` | $2.00 (acct 30) | $843.00 (acct 39) | All positive |
| `ACCT-CREDIT-LIMIT` | $120.00 (acct 30) | $9,750.00 (acct 39) | Wide range |
| `ACCT-OPEN-DATE` | 2009-04-20 | 2019-04-06 | 10-year spread |
| `ACCT-EXPIRAION-DATE` | 2023-01-06 | 2025-12-28 | Some already expired |

---

## Entity: Customer

**Data File:** `app/data/ASCII/custdata.txt`
**Copybooks:** `app/cpy/CVCUS01Y.cpy` (primary) and `app/cpy/CUSTREC.cpy` (alternate)
**Record Name:** `CUSTOMER-RECORD`
**Record Length:** 500 bytes
**Record Count:** 50

> **Note:** `CVCUS01Y.cpy` and `CUSTREC.cpy` are nearly identical. The only difference is the DOB field name: `CUST-DOB-YYYY-MM-DD` vs `CUST-DOB-YYYYMMDD`. Both define the same 500-byte layout.

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `CUST-ID` | `9(09)` | Numeric (zoned decimal) | 1 | 9 | Customer identifier (primary key) |
| 2 | `CUST-FIRST-NAME` | `X(25)` | Alphanumeric | 10 | 25 | First name |
| 3 | `CUST-MIDDLE-NAME` | `X(25)` | Alphanumeric | 35 | 25 | Middle name |
| 4 | `CUST-LAST-NAME` | `X(25)` | Alphanumeric | 60 | 25 | Last name |
| 5 | `CUST-ADDR-LINE-1` | `X(50)` | Alphanumeric | 85 | 50 | Address line 1 |
| 6 | `CUST-ADDR-LINE-2` | `X(50)` | Alphanumeric | 135 | 50 | Address line 2 (apt/suite) |
| 7 | `CUST-ADDR-LINE-3` | `X(50)` | Alphanumeric | 185 | 50 | Address line 3 (city) |
| 8 | `CUST-ADDR-STATE-CD` | `X(02)` | Alphanumeric | 235 | 2 | US state/territory code |
| 9 | `CUST-ADDR-COUNTRY-CD` | `X(03)` | Alphanumeric | 237 | 3 | Country code |
| 10 | `CUST-ADDR-ZIP` | `X(10)` | Alphanumeric | 240 | 10 | ZIP code (5 or 5+4 format) |
| 11 | `CUST-PHONE-NUM-1` | `X(15)` | Alphanumeric | 250 | 15 | Primary phone |
| 12 | `CUST-PHONE-NUM-2` | `X(15)` | Alphanumeric | 265 | 15 | Secondary phone |
| 13 | `CUST-SSN` | `9(09)` | Numeric (zoned decimal) | 280 | 9 | Social Security Number |
| 14 | `CUST-GOVT-ISSUED-ID` | `X(20)` | Alphanumeric | 289 | 20 | Government-issued ID number |
| 15 | `CUST-DOB-YYYY-MM-DD` | `X(10)` | Alphanumeric (date) | 309 | 10 | Date of birth (YYYY-MM-DD) |
| 16 | `CUST-EFT-ACCOUNT-ID` | `X(10)` | Alphanumeric | 319 | 10 | EFT/bank account for payments |
| 17 | `CUST-PRI-CARD-HOLDER-IND` | `X(01)` | Alphanumeric | 329 | 1 | Primary cardholder indicator |
| 18 | `CUST-FICO-CREDIT-SCORE` | `9(03)` | Numeric (zoned decimal) | 330 | 3 | FICO credit score |
| 19 | `FILLER` | `X(168)` | Filler | 333 | 168 | Reserved space |

### Sample Data Mapping (Record 1 - Customer 000000001)

| Field | Raw Value | Decoded Value | Business Meaning |
|---|---|---|---|
| `CUST-ID` | `000000001` | 1 | Customer #1 |
| `CUST-FIRST-NAME` | `Immanuel` | Immanuel | First name |
| `CUST-MIDDLE-NAME` | `Madeline` | Madeline | Middle name |
| `CUST-LAST-NAME` | `Kessler` | Kessler | Last name |
| `CUST-ADDR-LINE-1` | `618 Deshaun Route` | 618 Deshaun Route | Street address |
| `CUST-ADDR-LINE-2` | `Apt. 802` | Apt. 802 | Apartment |
| `CUST-ADDR-LINE-3` | `Altenwerthshire` | Altenwerthshire | City |
| `CUST-ADDR-STATE-CD` | `NC` | North Carolina | State |
| `CUST-ADDR-COUNTRY-CD` | `USA` | United States | Country |
| `CUST-ADDR-ZIP` | `12546` | 12546 | ZIP code (5-digit) |
| `CUST-PHONE-NUM-1` | `(908)119-8310` | (908) 119-8310 | Primary phone |
| `CUST-PHONE-NUM-2` | `(373)693-8684` | (373) 693-8684 | Secondary phone |
| `CUST-SSN` | `020973888` | 020-97-3888 | SSN |
| `CUST-GOVT-ISSUED-ID` | `00000000000493684371` | 493684371 | Government ID (leading zeros) |
| `CUST-DOB-YYYY-MM-DD` | `1961-06-08` | June 8, 1961 | Date of birth |
| `CUST-EFT-ACCOUNT-ID` | `0053581756` | 0053581756 | EFT bank account |
| `CUST-PRI-CARD-HOLDER-IND` | `Y` | Yes | Primary cardholder |
| `CUST-FICO-CREDIT-SCORE` | `274` | 274 | FICO score (very poor) |

### Value Ranges Observed

| Field | Sample Values | Notes |
|---|---|---|
| `CUST-ID` | 000000001 - 000000050 | Sequential 1-50 |
| `CUST-ADDR-STATE-CD` | NC, IN, GA, MI, VI, AL, SC, etc. | Includes US territories (VI, GU, AS, PW, FM, MH, AP) |
| `CUST-ADDR-COUNTRY-CD` | `USA` (all records) | Only USA customers |
| `CUST-PRI-CARD-HOLDER-IND` | `Y` (all records) | All are primary cardholders |
| `CUST-FICO-CREDIT-SCORE` | 001 - 793 | Wide range, some unrealistically low |
| `CUST-DOB-YYYY-MM-DD` | 1960-12-01 to 2001-12-12 | Ages roughly 24-65 at time of data |

---

## Entity: Card

**Data File:** `app/data/ASCII/carddata.txt`
**Copybook:** `app/cpy/CVACT02Y.cpy`
**Record Name:** `CARD-RECORD`
**Record Length:** 150 bytes
**Record Count:** 50

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `CARD-NUM` | `X(16)` | Alphanumeric | 1 | 16 | Card number (16-digit) |
| 2 | `CARD-ACCT-ID` | `9(11)` | Numeric (zoned decimal) | 17 | 11 | Associated account ID (FK) |
| 3 | `CARD-CVV-CD` | `9(03)` | Numeric (zoned decimal) | 28 | 3 | CVV security code |
| 4 | `CARD-EMBOSSED-NAME` | `X(50)` | Alphanumeric | 31 | 50 | Name embossed on card |
| 5 | `CARD-EXPIRAION-DATE` | `X(10)` | Alphanumeric (date) | 81 | 10 | Card expiration date (YYYY-MM-DD) |
| 6 | `CARD-ACTIVE-STATUS` | `X(01)` | Alphanumeric | 91 | 1 | Card active status |
| 7 | `FILLER` | `X(59)` | Filler | 92 | 59 | Reserved space |

### Sample Data Mapping (Record 1)

```
Raw: 050002445376574000000000050747Aniya Von                                         2023-03-09Y
```

| Field | Raw Value | Decoded Value | Business Meaning |
|---|---|---|---|
| `CARD-NUM` | `0500024453765740` | 0500024453765740 | 16-digit card number |
| `CARD-ACCT-ID` | `00000000050` | 50 | Linked to Account #50 |
| `CARD-CVV-CD` | `747` | 747 | CVV security code |
| `CARD-EMBOSSED-NAME` | `Aniya Von` | Aniya Von | Cardholder name on card |
| `CARD-EXPIRAION-DATE` | `2023-03-09` | Mar 9, 2023 | Card expiration |
| `CARD-ACTIVE-STATUS` | `Y` | Active | Card is active |

### Value Ranges Observed

| Field | Min | Max | Notes |
|---|---|---|---|
| `CARD-NUM` | 0500024453765740 | 9805583408996588 | 16-digit numbers |
| `CARD-ACCT-ID` | 00000000001 | 00000000050 | Maps to all 50 accounts |
| `CARD-CVV-CD` | Various 3-digit | Various 3-digit | All 3-digit CVVs |
| `CARD-ACTIVE-STATUS` | Y | Y | All cards active |

---

## Entity: Card Cross-Reference

**Data File:** `app/data/ASCII/cardxref.txt`
**Copybook:** `app/cpy/CVACT03Y.cpy`
**Record Name:** `CARD-XREF-RECORD`
**Record Length:** 50 bytes (data lines are 36 chars + filler)
**Record Count:** 50

This is the **central linking entity** that ties cards to customers and accounts.

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `XREF-CARD-NUM` | `X(16)` | Alphanumeric | 1 | 16 | Card number (FK to Card) |
| 2 | `XREF-CUST-ID` | `9(09)` | Numeric (zoned decimal) | 17 | 9 | Customer ID (FK to Customer) |
| 3 | `XREF-ACCT-ID` | `9(11)` | Numeric (zoned decimal) | 26 | 11 | Account ID (FK to Account) |
| 4 | `FILLER` | `X(14)` | Filler | 37 | 14 | Reserved space |

### Sample Data Mapping (Record 1)

```
Raw: 050002445376574000000005000000000050
```

| Field | Raw Value | Decoded Value | Business Meaning |
|---|---|---|---|
| `XREF-CARD-NUM` | `0500024453765740` | 0500024453765740 | Card number |
| `XREF-CUST-ID` | `000000050` | 50 | Customer #50 (Aniya Von) |
| `XREF-ACCT-ID` | `00000000050` | 50 | Account #50 |

### Cross-Reference Verification

The cross-reference confirms a 1:1:1 mapping in the sample data:
- Each card maps to exactly one customer and one account
- Customer ID and Account ID are numerically equal in this dataset (Customer #N = Account #N)
- Card numbers in `cardxref.txt` match exactly with `carddata.txt`

---

## Entity: Daily Transaction

**Data File:** `app/data/ASCII/dailytran.txt`
**Copybook:** `app/cpy/CVTRA06Y.cpy`
**Record Name:** `DALYTRAN-RECORD`
**Record Length:** 350 bytes
**Record Count:** 300

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `DALYTRAN-ID` | `X(16)` | Alphanumeric | 1 | 16 | Transaction ID |
| 2 | `DALYTRAN-TYPE-CD` | `X(02)` | Alphanumeric | 17 | 2 | Transaction type code (FK to trantype) |
| 3 | `DALYTRAN-CAT-CD` | `9(04)` | Numeric (zoned decimal) | 19 | 4 | Transaction category code |
| 4 | `DALYTRAN-SOURCE` | `X(10)` | Alphanumeric | 23 | 10 | Transaction source/channel |
| 5 | `DALYTRAN-DESC` | `X(100)` | Alphanumeric | 33 | 100 | Transaction description |
| 6 | `DALYTRAN-AMT` | `S9(09)V99` | Signed numeric with 2 implied decimals | 133 | 11 | Transaction amount |
| 7 | `DALYTRAN-MERCHANT-ID` | `9(09)` | Numeric (zoned decimal) | 144 | 9 | Merchant identifier |
| 8 | `DALYTRAN-MERCHANT-NAME` | `X(50)` | Alphanumeric | 153 | 50 | Merchant name |
| 9 | `DALYTRAN-MERCHANT-CITY` | `X(50)` | Alphanumeric | 203 | 50 | Merchant city |
| 10 | `DALYTRAN-MERCHANT-ZIP` | `X(10)` | Alphanumeric | 253 | 10 | Merchant ZIP code |
| 11 | `DALYTRAN-CARD-NUM` | `X(16)` | Alphanumeric | 263 | 16 | Card number used (FK to Card) |
| 12 | `DALYTRAN-ORIG-TS` | `X(26)` | Alphanumeric (timestamp) | 279 | 26 | Original transaction timestamp |
| 13 | `DALYTRAN-PROC-TS` | `X(26)` | Alphanumeric (timestamp) | 305 | 26 | Processing timestamp |
| 14 | `FILLER` | `X(20)` | Filler | 331 | 20 | Reserved space |

### Sample Data Mapping (Record 1)

```
Raw: 0000000000683580010001POS TERM  Purchase at Abshire-Lowe...0000005047G800000000Abshire-Lowe...
```

| Field | Raw Value | Decoded Value | Business Meaning |
|---|---|---|---|
| `DALYTRAN-ID` | `0000000000683580` | 683580 | Transaction ID |
| `DALYTRAN-TYPE-CD` | `01` | Purchase (01) | Purchase transaction |
| `DALYTRAN-CAT-CD` | `0001` | 0001 | Regular Sales Draft |
| `DALYTRAN-SOURCE` | `POS TERM` | POS Terminal | Point-of-sale terminal |
| `DALYTRAN-DESC` | `Purchase at Abshire-Lowe` | Purchase description | Merchant purchase |
| `DALYTRAN-AMT` | `0000005047G` | +$504.77 (`G` = +7) | Transaction amount |
| `DALYTRAN-MERCHANT-ID` | `800000000` | 800000000 | Merchant ID |
| `DALYTRAN-MERCHANT-NAME` | `Abshire-Lowe` | Abshire-Lowe | Merchant name |
| `DALYTRAN-MERCHANT-CITY` | `North Enoshaven` | North Enoshaven | Merchant city |
| `DALYTRAN-MERCHANT-ZIP` | `72112` | 72112 | Merchant ZIP |
| `DALYTRAN-CARD-NUM` | `4859452612877065` | 4859452612877065 | Card used |
| `DALYTRAN-ORIG-TS` | `2022-06-10 19:27:53.000000` | Jun 10, 2022 19:27:53 | Original timestamp |
| `DALYTRAN-PROC-TS` | (spaces) | Not yet processed | Processing timestamp |

### Transaction Types in Sample Data

| Type Code | Source | Description Pattern | Count (approx) |
|---|---|---|---|
| `01` | `POS TERM` | Purchase at [Merchant] | ~250 |
| `03` | `OPERATOR` | Return item at [Merchant] | ~50 |

### Amount Encoding

Transaction amounts use COBOL zoned decimal with sign overpunch on the last digit:

| Last Char | Digit | Sign | Example |
|---|---|---|---|
| `{` | 0 | + | `0000002740{` = +$274.00 |
| `A` | 1 | + | `0000004161A` = +$416.11 |
| `B` | 2 | + | `0000002502B` = +$250.22 |
| `C` | 3 | + | `0000000943C` = +$94.33 |
| `D` | 4 | + | `0000000294D` = +$29.44 |
| `E` | 5 | + | `0000008295E` = +$829.55 |
| `F` | 6 | + | `0000000678F` = +$67.86 |
| `G` | 7 | + | `0000005047G` = +$504.77 |
| `H` | 8 | + | `0000000678H` = +$67.88 |
| `I` | 9 | + | `0000008499I` = +$849.99 |
| `}` | 0 | - | `0000009190}` = -$919.00 |
| `J`-`R` | 1-9 | - | Negative amounts (returns) |

---

## Entity: Transaction Type

**Data File:** `app/data/ASCII/trantype.txt`
**Copybook:** `app/cpy/CVTRA03Y.cpy`
**Record Name:** `TRAN-TYPE-RECORD`
**Record Length:** 60 bytes
**Record Count:** 7

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `TRAN-TYPE` | `X(02)` | Alphanumeric | 1 | 2 | Transaction type code (PK) |
| 2 | `TRAN-TYPE-DESC` | `X(50)` | Alphanumeric | 3 | 50 | Description |
| 3 | `FILLER` | `X(08)` | Filler | 53 | 8 | Reserved (all zeros) |

### Complete Data

| Type Code | Description | Business Meaning |
|---|---|---|
| `01` | Purchase | Standard purchase transaction |
| `02` | Payment | Payment received from customer |
| `03` | Credit | Credit applied to account |
| `04` | Authorization | Pre-authorization hold |
| `05` | Refund | Refund to customer |
| `06` | Reversal | Transaction reversal (fraud/non-fraud) |
| `07` | Adjustment | Manual adjustment to account |

---

## Entity: Transaction Category

**Data File:** `app/data/ASCII/trancatg.txt`
**Copybook:** `app/cpy/CVTRA04Y.cpy`
**Record Name:** `TRAN-CAT-RECORD`
**Record Length:** 60 bytes
**Record Count:** 18

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `TRAN-TYPE-CD` | `X(02)` | Alphanumeric | 1 | 2 | Transaction type code (FK, composite PK part 1) |
| 2 | `TRAN-CAT-CD` | `9(04)` | Numeric (zoned decimal) | 3 | 4 | Category code (composite PK part 2) |
| 3 | `TRAN-CAT-TYPE-DESC` | `X(50)` | Alphanumeric | 7 | 50 | Category description |
| 4 | `FILLER` | `X(04)` | Filler | 57 | 4 | Reserved |

### Complete Data

| Type | Category | Description | Business Meaning |
|---|---|---|---|
| 01 | 0001 | Regular Sales Draft | Standard POS purchase |
| 01 | 0002 | Regular Cash Advance | Cash withdrawal against credit |
| 01 | 0003 | Convenience Check Debit | Check drawn on credit line |
| 01 | 0004 | ATM Cash Advance | ATM cash withdrawal |
| 01 | 0005 | Interest Amount | Interest charges |
| 02 | 0001 | Cash payment | Cash payment received |
| 02 | 0002 | Electronic payment | EFT/online payment |
| 02 | 0003 | Check payment | Check payment received |
| 03 | 0001 | Credit to Account | General account credit |
| 03 | 0002 | Credit to Purchase balance | Credit against purchases |
| 03 | 0003 | Credit to Cash balance | Credit against cash advances |
| 04 | 0001 | Zero dollar authorization | $0 auth for card validation |
| 04 | 0002 | Online purchase authorization | E-commerce pre-auth |
| 04 | 0003 | Travel booking authorization | Travel pre-auth hold |
| 05 | 0001 | Refund credit | Merchant-initiated refund |
| 06 | 0001 | Fraud reversal | Reversal due to fraud |
| 06 | 0002 | Non-fraud reversal | Reversal for other reasons |
| 07 | 0001 | Sales draft credit adjustment | Manual balance adjustment |

---

## Entity: Transaction Category Balance

**Data File:** `app/data/ASCII/tcatbal.txt`
**Copybook:** `app/cpy/CVTRA01Y.cpy`
**Record Name:** `TRAN-CAT-BAL-RECORD`
**Record Length:** 50 bytes
**Record Count:** 50

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `TRANCAT-ACCT-ID` | `9(11)` | Numeric (zoned decimal) | 1 | 11 | Account ID (composite PK part 1, FK) |
| 2 | `TRANCAT-TYPE-CD` | `X(02)` | Alphanumeric | 12 | 2 | Transaction type code (composite PK part 2) |
| 3 | `TRANCAT-CD` | `9(04)` | Numeric (zoned decimal) | 14 | 4 | Category code (composite PK part 3) |
| 4 | `TRAN-CAT-BAL` | `S9(09)V99` | Signed numeric with 2 implied decimals | 18 | 11 | Balance for this category |
| 5 | `FILLER` | `X(22)` | Filler | 29 | 22 | Reserved space |

### Sample Data Mapping (Record 1)

```
Raw: 000000000010100010000000000{0000000000000000000000
```

| Field | Raw Value | Decoded Value | Business Meaning |
|---|---|---|---|
| `TRANCAT-ACCT-ID` | `00000000001` | 1 | Account #1 |
| `TRANCAT-TYPE-CD` | `01` | Purchase | Purchase type |
| `TRANCAT-CD` | `0001` | Regular Sales Draft | Sales draft category |
| `TRAN-CAT-BAL` | `0000000000{` | $0.00 | Zero balance |

### Observations

- All 50 records follow the pattern: Account 1-50, Type `01`, Category `0001`
- All balances are `$0.00` (initialized but not yet populated with transaction totals)
- Only one type/category combination per account exists in the sample data

---

## Entity: Disclosure Group

**Data File:** `app/data/ASCII/discgrp.txt`
**Copybook:** `app/cpy/CVTRA02Y.cpy`
**Record Name:** `DIS-GROUP-RECORD`
**Record Length:** 50 bytes
**Record Count:** 51

### Field Layout

| # | Field Name | COBOL PIC | Type | Offset | Length | Description |
|---|---|---|---|---|---|---|
| 1 | `DIS-ACCT-GROUP-ID` | `X(10)` | Alphanumeric | 1 | 10 | Account group identifier (composite PK part 1) |
| 2 | `DIS-TRAN-TYPE-CD` | `X(02)` | Alphanumeric | 11 | 2 | Transaction type code (composite PK part 2) |
| 3 | `DIS-TRAN-CAT-CD` | `9(04)` | Numeric (zoned decimal) | 13 | 4 | Category code (composite PK part 3) |
| 4 | `DIS-INT-RATE` | `S9(04)V99` | Signed numeric with 2 implied decimals | 17 | 6 | Interest rate (e.g., 00150{ = 1.50%) |
| 5 | `FILLER` | `X(28)` | Filler | 23 | 28 | Reserved space |

### Sample Data Mapping (Record 1)

```
Raw: A00000000001000100150{0000000000000000000000000000
```

| Field | Raw Value | Decoded Value | Business Meaning |
|---|---|---|---|
| `DIS-ACCT-GROUP-ID` | `A000000000` | Group A | Account group A |
| `DIS-TRAN-TYPE-CD` | `01` | Purchase | Purchase transactions |
| `DIS-TRAN-CAT-CD` | `0001` | Regular Sales Draft | Sales draft category |
| `DIS-INT-RATE` | `00150{` | 1.50% | Interest rate for this combo |

### Disclosure Groups in Sample Data

| Group ID | Description | Rate Pattern |
|---|---|---|
| `A000000000` | Group A (17 entries) | Purchases: 1.50-2.50%; Payments/Credits: 0%; Auth: 1.50%; Refund: 1.50%; Reversal: 1.50% |
| `DEFAULT` | Default group (17 entries) | Same rate structure as Group A |
| `ZEROAPR` | Zero APR promotional (17 entries) | All rates are 0.00% |

---

## Supporting Copybooks

### CVTRA05Y.cpy - Transaction Record (Processed)

Identical layout to `CVTRA06Y.cpy` (Daily Transaction) but used for the processed/historical transaction file rather than the daily feed. Field prefix is `TRAN-` instead of `DALYTRAN-`.

### CVTRA07Y.cpy - Transaction Report Layout

Defines the print layout for the Daily Transaction Report (`DALYREPT`), including:
- Report headers with date range
- Transaction detail line format
- Page totals, account totals, and grand totals
- Not a data storage layout; used for report generation only

### CVEXPORT.cpy - Multi-Record Export Layout

Defines a 500-byte export record with a `REDEFINES` structure supporting multiple entity types:
- `EXPORT-REC-TYPE`: Record type discriminator (1 byte)
- Includes `EXPORT-CUSTOMER-DATA`, `EXPORT-ACCOUNT-DATA`, `EXPORT-TRANSACTION-DATA`, `EXPORT-CARD-XREF-DATA`, `EXPORT-CARD-DATA`
- Uses `COMP` and `COMP-3` packed fields for storage optimization in export files
- Includes `EXPORT-BRANCH-ID` and `EXPORT-REGION-CODE` for branch migration scenarios

### CVCRD01Y.cpy - Card Work Areas

Defines working storage for CICS screen interactions, not a data file layout. Contains:
- `CCARD-AID`: Attention identifier for function keys (ENTER, CLEAR, PF1-PF12)
- `CCARD-NEXT-PROG`, `CCARD-NEXT-MAPSET`, `CCARD-NEXT-MAP`: Navigation control
- `CC-ACCT-ID`, `CC-CARD-NUM`, `CC-CUST-ID`: Working storage for current entity IDs
- Error and return message areas

### COCOM01Y.cpy - Communication Area

Defines the CICS COMMAREA structure for passing data between programs:
- User session info (`CDEMO-USER-ID`, `CDEMO-USER-TYPE`)
- Current customer/account/card context
- Map navigation state

### COADM02Y.cpy - Admin Menu Options

Defines the admin menu with 6 options:
1. User List (Security)
2. User Add (Security)
3. User Update (Security)
4. User Delete (Security)
5. Transaction Type List/Update (Db2)
6. Transaction Type Maintenance (Db2)

### UNUSED1Y.cpy - Unused/Legacy Layout

An 80-byte user security record layout that appears to be deprecated. Fields include user ID, name, password, and type.

---

## COBOL Numeric Encoding Notes

### Zoned Decimal (PIC 9)

Standard COBOL zoned decimal stores one digit per byte. In ASCII data files, digits appear as their ASCII character equivalents (`0`-`9`).

### Signed Zoned Decimal (PIC S9)

The sign is encoded in the **last byte** of the field using an "overpunch" convention. In EBCDIC-to-ASCII converted files:

| Last Character | Digit Value | Sign | EBCDIC Origin |
|---|---|---|---|
| `{` | 0 | + | `C0` |
| `A` | 1 | + | `C1` |
| `B` | 2 | + | `C2` |
| `C` | 3 | + | `C3` |
| `D` | 4 | + | `C4` |
| `E` | 5 | + | `C5` |
| `F` | 6 | + | `C6` |
| `G` | 7 | + | `C7` |
| `H` | 8 | + | `C8` |
| `I` | 9 | + | `C9` |
| `}` | 0 | - | `D0` |
| `J` | 1 | - | `D1` |
| `K` | 2 | - | `D2` |
| `L` | 3 | - | `D3` |
| `M` | 4 | - | `D4` |
| `N` | 5 | - | `D5` |
| `O` | 6 | - | `D6` |
| `P` | 7 | - | `D7` |
| `Q` | 8 | - | `D8` |
| `R` | 9 | - | `D9` |

### Implied Decimal (V99)

The `V` in `PIC S9(10)V99` indicates an implied decimal point. No physical decimal point exists in the data. The last 2 digits represent cents.

Example: `00000001940{` with `PIC S9(10)V99` = `+0000000194.00` = **$194.00**

---

## Data Quality Issues

### 1. Expired Accounts Still Marked Active

**Severity:** Medium
**Files affected:** `acctdata.txt`
**Details:** Many accounts have `ACCT-EXPIRAION-DATE` in the past (e.g., `2023-01-06`, `2023-01-27`) but `ACCT-ACTIVE-STATUS` = `Y`. In a production system, expired accounts should either be deactivated or have their expiration dates updated.

**Examples:**
- Account 10: Expired `2023-01-27`, Status `Y`
- Account 21: Expired `2023-01-06`, Status `Y`
- Account 37: Expired `2023-10-24`, Status `Y`

### 2. Misspelled Field Name in Copybook

**Severity:** Low (code-level)
**Files affected:** `CVACT01Y.cpy`, `CVACT02Y.cpy`, `CVEXPORT.cpy`
**Details:** The field `ACCT-EXPIRAION-DATE` and `CARD-EXPIRAION-DATE` are misspelled (should be "EXPIRATION"). This is consistent across all copybooks, so it does not cause functional issues but indicates a naming error propagated throughout the codebase.

### 3. Unrealistic FICO Credit Scores

**Severity:** Medium
**Files affected:** `custdata.txt`
**Details:** FICO scores range from 300-850 in reality. Several customers have scores below 300:
- Customer 1: FICO 274
- Customer 8: FICO 051
- Customer 13: FICO 053
- Customer 17: FICO 054
- Customer 26: FICO 001
- Customer 27: FICO 078
- Customer 31: FICO 058

These are unrealistically low and would not exist in a real credit bureau file.

### 4. Non-Standard US State Codes

**Severity:** Low
**Files affected:** `custdata.txt`
**Details:** Several customer records use US territory codes or non-standard state abbreviations that, while valid USPS codes, are unusual for credit card customers:
- `FM` (Federated States of Micronesia) - Customer 17
- `MH` (Marshall Islands) - Customer 20
- `PW` (Palau) - Customer 46
- `GU` (Guam) - Customers 27, 47
- `AS` (American Samoa) - Customer 28
- `AP` (Armed Forces Pacific) - Customer 18

### 5. All Cycle Credits and Debits Are Zero

**Severity:** Low (likely initialization state)
**Files affected:** `acctdata.txt`
**Details:** `ACCT-CURR-CYC-CREDIT` and `ACCT-CURR-CYC-DEBIT` are `$0.00` for all 50 accounts, suggesting the data represents a start-of-cycle state before any transactions have been posted to account balances.

### 6. All Transaction Category Balances Are Zero

**Severity:** Low (likely initialization state)
**Files affected:** `tcatbal.txt`
**Details:** All 50 category balance records show `$0.00`, and only one category (`01`/`0001` - Regular Sales Draft) exists per account. This confirms the data is in an initialized state before batch processing.

### 7. All Daily Transactions Have Same Timestamp

**Severity:** Medium
**Files affected:** `dailytran.txt`
**Details:** All 300 transactions share the identical timestamp `2022-06-10 19:27:53.000000`, which is unrealistic for real transaction data. This indicates the data was batch-generated for testing purposes.

### 8. Processing Timestamp Not Populated

**Severity:** Low (expected for unprocessed transactions)
**Files affected:** `dailytran.txt`
**Details:** The `DALYTRAN-PROC-TS` field is blank/spaces for all 300 records, indicating these transactions have not yet been processed by the batch posting program.

### 9. Merchant ID Is Constant

**Severity:** Low
**Files affected:** `dailytran.txt`
**Details:** All 300 transactions use the same `DALYTRAN-MERCHANT-ID` value of `800000000`, despite having different merchant names and cities. In production, each merchant would have a unique ID.

### 10. Customer ID Equals Account ID

**Severity:** Low (simplification for demo)
**Files affected:** `cardxref.txt`
**Details:** In all 50 cross-reference records, `XREF-CUST-ID` equals `XREF-ACCT-ID` (after accounting for field length differences). In production systems, a customer can have multiple accounts, and account IDs typically differ from customer IDs.

### 11. Only Two Transaction Types Used in Daily Data

**Severity:** Low (limited test coverage)
**Files affected:** `dailytran.txt`
**Details:** While 7 transaction types are defined (`trantype.txt`), only types `01` (Purchase) and `03` (Credit/Return) appear in the daily transaction file. Types `02` (Payment), `04` (Authorization), `05` (Refund), `06` (Reversal), and `07` (Adjustment) have no sample transactions.

### 12. ACCT-ADDR-ZIP Contains Group Identifier

**Severity:** Low (ambiguous field usage)
**Files affected:** `acctdata.txt`
**Details:** The `ACCT-ADDR-ZIP` field (position 103-112) contains values like `A000000000` which appear to be group identifiers rather than ZIP codes. This may indicate the ZIP field is being repurposed or the field mapping in the sample data is shifted.

---

*Document generated from CardDemo v1.0/v2.0 copybooks and ASCII sample data files.*
*Copybook dates: 2022-07-19 to 2025-10-16*
