@transaction @transformation
Feature: Transaction Data Transformation Validation
  Validates that mainframe COBOL transaction records (CVTRA05Y.cpy, RECLN 350)
  are correctly transformed into modern JSON representations.

  COBOL Copybook Reference (CVTRA05Y.cpy):
    01 TRAN-RECORD.
      05 TRAN-ID                    PIC X(16)       - Transaction identifier
      05 TRAN-TYPE-CD               PIC X(02)       - Transaction type code
      05 TRAN-CAT-CD                PIC 9(04)       - Category code (numeric)
      05 TRAN-SOURCE                PIC X(10)       - Transaction source
      05 TRAN-DESC                  PIC X(100)      - Description
      05 TRAN-AMT                   PIC S9(09)V99   - Signed amount with 2 decimals
      05 TRAN-MERCHANT-ID           PIC 9(09)       - Merchant identifier
      05 TRAN-MERCHANT-NAME         PIC X(50)       - Merchant name
      05 TRAN-MERCHANT-CITY         PIC X(50)       - Merchant city
      05 TRAN-MERCHANT-ZIP          PIC X(10)       - Merchant ZIP
      05 TRAN-CARD-NUM              PIC X(16)       - Card number
      05 TRAN-ORIG-TS               PIC X(26)       - Origination timestamp
      05 TRAN-PROC-TS               PIC X(26)       - Processing timestamp
      05 FILLER                     PIC X(20)       - Padding to RECLN 350

  Background:
    Given the transaction transformation service is available
    And the COBOL copybook "CVTRA05Y" defines record length 350

  # ───────────────────────────────────────────────────────────────
  # Happy-Path Direct Field Mappings
  # ───────────────────────────────────────────────────────────────

  @happy-path
  Scenario: HP-01 - Transform a fully populated transaction record
    Given a mainframe transaction record with the following COBOL fields:
      | field              | value                           |
      | TRAN-ID            | 0000000000000001                |
      | TRAN-TYPE-CD       | SA                              |
      | TRAN-CAT-CD        | 5001                            |
      | TRAN-SOURCE        | ONLINE                          |
      | TRAN-DESC          | GROCERY PURCHASE AT STORE 42    |
      | TRAN-AMT           | +000125000.50                   |
      | TRAN-MERCHANT-ID   | 000012345                       |
      | TRAN-MERCHANT-NAME | MEGAMART GROCERY                |
      | TRAN-MERCHANT-CITY | NEW YORK                        |
      | TRAN-MERCHANT-ZIP  | 10001                           |
      | TRAN-CARD-NUM      | 4111111111111111                |
      | TRAN-ORIG-TS       | 2024-07-15-10.30.00.000000      |
      | TRAN-PROC-TS       | 2024-07-15-10.30.05.123456      |
    When the transaction record is submitted for transformation
    Then the transformed transaction JSON should contain:
      | jsonField      | expectedValue                    | expectedType |
      | transactionId  | 0000000000000001                 | string       |
      | typeCode       | SA                               | string       |
      | categoryCode   | 5001                             | integer      |
      | source         | ONLINE                           | string       |
      | description    | GROCERY PURCHASE AT STORE 42     | string       |
      | amount         | 125000.50                        | decimal      |
      | merchantId     | 12345                            | long         |
      | merchantName   | MEGAMART GROCERY                 | string       |
      | merchantCity   | NEW YORK                         | string       |
      | merchantZip    | 10001                            | string       |
      | cardNumber     | 4111111111111111                 | string       |
      | originTimestamp  | 2024-07-15T10:30:00.000000     | timestamp    |
      | processTimestamp | 2024-07-15T10:30:05.123456     | timestamp    |

  @happy-path
  Scenario: HP-02 - Refund transaction with negative amount
    Given a mainframe transaction record with the following COBOL fields:
      | field            | value                       |
      | TRAN-ID          | 0000000000000002            |
      | TRAN-TYPE-CD     | CR                          |
      | TRAN-AMT         | -000000050.00               |
      | TRAN-DESC        | REFUND FOR ORDER 12345      |
    When the transaction record is submitted for transformation
    Then the transformed JSON field "amount" should be -50.00 as a decimal
    And the transformed JSON field "typeCode" should be "CR"

  @happy-path
  Scenario: HP-03 - Transaction ID preserves alphanumeric format
    Given a mainframe transaction record with TRAN-ID "TXN20240715A001"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "transactionId" should be "TXN20240715A001"

  # ───────────────────────────────────────────────────────────────
  # Type Conversion: Signed Packed Decimal (PIC S9(09)V99)
  # ───────────────────────────────────────────────────────────────

  @type-conversion @packed-decimal
  Scenario: TC-01 - Positive transaction amount converts to BigDecimal
    Given a mainframe transaction record with TRAN-AMT "+000000100.00"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "amount" should be 100.00 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-02 - Negative transaction amount preserves sign
    Given a mainframe transaction record with TRAN-AMT "-000000999.99"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "amount" should be -999.99 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-03 - Zero amount converts correctly
    Given a mainframe transaction record with TRAN-AMT "+000000000.00"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "amount" should be 0.00 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-04 - Maximum value for PIC S9(09)V99
    Given a mainframe transaction record with TRAN-AMT "+999999999.99"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "amount" should be 999999999.99 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-05 - Minimum negative for PIC S9(09)V99
    Given a mainframe transaction record with TRAN-AMT "-999999999.99"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "amount" should be -999999999.99 as a decimal

  # ───────────────────────────────────────────────────────────────
  # Type Conversion: Timestamps (PIC X(26))
  # ───────────────────────────────────────────────────────────────

  @type-conversion @timestamp
  Scenario: TC-06 - COBOL timestamp converts to ISO-8601 datetime
    Given a mainframe transaction record with TRAN-ORIG-TS "2024-01-15-14.30.45.123456"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "originTimestamp" should be "2024-01-15T14:30:45.123456" as an ISO timestamp

  @type-conversion @timestamp
  Scenario: TC-07 - Midnight timestamp converts correctly
    Given a mainframe transaction record with TRAN-ORIG-TS "2024-12-31-00.00.00.000000"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "originTimestamp" should be "2024-12-31T00:00:00.000000" as an ISO timestamp

  @type-conversion @numeric
  Scenario: TC-08 - TRAN-CAT-CD PIC 9(04) converts to integer
    Given a mainframe transaction record with TRAN-CAT-CD "0042"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "categoryCode" should be 42 as an integer

  @type-conversion @numeric
  Scenario: TC-09 - TRAN-MERCHANT-ID PIC 9(09) with leading zeros converts to long
    Given a mainframe transaction record with TRAN-MERCHANT-ID "000000001"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "merchantId" should be 1 as a long

  # ───────────────────────────────────────────────────────────────
  # Nullable Handling & Default Values
  # ───────────────────────────────────────────────────────────────

  @nullable
  Scenario: NL-01 - Spaces-only TRAN-DESC maps to null or empty
    Given a mainframe transaction record with TRAN-DESC all spaces
    When the transaction record is submitted for transformation
    Then the transformed JSON field "description" should be null or empty

  @nullable
  Scenario: NL-02 - Spaces-only TRAN-MERCHANT-NAME maps to null or empty
    Given a mainframe transaction record with TRAN-MERCHANT-NAME all spaces
    When the transaction record is submitted for transformation
    Then the transformed JSON field "merchantName" should be null or empty

  @nullable
  Scenario: NL-03 - Spaces-only TRAN-MERCHANT-CITY maps to null or empty
    Given a mainframe transaction record with TRAN-MERCHANT-CITY all spaces
    When the transaction record is submitted for transformation
    Then the transformed JSON field "merchantCity" should be null or empty

  @nullable
  Scenario: NL-04 - Spaces-only TRAN-PROC-TS maps to null
    Given a mainframe transaction record with TRAN-PROC-TS "                          "
    When the transaction record is submitted for transformation
    Then the transformed JSON field "processTimestamp" should be null

  @nullable
  Scenario: NL-05 - Spaces-only TRAN-SOURCE maps to null or empty
    Given a mainframe transaction record with TRAN-SOURCE "          "
    When the transaction record is submitted for transformation
    Then the transformed JSON field "source" should be null or empty

  # ───────────────────────────────────────────────────────────────
  # Boundary & Edge Cases
  # ───────────────────────────────────────────────────────────────

  @edge-case @boundary
  Scenario: EC-01 - Transaction category code at maximum PIC 9(04) (9999)
    Given a mainframe transaction record with TRAN-CAT-CD "9999"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "categoryCode" should be 9999 as an integer

  @edge-case @boundary
  Scenario: EC-02 - Transaction category code at minimum (0000)
    Given a mainframe transaction record with TRAN-CAT-CD "0000"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "categoryCode" should be 0 as an integer

  @edge-case @boundary
  Scenario: EC-03 - Merchant ID at maximum PIC 9(09) value
    Given a mainframe transaction record with TRAN-MERCHANT-ID "999999999"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "merchantId" should be 999999999 as a long

  @edge-case
  Scenario: EC-04 - Description field at maximum 100 characters
    Given a mainframe transaction record with TRAN-DESC "ABCDEFGHIJ" repeated to fill 100 characters
    When the transaction record is submitted for transformation
    Then the transformed JSON field "description" should have length <= 100

  @edge-case
  Scenario: EC-05 - FILLER bytes are discarded during transformation
    Given a mainframe transaction record with 20 bytes of FILLER data
    When the transaction record is submitted for transformation
    Then the transformed JSON should not contain a "filler" field
    And the total record length consumed should be 350 bytes

  @edge-case @error
  Scenario: EC-06 - Invalid timestamp format is handled gracefully
    Given a mainframe transaction record with TRAN-ORIG-TS "2024/01/15 14:30:45.123"
    When the transaction record is submitted for transformation
    Then the transformation should either reject the record or set "originTimestamp" to null

  @edge-case @error
  Scenario: EC-07 - Non-numeric TRAN-CAT-CD is rejected
    Given a mainframe transaction record with TRAN-CAT-CD "ABCD"
    When the transaction record is submitted for transformation
    Then the transformation should report a validation error for "categoryCode"

  @edge-case
  Scenario: EC-08 - Card number with spaces is trimmed
    Given a mainframe transaction record with TRAN-CARD-NUM "4111111111111111"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "cardNumber" should be "4111111111111111"

  @edge-case
  Scenario: EC-09 - Trailing spaces in merchant name are trimmed
    Given a mainframe transaction record with TRAN-MERCHANT-NAME "WALMART                                           "
    When the transaction record is submitted for transformation
    Then the transformed JSON field "merchantName" should be "WALMART"

  @edge-case @timestamp
  Scenario: EC-10 - End-of-day timestamp boundary
    Given a mainframe transaction record with TRAN-ORIG-TS "2024-12-31-23.59.59.999999"
    When the transaction record is submitted for transformation
    Then the transformed JSON field "originTimestamp" should be "2024-12-31T23:59:59.999999" as an ISO timestamp
