# Phase 5: Comprehensive Testing Plan

## CardDemo Transaction Processing Module - Modernization Testing

**Document Version:** 1.0
**Date:** 2026-02-24
**Module:** Transaction Processing (COTRN00C, COTRN01C, COTRN02C)
**Target Stack:** Java 21, Spring Boot 3, PostgreSQL, React

---

## 1. Testing Objectives

Verify 100% business logic parity between the legacy COBOL/CICS transaction processing module and the modernized Java/Spring Boot microservice. Every one of the 30 Business Rules and every error message in BRE Section 8.2 must have at least one corresponding test case.

### Quality Gate Criteria

| Criteria | Target |
|----------|--------|
| All 30 Business Rules have test cases | 30/30 |
| Every BRE Section 8.2 error message verified | 28/28 |
| All unit tests pass | 100% |
| 6-phase validation chain fully covered | 6 phases + full chain |
| Pagination boundary tests | Top + Bottom |
| Cross-reference resolution (both paths) | Path A + Path B |
| Thread-safe ID generation | Sequence-based |
| Confirmation gate (all values) | Y/y/N/n/blank/invalid |

---

## 2. Test Strategy

### 2.1 Testing Framework

| Component | Technology |
|-----------|-----------|
| Unit Testing | JUnit 5 (Jupiter) |
| Mocking | Mockito |
| Database | H2 in-memory (test profile) |
| Assertions | JUnit 5 + Hamcrest |
| Build Tool | Maven (Surefire plugin) |

### 2.2 Test Categories

1. **Service Layer Unit Tests** - Mock repositories, test pure business logic
2. **Controller Layer Tests** - Test HTTP status codes, request/response mapping
3. **Validation Chain Tests** - Test each of the 6 phases individually and the full chain
4. **Error Message Catalog Tests** - Verify exact error message strings from BRE Section 8.2
5. **Cross-Reference Resolution Tests** - Both Path A and Path B
6. **Pagination & Boundary Tests** - Top/bottom of page detection
7. **ID Generation Tests** - Sequence-based generation, duplicate handling

### 2.3 Test Data Strategy

- Use Mockito mocks for repository layer (unit tests)
- H2 in-memory database for integration-style tests
- Test data mirrors V7 seed data structure

---

## 3. Business Rule Test Matrix

### 3.1 Cross-Functional Rules (BR-CF-01 through BR-CF-03)

| Rule ID | Rule Name | Test Class | Test Method(s) |
|---------|-----------|------------|----------------|
| BR-CF-01 | Pseudo-Conversational State | TransactionControllerTest | testListTransactionsReturnsOk, testViewTransactionReturnsOk |
| BR-CF-02 | Function Key Routing | TransactionControllerTest | testEndpointMapping |
| BR-CF-03 | Invalid Key Handling | TransactionControllerTest | testInvalidEndpointReturns404 |

### 3.2 List Transaction Rules (BR-LT-01 through BR-LT-08)

| Rule ID | Rule Name | Test Class | Test Method(s) |
|---------|-----------|------------|----------------|
| BR-LT-01 | Page Size Fixed at 10 | TransactionServiceTest | testListTransactions_DefaultPageSize |
| BR-LT-02 | Numeric Filter Validation | TransactionServiceTest | testListTransactions_NonNumericFilter_ThrowsException |
| BR-LT-03 | Valid Selection Value | TransactionControllerTest | testListEndpointAcceptsValidParams |
| BR-LT-04 | Empty Filter Browses from Start | TransactionServiceTest | testListTransactions_EmptyFilter_BrowsesFromStart |
| BR-LT-05 | Forward Pagination Boundary | TransactionServiceTest | testListTransactions_LastPage_IsLastTrue |
| BR-LT-06 | Backward Pagination Boundary | TransactionServiceTest | testListTransactions_FirstPage_IsFirstTrue |
| BR-LT-07 | Page State Preservation | TransactionServiceTest | testListTransactions_PageStatePreserved |
| BR-LT-08 | Selection Triggers Detail View | TransactionControllerTest | testViewTransactionEndpoint |

### 3.3 View Transaction Rules (BR-VT-01 through BR-VT-05)

| Rule ID | Rule Name | Test Class | Test Method(s) |
|---------|-----------|------------|----------------|
| BR-VT-01 | Transaction ID Required | TransactionServiceTest | testViewTransaction_EmptyId_ThrowsResourceNotFound, testViewTransaction_NotFound_ThrowsResourceNotFound |
| BR-VT-02 | Transaction Must Exist | TransactionServiceTest | testViewTransaction_ExistingId_ReturnsDetail |
| BR-VT-03 | All 13 Fields Returned | TransactionServiceTest | testViewTransaction_Returns13Fields |
| BR-VT-04 | Read-Only Display | TransactionControllerTest | testViewTransactionIsGetOnly |
| BR-VT-05 | PF5 Returns to List | TransactionControllerTest | testListEndpointExists |

