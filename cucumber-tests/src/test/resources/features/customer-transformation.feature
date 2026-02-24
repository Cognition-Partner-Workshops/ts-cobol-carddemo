@customer @transformation
Feature: Customer Data Transformation Validation
  Validates that mainframe COBOL customer records (CVCUS01Y.cpy, RECLN 500)
  are correctly transformed into modern JSON representations.

  COBOL Copybook Reference (CVCUS01Y.cpy):
    01 CUSTOMER-RECORD.
      05 CUST-ID                    PIC 9(09)       - Numeric customer identifier
      05 CUST-FIRST-NAME            PIC X(25)       - Alphanumeric, space-padded
      05 CUST-MIDDLE-NAME           PIC X(25)       - Alphanumeric, space-padded
      05 CUST-LAST-NAME             PIC X(25)       - Alphanumeric, space-padded
      05 CUST-ADDR-LINE-1           PIC X(50)       - Address line 1
      05 CUST-ADDR-LINE-2           PIC X(50)       - Address line 2
      05 CUST-ADDR-LINE-3           PIC X(50)       - Address line 3
      05 CUST-ADDR-STATE-CD         PIC X(02)       - Two-char state code
      05 CUST-ADDR-COUNTRY-CD       PIC X(03)       - Three-char country code
      05 CUST-ADDR-ZIP              PIC X(10)       - ZIP/postal code
      05 CUST-PHONE-NUM-1           PIC X(15)       - Primary phone
      05 CUST-PHONE-NUM-2           PIC X(15)       - Secondary phone
      05 CUST-SSN                   PIC 9(09)       - Social Security Number
      05 CUST-GOVT-ISSUED-ID        PIC X(20)       - Government ID
      05 CUST-DOB-YYYY-MM-DD        PIC X(10)       - Date of birth
      05 CUST-EFT-ACCOUNT-ID        PIC X(10)       - EFT account reference
      05 CUST-PRI-CARD-HOLDER-IND   PIC X(01)       - Primary card holder flag
      05 CUST-FICO-CREDIT-SCORE     PIC 9(03)       - FICO score (000-999)
      05 FILLER                     PIC X(168)      - Padding to RECLN 500

  Background:
    Given the customer transformation service is available
    And the COBOL copybook "CVCUS01Y" defines record length 500

  # ───────────────────────────────────────────────────────────────
  # Happy-Path Direct Field Mappings
  # ───────────────────────────────────────────────────────────────

  @happy-path
  Scenario: HP-01 - Transform a fully populated customer record
    Given a mainframe customer record with the following COBOL fields:
      | field                    | value                     |
      | CUST-ID                  | 000000001                 |
      | CUST-FIRST-NAME          | JOHN                      |
      | CUST-MIDDLE-NAME         | MICHAEL                   |
      | CUST-LAST-NAME           | SMITH                     |
      | CUST-ADDR-LINE-1         | 123 MAIN STREET           |
      | CUST-ADDR-LINE-2         | APT 4B                    |
      | CUST-ADDR-LINE-3         |                           |
      | CUST-ADDR-STATE-CD       | NY                        |
      | CUST-ADDR-COUNTRY-CD     | USA                       |
      | CUST-ADDR-ZIP            | 10001                     |
      | CUST-PHONE-NUM-1         | 212-555-0100              |
      | CUST-PHONE-NUM-2         | 917-555-0200              |
      | CUST-SSN                 | 123456789                 |
      | CUST-GOVT-ISSUED-ID      | DL12345678901234          |
      | CUST-DOB-YYYY-MM-DD      | 1985-06-15                |
      | CUST-EFT-ACCOUNT-ID      | EFT0000001                |
      | CUST-PRI-CARD-HOLDER-IND | Y                         |
      | CUST-FICO-CREDIT-SCORE   | 750                       |
    When the customer record is submitted for transformation
    Then the transformed customer JSON should contain:
      | jsonField          | expectedValue          | expectedType |
      | customerId         | 1                      | long         |
      | firstName          | JOHN                   | string       |
      | middleName         | MICHAEL                | string       |
      | lastName           | SMITH                  | string       |
      | addressLine1       | 123 MAIN STREET        | string       |
      | addressLine2       | APT 4B                 | string       |
      | addressLine3       |                        | string       |
      | stateCode          | NY                     | string       |
      | countryCode        | USA                    | string       |
      | addressZip         | 10001                  | string       |
      | phoneNumber1       | 212-555-0100           | string       |
      | phoneNumber2       | 917-555-0200           | string       |
      | ssn                | 123456789              | string       |
      | govtIssuedId       | DL12345678901234       | string       |
      | dateOfBirth        | 1985-06-15             | date         |
      | eftAccountId       | EFT0000001             | string       |
      | primaryCardHolder  | true                   | boolean      |
      | ficoCreditScore    | 750                    | integer      |

  @happy-path
  Scenario: HP-02 - Non-primary card holder flag transforms to boolean false
    Given a mainframe customer record with the following COBOL fields:
      | field                    | value     |
      | CUST-ID                  | 000000002 |
      | CUST-FIRST-NAME          | JANE      |
      | CUST-LAST-NAME           | DOE       |
      | CUST-PRI-CARD-HOLDER-IND | N         |
    When the customer record is submitted for transformation
    Then the transformed JSON field "primaryCardHolder" should be false as a boolean

  @happy-path
  Scenario: HP-03 - Customer ID with leading zeros converts to numeric long
    Given a mainframe customer record with CUST-ID "000000099"
    When the customer record is submitted for transformation
    Then the transformed JSON field "customerId" should be 99 as a long

  # ───────────────────────────────────────────────────────────────
  # Type Conversion: Numeric Fields
  # ───────────────────────────────────────────────────────────────

  @type-conversion @numeric
  Scenario: TC-01 - CUST-SSN PIC 9(09) converts to masked or raw string
    Given a mainframe customer record with CUST-SSN "987654321"
    When the customer record is submitted for transformation
    Then the transformed JSON field "ssn" should be "987654321" as a string

  @type-conversion @numeric
  Scenario: TC-02 - FICO score PIC 9(03) converts to integer
    Given a mainframe customer record with CUST-FICO-CREDIT-SCORE "820"
    When the customer record is submitted for transformation
    Then the transformed JSON field "ficoCreditScore" should be 820 as an integer

  @type-conversion @numeric
  Scenario: TC-03 - FICO score with leading zeros converts correctly
    Given a mainframe customer record with CUST-FICO-CREDIT-SCORE "045"
    When the customer record is submitted for transformation
    Then the transformed JSON field "ficoCreditScore" should be 45 as an integer

  @type-conversion @date
  Scenario: TC-04 - Date of birth EBCDIC format converts to ISO-8601
    Given a mainframe customer record with CUST-DOB-YYYY-MM-DD "1990-12-25"
    When the customer record is submitted for transformation
    Then the transformed JSON field "dateOfBirth" should be "1990-12-25" as an ISO date

  @type-conversion @boolean
  Scenario: TC-05 - Primary card holder indicator Y maps to true
    Given a mainframe customer record with CUST-PRI-CARD-HOLDER-IND "Y"
    When the customer record is submitted for transformation
    Then the transformed JSON field "primaryCardHolder" should be true as a boolean

  @type-conversion @boolean
  Scenario: TC-06 - Primary card holder indicator N maps to false
    Given a mainframe customer record with CUST-PRI-CARD-HOLDER-IND "N"
    When the customer record is submitted for transformation
    Then the transformed JSON field "primaryCardHolder" should be false as a boolean

  # ───────────────────────────────────────────────────────────────
  # Nullable Handling & Default Values
  # ───────────────────────────────────────────────────────────────

  @nullable
  Scenario: NL-01 - Spaces-only CUST-MIDDLE-NAME maps to null or empty
    Given a mainframe customer record with CUST-MIDDLE-NAME "                         "
    When the customer record is submitted for transformation
    Then the transformed JSON field "middleName" should be null or empty

  @nullable
  Scenario: NL-02 - Spaces-only CUST-ADDR-LINE-2 maps to null or empty
    Given a mainframe customer record with CUST-ADDR-LINE-2 all spaces
    When the customer record is submitted for transformation
    Then the transformed JSON field "addressLine2" should be null or empty

  @nullable
  Scenario: NL-03 - Spaces-only CUST-ADDR-LINE-3 maps to null or empty
    Given a mainframe customer record with CUST-ADDR-LINE-3 all spaces
    When the customer record is submitted for transformation
    Then the transformed JSON field "addressLine3" should be null or empty

  @nullable
  Scenario: NL-04 - Spaces-only CUST-PHONE-NUM-2 maps to null or empty
    Given a mainframe customer record with CUST-PHONE-NUM-2 "               "
    When the customer record is submitted for transformation
    Then the transformed JSON field "phoneNumber2" should be null or empty

  @nullable
  Scenario: NL-05 - Spaces-only CUST-DOB-YYYY-MM-DD maps to null
    Given a mainframe customer record with CUST-DOB-YYYY-MM-DD "          "
    When the customer record is submitted for transformation
    Then the transformed JSON field "dateOfBirth" should be null

  @nullable
  Scenario: NL-06 - Spaces-only CUST-EFT-ACCOUNT-ID maps to null or empty
    Given a mainframe customer record with CUST-EFT-ACCOUNT-ID "          "
    When the customer record is submitted for transformation
    Then the transformed JSON field "eftAccountId" should be null or empty

  # ───────────────────────────────────────────────────────────────
  # Boundary & Edge Cases
  # ───────────────────────────────────────────────────────────────

  @edge-case @boundary
  Scenario: EC-01 - Customer ID at maximum PIC 9(09) value
    Given a mainframe customer record with CUST-ID "999999999"
    When the customer record is submitted for transformation
    Then the transformed JSON field "customerId" should be 999999999 as a long

  @edge-case @boundary
  Scenario: EC-02 - FICO score at maximum (999)
    Given a mainframe customer record with CUST-FICO-CREDIT-SCORE "999"
    When the customer record is submitted for transformation
    Then the transformed JSON field "ficoCreditScore" should be 999 as an integer

  @edge-case @boundary
  Scenario: EC-03 - FICO score at minimum (000)
    Given a mainframe customer record with CUST-FICO-CREDIT-SCORE "000"
    When the customer record is submitted for transformation
    Then the transformed JSON field "ficoCreditScore" should be 0 as an integer

  @edge-case
  Scenario: EC-04 - Name fields at maximum length (25 characters)
    Given a mainframe customer record with CUST-FIRST-NAME "ABCDEFGHIJKLMNOPQRSTUVWXY"
    When the customer record is submitted for transformation
    Then the transformed JSON field "firstName" should be "ABCDEFGHIJKLMNOPQRSTUVWXY"

  @edge-case
  Scenario: EC-05 - FILLER bytes are discarded during transformation
    Given a mainframe customer record with 168 bytes of FILLER data
    When the customer record is submitted for transformation
    Then the transformed JSON should not contain a "filler" field
    And the total record length consumed should be 500 bytes

  @edge-case @error
  Scenario: EC-06 - Invalid date of birth format is handled gracefully
    Given a mainframe customer record with CUST-DOB-YYYY-MM-DD "15/06/1985"
    When the customer record is submitted for transformation
    Then the transformation should either reject the record or set "dateOfBirth" to null

  @edge-case @error
  Scenario: EC-07 - Non-numeric data in CUST-SSN field
    Given a mainframe customer record with CUST-SSN "12345ABCD"
    When the customer record is submitted for transformation
    Then the transformation should report a validation error for "ssn"

  @edge-case
  Scenario: EC-08 - State code with lowercase converts or preserves case
    Given a mainframe customer record with CUST-ADDR-STATE-CD "ny"
    When the customer record is submitted for transformation
    Then the transformed JSON field "stateCode" should be "NY" or "ny"

  @edge-case
  Scenario: EC-09 - SSN with all zeros is still valid
    Given a mainframe customer record with CUST-SSN "000000000"
    When the customer record is submitted for transformation
    Then the transformed JSON field "ssn" should be "000000000" as a string

  @edge-case
  Scenario: EC-10 - Trailing spaces in address fields are trimmed
    Given a mainframe customer record with CUST-ADDR-LINE-1 "123 MAIN STREET                                   "
    When the customer record is submitted for transformation
    Then the transformed JSON field "addressLine1" should be "123 MAIN STREET"
