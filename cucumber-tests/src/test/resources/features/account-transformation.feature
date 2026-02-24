@account @transformation
Feature: Account Data Transformation Validation
  Validates that mainframe COBOL account records (CVACT01Y.cpy, RECLN 300)
  are correctly transformed into modern JSON representations.

  COBOL Copybook Reference (CVACT01Y.cpy):
    01 ACCOUNT-RECORD.
      05 ACCT-ID                    PIC 9(11)       - Numeric account identifier
      05 ACCT-ACTIVE-STATUS         PIC X(01)       - Single-char status flag
      05 ACCT-CURR-BAL              PIC S9(10)V99   - Signed packed decimal balance
      05 ACCT-CREDIT-LIMIT          PIC S9(10)V99   - Signed packed decimal limit
      05 ACCT-CASH-CREDIT-LIMIT     PIC S9(10)V99   - Signed packed decimal cash limit
      05 ACCT-OPEN-DATE             PIC X(10)       - EBCDIC date string
      05 ACCT-EXPIRAION-DATE        PIC X(10)       - EBCDIC date string (typo preserved)
      05 ACCT-REISSUE-DATE          PIC X(10)       - EBCDIC date string
      05 ACCT-CURR-CYC-CREDIT       PIC S9(10)V99   - Signed packed decimal
      05 ACCT-CURR-CYC-DEBIT        PIC S9(10)V99   - Signed packed decimal
      05 ACCT-ADDR-ZIP              PIC X(10)       - Alphanumeric ZIP
      05 ACCT-GROUP-ID              PIC X(10)       - Alphanumeric group
      05 FILLER                     PIC X(178)      - Padding to RECLN 300

  Background:
    Given the account transformation service is available
    And the COBOL copybook "CVACT01Y" defines record length 300

  # ───────────────────────────────────────────────────────────────
  # Happy-Path Direct Field Mappings
  # ───────────────────────────────────────────────────────────────

  @happy-path
  Scenario: HP-01 - Transform a fully populated account record
    Given a mainframe account record with the following COBOL fields:
      | field                  | value                |
      | ACCT-ID                | 00000000001          |
      | ACCT-ACTIVE-STATUS     | Y                    |
      | ACCT-CURR-BAL          | +0000050000.00       |
      | ACCT-CREDIT-LIMIT      | +0000100000.00       |
      | ACCT-CASH-CREDIT-LIMIT | +0000025000.00       |
      | ACCT-OPEN-DATE         | 2020-03-15           |
      | ACCT-EXPIRAION-DATE    | 2025-03-15           |
      | ACCT-REISSUE-DATE      | 2023-03-15           |
      | ACCT-CURR-CYC-CREDIT   | +0000001500.75       |
      | ACCT-CURR-CYC-DEBIT    | -0000000200.50       |
      | ACCT-ADDR-ZIP          | 10001     	          |
      | ACCT-GROUP-ID          | GRP001              |
    When the account record is submitted for transformation
    Then the transformed account JSON should contain:
      | jsonField       | expectedValue   | expectedType |
      | accountId       | 1               | long         |
      | activeStatus    | Y               | string       |
      | currentBalance  | 50000.00        | decimal      |
      | creditLimit     | 100000.00       | decimal      |
      | cashCreditLimit | 25000.00        | decimal      |
      | openDate        | 2020-03-15      | date         |
      | expirationDate  | 2025-03-15      | date         |
      | reissueDate     | 2023-03-15      | date         |
      | cycleCredit     | 1500.75         | decimal      |
      | cycleDebit      | -200.50         | decimal      |
      | addressZip      | 10001           | string       |
      | groupId         | GRP001          | string       |

  @happy-path
  Scenario: HP-02 - Account with inactive status transforms correctly
    Given a mainframe account record with the following COBOL fields:
      | field              | value           |
      | ACCT-ID            | 99999999999     |
      | ACCT-ACTIVE-STATUS | N               |
      | ACCT-CURR-BAL      | +0000000000.00  |
      | ACCT-CREDIT-LIMIT  | +0000005000.00  |
    When the account record is submitted for transformation
    Then the transformed JSON field "accountId" should be 99999999999 as a long
    And the transformed JSON field "activeStatus" should be "N"
    And the transformed JSON field "currentBalance" should be 0.00 as a decimal

  @happy-path
  Scenario: HP-03 - Account ID with leading zeros is converted to numeric long
    Given a mainframe account record with ACCT-ID "00000000042"
    When the account record is submitted for transformation
    Then the transformed JSON field "accountId" should be 42 as a long

  # ───────────────────────────────────────────────────────────────
  # Type Conversion: Signed Packed Decimal (PIC S9(10)V99)
  # ───────────────────────────────────────────────────────────────

  @type-conversion @packed-decimal
  Scenario: TC-01 - Positive signed decimal balance converts to BigDecimal
    Given a mainframe account record with ACCT-CURR-BAL "+0000012345.67"
    When the account record is submitted for transformation
    Then the transformed JSON field "currentBalance" should be 12345.67 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-02 - Negative signed decimal balance preserves sign
    Given a mainframe account record with ACCT-CURR-BAL "-0000054321.99"
    When the account record is submitted for transformation
    Then the transformed JSON field "currentBalance" should be -54321.99 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-03 - Zero balance with sign converts correctly
    Given a mainframe account record with ACCT-CURR-BAL "+0000000000.00"
    When the account record is submitted for transformation
    Then the transformed JSON field "currentBalance" should be 0.00 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-04 - Maximum value for PIC S9(10)V99 field
    Given a mainframe account record with ACCT-CREDIT-LIMIT "+9999999999.99"
    When the account record is submitted for transformation
    Then the transformed JSON field "creditLimit" should be 9999999999.99 as a decimal

  @type-conversion @packed-decimal
  Scenario: TC-05 - Minimum negative value for PIC S9(10)V99 field
    Given a mainframe account record with ACCT-CURR-CYC-DEBIT "-9999999999.99"
    When the account record is submitted for transformation
    Then the transformed JSON field "cycleDebit" should be -9999999999.99 as a decimal

  # ───────────────────────────────────────────────────────────────
  # Type Conversion: EBCDIC Dates (PIC X(10))
  # ───────────────────────────────────────────────────────────────

  @type-conversion @date
  Scenario: TC-06 - EBCDIC date string converts to ISO-8601 date
    Given a mainframe account record with ACCT-OPEN-DATE "2019-01-31"
    When the account record is submitted for transformation
    Then the transformed JSON field "openDate" should be "2019-01-31" as an ISO date

  @type-conversion @date
  Scenario: TC-07 - Leap year date converts correctly
    Given a mainframe account record with ACCT-OPEN-DATE "2024-02-29"
    When the account record is submitted for transformation
    Then the transformed JSON field "openDate" should be "2024-02-29" as an ISO date

  # ───────────────────────────────────────────────────────────────
  # Nullable Handling & Default Values
  # ───────────────────────────────────────────────────────────────

  @nullable
  Scenario: NL-01 - Spaces-only ACCT-OPEN-DATE maps to null
    Given a mainframe account record with ACCT-OPEN-DATE "          "
    When the account record is submitted for transformation
    Then the transformed JSON field "openDate" should be null

  @nullable
  Scenario: NL-02 - Spaces-only ACCT-REISSUE-DATE maps to null
    Given a mainframe account record with ACCT-REISSUE-DATE "          "
    When the account record is submitted for transformation
    Then the transformed JSON field "reissueDate" should be null

  @nullable
  Scenario: NL-03 - Spaces-only ACCT-GROUP-ID maps to null or empty
    Given a mainframe account record with ACCT-GROUP-ID "          "
    When the account record is submitted for transformation
    Then the transformed JSON field "groupId" should be null or empty

  @nullable @default
  Scenario: NL-04 - Spaces-only ACCT-ADDR-ZIP maps to null or empty
    Given a mainframe account record with ACCT-ADDR-ZIP "          "
    When the account record is submitted for transformation
    Then the transformed JSON field "addressZip" should be null or empty

  @default
  Scenario: DF-01 - Spaces-only ACCT-ACTIVE-STATUS defaults appropriately
    Given a mainframe account record with ACCT-ACTIVE-STATUS " "
    When the account record is submitted for transformation
    Then the transformed JSON field "activeStatus" should be " " or a defined default

  # ───────────────────────────────────────────────────────────────
  # Boundary & Edge Cases
  # ───────────────────────────────────────────────────────────────

  @edge-case @boundary
  Scenario: EC-01 - Account ID at maximum PIC 9(11) value
    Given a mainframe account record with ACCT-ID "99999999999"
    When the account record is submitted for transformation
    Then the transformed JSON field "accountId" should be 99999999999 as a long

  @edge-case @boundary
  Scenario: EC-02 - Account ID at minimum value (all zeros)
    Given a mainframe account record with ACCT-ID "00000000000"
    When the account record is submitted for transformation
    Then the transformed JSON field "accountId" should be 0 as a long

  @edge-case
  Scenario: EC-03 - ACCT-ADDR-ZIP with mixed alphanumeric (Canadian postal)
    Given a mainframe account record with ACCT-ADDR-ZIP "K1A 0B1   "
    When the account record is submitted for transformation
    Then the transformed JSON field "addressZip" should be "K1A 0B1"

  @edge-case
  Scenario: EC-04 - Date fields at epoch boundary (Y2K)
    Given a mainframe account record with ACCT-OPEN-DATE "2000-01-01"
    When the account record is submitted for transformation
    Then the transformed JSON field "openDate" should be "2000-01-01" as an ISO date

  @edge-case @error
  Scenario: EC-05 - Invalid date format is rejected or handled gracefully
    Given a mainframe account record with ACCT-OPEN-DATE "31/01/2020"
    When the account record is submitted for transformation
    Then the transformation should either reject the record or set "openDate" to null

  @edge-case @error
  Scenario: EC-06 - Non-numeric data in numeric ACCT-ID field
    Given a mainframe account record with ACCT-ID "0000000ABCD"
    When the account record is submitted for transformation
    Then the transformation should report a validation error for "accountId"

  @edge-case
  Scenario: EC-07 - FILLER bytes are discarded during transformation
    Given a mainframe account record with 178 bytes of FILLER data "XXXXXXXXXX..."
    When the account record is submitted for transformation
    Then the transformed JSON should not contain a "filler" field
    And the total record length consumed should be 300 bytes

  @edge-case
  Scenario: EC-08 - Trailing spaces in string fields are trimmed
    Given a mainframe account record with ACCT-GROUP-ID "GRP001    "
    When the account record is submitted for transformation
    Then the transformed JSON field "groupId" should be "GRP001"

  @edge-case @boundary
  Scenario: EC-09 - Credit limit of exactly zero
    Given a mainframe account record with ACCT-CREDIT-LIMIT "+0000000000.00"
    When the account record is submitted for transformation
    Then the transformed JSON field "creditLimit" should be 0.00 as a decimal

  @edge-case
  Scenario: EC-10 - Expiration date in the past is still transformed
    Given a mainframe account record with ACCT-EXPIRAION-DATE "2019-12-31"
    When the account record is submitted for transformation
    Then the transformed JSON field "expirationDate" should be "2019-12-31" as an ISO date