### 3.4 Add Transaction Rules (BR-AT-01 through BR-AT-14)

| Rule ID | Rule Name | Test Class | Test Method(s) |
|---------|-----------|------------|----------------|
| BR-AT-01 | Account or Card Required | TransactionServiceTest | testAddTransaction_NoAccountNoCard_ThrowsValidation |
| BR-AT-02 | Account ID Numeric | TransactionServiceTest | testAddTransaction_NonNumericAccount_ThrowsValidation |
| BR-AT-03 | Card Number Numeric | TransactionServiceTest | testAddTransaction_NonNumericCard_ThrowsValidation |
| BR-AT-04 | Account/Card Must Exist | TransactionServiceTest | testAddTransaction_AccountNotFound_ThrowsValidation, testAddTransaction_CardNotFound_ThrowsValidation |
| BR-AT-05 | Cross-Reference Resolution | CrossReferenceServiceTest | testResolve_ByAccountId_ReturnsCardNumber, testResolve_ByCardNumber_ReturnsAccountId |
| BR-AT-06 | All 11 Fields Mandatory | TransactionServiceTest | testAddTransaction_EmptyTypeCode, testAddTransaction_EmptyCategoryCode, ...(11 tests) |
| BR-AT-07 | Type/Category Numeric | TransactionServiceTest | testAddTransaction_NonNumericTypeCode, testAddTransaction_NonNumericCategoryCode |
| BR-AT-08 | Amount Format | TransactionServiceTest | testAddTransaction_InvalidAmountFormat |
| BR-AT-09 | Date Format Required | TransactionServiceTest | testAddTransaction_InvalidOrigDateFormat, testAddTransaction_InvalidProcDateFormat |
| BR-AT-10 | Date Validity | TransactionServiceTest | testAddTransaction_InvalidOrigDateCalendar, testAddTransaction_InvalidProcDateCalendar |
| BR-AT-11 | Merchant ID Numeric | TransactionServiceTest | testAddTransaction_NonNumericMerchantId |
| BR-AT-12 | Confirmation Gate | TransactionServiceTest | testAddTransaction_ConfirmY, testAddTransaction_ConfirmN, testAddTransaction_ConfirmBlank, testAddTransaction_ConfirmInvalid |
| BR-AT-13 | Auto-Increment ID | TransactionServiceTest | testAddTransaction_GeneratesSequentialId |
| BR-AT-14 | Duplicate ID Rejection | TransactionServiceTest | testAddTransaction_DuplicateId_ThrowsDuplicate |

---

## 4. 6-Phase Validation Chain Tests

Each phase is tested individually to verify it catches errors at the correct point, and a full-chain test verifies the phases execute in order.

### Phase 1: Key Field Validation (BR-AT-01 through BR-AT-05)

| Test | Input | Expected Error |
|------|-------|----------------|
| No key fields | accountId=null, cardNumber=null | "Account or Card Number must be entered..." |
| Non-numeric account | accountId="ABC" | "Account ID must be Numeric..." |
| Non-numeric card | cardNumber="XYZ" | "Card Number must be Numeric..." |
| Account not found | accountId="99999" (not in xref) | "Account ID NOT found..." |
| Card not found | cardNumber="9999999999999999" (not in xref) | "Card Number NOT found..." |

### Phase 2: Mandatory Field Checks (BR-AT-06)

| Test | Empty Field | Expected Error |
|------|-------------|----------------|
| Empty typeCode | typeCode=null | "Type CD can NOT be empty..." |
| Empty categoryCode | categoryCode="" | "Category CD can NOT be empty..." |
| Empty source | source=null | "Source can NOT be empty..." |
| Empty description | description="" | "Description can NOT be empty..." |
| Empty amount | amount=null | "Amount can NOT be empty..." |
| Empty originationDate | originationDate="" | "Orig Date can NOT be empty..." |
| Empty processingDate | processingDate=null | "Proc Date can NOT be empty..." |
| Empty merchantId | merchantId="" | "Merchant ID can NOT be empty..." |
| Empty merchantName | merchantName=null | "Merchant Name can NOT be empty..." |
| Empty merchantCity | merchantCity="" | "Merchant City can NOT be empty..." |
| Empty merchantZip | merchantZip=null | "Merchant Zip can NOT be empty..." |

### Phase 3: Numeric Type Checks (BR-AT-07)

| Test | Input | Expected Error |
|------|-------|----------------|
| Non-numeric typeCode | typeCode="AB" | "Type CD must be Numeric..." |
| Non-numeric categoryCode | categoryCode="ABCD" | "Category CD must be Numeric..." |

### Phase 4: Amount Format Validation (BR-AT-08)

| Test | Input | Expected Error |
|------|-------|----------------|
| Missing decimal | amount="100" | "Amount should be in format -99999999.99" |
| Too many decimals | amount="100.123" | "Amount should be in format -99999999.99" |
| Letters in amount | amount="abc.00" | "Amount should be in format -99999999.99" |
| Valid positive | amount="12345678.99" | (passes) |
| Valid negative | amount="-12345678.99" | (passes) |

### Phase 5: Date Validation (BR-AT-09, BR-AT-10)

| Test | Input | Expected Error |
|------|-------|----------------|
| Bad orig date format | originationDate="01-01-2024" | "Orig Date - Date format must be YYYY-MM-DD..." |
| Bad proc date format | processingDate="2024/01/01" | "Proc Date - Date format must be YYYY-MM-DD..." |
| Invalid orig calendar | originationDate="2024-02-30" | "Orig Date - Not a valid date..." |
| Invalid proc calendar | processingDate="2024-13-01" | "Proc Date - Not a valid date..." |

### Phase 6: Merchant ID Numeric Check (BR-AT-11)

| Test | Input | Expected Error |
|------|-------|----------------|
| Non-numeric merchant ID | merchantId="ABC123" | "Merchant ID must be Numeric..." |

### Full Chain Test

| Test | Description |
|------|-------------|
| All phases pass + confirm Y | Valid request with confirmation Y -> 201 Created |
| Phase 1 fails | Should not reach Phase 2 |
| Phase 2 fails | Should not reach Phase 3 |
| Phase 3 fails | Should not reach Phase 4 |
| Phase 4 fails | Should not reach Phase 5 |
| Phase 5 fails | Should not reach Phase 6 |

---

## 5. Error Message Catalog Verification (BRE Section 8.2)

Every error message listed in BRE Section 8.2 must have a corresponding test that verifies the exact message string.

### List Transactions (COTRN00C) - 5 Messages

| # | Error Message | Test Method |
|---|---------------|-------------|
| 1 | "Tran ID must be Numeric ..." | testListTransactions_NonNumericFilter |
| 2 | "Invalid selection. Valid value is S" | (frontend-only; controller test for valid params) |
| 3 | "You are already at the top of the page..." | testListTransactions_FirstPage_IsFirstTrue |
| 4 | "You are already at the bottom of the page..." | testListTransactions_LastPage_IsLastTrue |
| 5 | "Unable to lookup transaction..." | (generic exception handler test) |

### View Transaction (COTRN01C) - 3 Messages

| # | Error Message | Test Method |
|---|---------------|-------------|
| 6 | "Tran ID can NOT be empty..." | testViewTransaction_EmptyId |
| 7 | "Transaction ID NOT found..." | testViewTransaction_NotFound |
| 8 | "Unable to lookup Transaction..." | (generic exception handler test) |

### Add Transaction (COTRN02C) - 27 Messages

| # | Error Message | Test Method |
|---|---------------|-------------|
| 9 | "Account or Card Number must be entered..." | testPhase1_NoKeyFields |
| 10 | "Account ID must be Numeric..." | testPhase1_NonNumericAccount |
| 11 | "Card Number must be Numeric..." | testPhase1_NonNumericCard |
| 12 | "Account ID NOT found..." | testPhase1_AccountNotFound |
| 13 | "Card Number NOT found..." | testPhase1_CardNotFound |
| 14 | "Type CD can NOT be empty..." | testPhase2_EmptyTypeCode |
| 15 | "Category CD can NOT be empty..." | testPhase2_EmptyCategoryCode |
| 16 | "Source can NOT be empty..." | testPhase2_EmptySource |
| 17 | "Description can NOT be empty..." | testPhase2_EmptyDescription |
| 18 | "Amount can NOT be empty..." | testPhase2_EmptyAmount |
| 19 | "Orig Date can NOT be empty..." | testPhase2_EmptyOrigDate |
| 20 | "Proc Date can NOT be empty..." | testPhase2_EmptyProcDate |
| 21 | "Merchant ID can NOT be empty..." | testPhase2_EmptyMerchantId |
| 22 | "Merchant Name can NOT be empty..." | testPhase2_EmptyMerchantName |
| 23 | "Merchant City can NOT be empty..." | testPhase2_EmptyMerchantCity |
| 24 | "Merchant Zip can NOT be empty..." | testPhase2_EmptyMerchantZip |
| 25 | "Type CD must be Numeric..." | testPhase3_NonNumericTypeCode |
| 26 | "Category CD must be Numeric..." | testPhase3_NonNumericCategoryCode |
| 27 | "Amount should be in format -99999999.99" | testPhase4_InvalidAmountFormat |
| 28 | "Orig Date - Date format must be YYYY-MM-DD..." | testPhase5_BadOrigDateFormat |
| 29 | "Proc Date - Date format must be YYYY-MM-DD..." | testPhase5_BadProcDateFormat |
| 30 | "Orig Date - Not a valid date..." | testPhase5_InvalidOrigCalendar |
| 31 | "Proc Date - Not a valid date..." | testPhase5_InvalidProcCalendar |
| 32 | "Merchant ID must be Numeric..." | testPhase6_NonNumericMerchantId |
| 33 | "Invalid value. Valid values are (Y/N)..." | testConfirmation_InvalidValue |
| 34 | "Tran ID already exist..." | testDuplicateTransactionId |
| 35 | "Unable to Add Transaction..." | testWriteError |

---

## 6. Cross-Reference Resolution Tests

### Path A: Account ID -> Card Number

| Test | Input | Expected |
|------|-------|----------|
| Valid account | accountId="1" | Returns card number + account ID |
| Non-existent account | accountId="99999" | ResourceNotFoundException |
| Both provided | accountId + cardNumber | IllegalArgumentException |
| Neither provided | null, null | IllegalArgumentException |

### Path B: Card Number -> Account ID

| Test | Input | Expected |
|------|-------|----------|
| Valid card number | cardNumber="4111111111111111" | Returns account ID + card number |
| Non-existent card | cardNumber="9999999999999999" | ResourceNotFoundException |

---

## 7. Pagination Boundary Tests

| Test | Scenario | Expected |
|------|----------|----------|
| First page | page=0, 15 total records | isFirst=true, hasPrevious=false |
| Last page | page=1, 15 total (10 per page) | isLast=true, hasNext=false |
| Middle page (if applicable) | Multiple pages | isFirst=false, isLast=false |
| Empty result set | No matching records | content=[], totalElements=0 |
| Negative page size | size=-1 | Defaults to 10 |
| Filter narrows to single page | startTransactionId with few results | Correct pagination flags |

---

## 8. Transaction ID Generation Tests

| Test | Scenario | Expected |
|------|----------|----------|
| Normal generation | Call generateNextTransactionId | Returns 16-char zero-padded ID |
| Duplicate check | existsByTransactionId returns true | DuplicateTransactionException |
| DataIntegrityViolation with duplicate | DB constraint violation | DuplicateTransactionException |
| DataIntegrityViolation other | Non-duplicate DB error | RuntimeException |

---

## 9. Confirmation Gate Tests

| Test | Confirmation Value | Expected |
|------|-------------------|----------|
| "Y" | Uppercase Y | Transaction created (201) |
| "y" | Lowercase y | Transaction created (201) |
| "N" | Uppercase N | ConfirmationRequiredResponse with "Confirm to add..." |
| "n" | Lowercase n | ConfirmationRequiredResponse with "Confirm to add..." |
| null | Not provided | ConfirmationRequiredResponse with "Confirm to add..." |
| "" | Empty string | ConfirmationRequiredResponse with "Confirm to add..." |
| " " | Blank/whitespace | ConfirmationRequiredResponse with "Confirm to add..." |
| "X" | Invalid value | ConfirmationRequiredResponse with "Invalid value..." |
| "YES" | Multi-char | ConfirmationRequiredResponse with "Invalid value..." |

---

## 10. Test File Organization

```
backend/src/test/
  java/com/carddemo/transaction/
    service/
      TransactionServiceTest.java          # Core service tests (BR-LT, BR-VT, BR-AT)
      CrossReferenceServiceTest.java       # Cross-ref resolution tests
    controller/
      TransactionControllerTest.java       # REST endpoint tests
      CrossReferenceControllerTest.java    # Cross-ref endpoint tests
    exception/
      GlobalExceptionHandlerTest.java      # Exception -> HTTP response mapping
  resources/
    application-test.properties            # H2 in-memory database config
```

---

## 11. Coverage Targets

| Package | Target Coverage |
|---------|----------------|
| service | > 90% line coverage |
| controller | > 85% line coverage |
| exception | > 90% line coverage |
| Overall | > 85% line coverage |

---

## 12. Risk Assessment

| Risk | Mitigation |
|------|-----------|
| H2 SQL dialect differences from PostgreSQL | Use H2 PostgreSQL compatibility mode; native query tests may need adaptation |
| Sequence generation not available in H2 | Mock repository method for unit tests; use H2 sequence support for integration |
| Date validation edge cases | Test leap years, boundary dates (Feb 29/30), month boundaries |
| Concurrent ID generation | Verified by PostgreSQL sequence design; unit tests mock the sequence call |
