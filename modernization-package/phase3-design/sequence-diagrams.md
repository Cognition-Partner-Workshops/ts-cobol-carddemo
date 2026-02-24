# Sequence Diagrams: Transaction Processing Module

> **Module:** Transaction Processing (CardDemo Modernization)
> **Phase:** 3 - Design
> **Notation:** Mermaid.js
> **BRE Reference:** `transactions-processing-module-doc-devin.md`

---

## 1. CT02 Add Transaction — Full 6-Phase Validation Chain

This is the primary sequence diagram showing the complete flow of the Add Transaction function, including all 6 validation phases, cross-reference resolution, confirmation gate, ID generation, and record write.

```mermaid
sequenceDiagram
    autonumber
    participant UI as React Frontend
    participant API as TransactionController
    participant VS as TransactionValidationService
    participant XS as CrossReferenceService
    participant XR as CardCrossReferenceRepository
    participant TS as TransactionService
    participant TR as TransactionRepository
    participant DB as PostgreSQL

    UI->>API: POST /api/v1/transactions<br/>(AddTransactionRequest)
    API->>VS: validateAndProcess(request)

    Note over VS: ═══════════════════════<br/>PHASE 1: Key Field Validation<br/>BR-AT-01, BR-AT-02, BR-AT-03,<br/>BR-AT-04, BR-AT-05<br/>═══════════════════════

    VS->>VS: Phase 1.1: Check Account ID or Card Number provided (BR-AT-01)
    alt Both accountId AND cardNumber are empty
        VS-->>API: ValidationException(phase=1, field="accountId",<br/>"Account or Card Number must be entered...")
        API-->>UI: 400 Bad Request
    end

    alt accountId is provided
        VS->>VS: Phase 1.2a: Validate Account ID is numeric (BR-AT-02)
        alt accountId is NOT numeric
            VS-->>API: ValidationException(phase=1, field="accountId",<br/>"Account ID must be Numeric...")
            API-->>UI: 400 Bad Request
        end
        VS->>XS: resolveByAccountId(accountId)
        XS->>XR: findByAccountId(accountId)
        XR->>DB: SELECT * FROM card_cross_reference<br/>WHERE account_id = ?
        DB-->>XR: Result
        alt Account NOT found in cross-reference (BR-AT-04)
            XR-->>XS: Optional.empty()
            XS-->>VS: NotFoundException("Account ID NOT found...")
            VS-->>API: ValidationException(phase=1, field="accountId",<br/>"Account ID NOT found...")
            API-->>UI: 404 Not Found
        end
        XR-->>XS: CardCrossReference record
        XS-->>VS: resolvedCardNumber (BR-AT-05)
    else cardNumber is provided
        VS->>VS: Phase 1.2b: Validate Card Number is numeric (BR-AT-03)
        alt cardNumber is NOT numeric
            VS-->>API: ValidationException(phase=1, field="cardNumber",<br/>"Card Number must be Numeric...")
            API-->>UI: 400 Bad Request
        end
        VS->>XS: resolveByCardNumber(cardNumber)
        XS->>XR: findByCardNumber(cardNumber)
        XR->>DB: SELECT * FROM card_cross_reference<br/>WHERE card_number = ?
        DB-->>XR: Result
        alt Card NOT found in cross-reference (BR-AT-04)
            XR-->>XS: Optional.empty()
            XS-->>VS: NotFoundException("Card Number NOT found...")
            VS-->>API: ValidationException(phase=1, field="cardNumber",<br/>"Card Number NOT found...")
            API-->>UI: 404 Not Found
        end
        XR-->>XS: CardCrossReference record
        XS-->>VS: resolvedAccountId (BR-AT-05)
    end

    Note over VS: Phase 1 PASSED ✓<br/>Cross-reference resolved

    Note over VS: ═══════════════════════<br/>PHASE 2: Mandatory Field Checks<br/>BR-AT-06 (11 fields)<br/>═══════════════════════

    VS->>VS: Phase 2: Check 11 mandatory fields in order
    Note right of VS: Check order:<br/>1. Type Code<br/>2. Category Code<br/>3. Source<br/>4. Description<br/>5. Amount<br/>6. Origination Date<br/>7. Processing Date<br/>8. Merchant ID<br/>9. Merchant Name<br/>10. Merchant City<br/>11. Merchant Zip
    alt Any mandatory field is empty (first empty field halts chain)
        VS-->>API: ValidationException(phase=2, field="{fieldName}",<br/>"{Field} can NOT be empty...")
        API-->>UI: 400 Bad Request
    end

    Note over VS: Phase 2 PASSED ✓<br/>All 11 fields present

    Note over VS: ═══════════════════════<br/>PHASE 3: Numeric Type Checks<br/>BR-AT-07<br/>═══════════════════════

    VS->>VS: Phase 3.1: Validate Type Code is numeric
    alt typeCode is NOT numeric
        VS-->>API: ValidationException(phase=3, field="typeCode",<br/>"Type CD must be Numeric...")
        API-->>UI: 400 Bad Request
    end
    VS->>VS: Phase 3.2: Validate Category Code is numeric
    alt categoryCode is NOT numeric
        VS-->>API: ValidationException(phase=3, field="categoryCode",<br/>"Category CD must be Numeric...")
        API-->>UI: 400 Bad Request
    end

    Note over VS: Phase 3 PASSED ✓<br/>Types are numeric

    Note over VS: ═══════════════════════<br/>PHASE 4: Amount Format Validation<br/>BR-AT-08<br/>═══════════════════════

    VS->>VS: Phase 4: Validate amount matches +/-99999999.99
    alt amount does NOT match format
        VS-->>API: ValidationException(phase=4, field="amount",<br/>"Amount should be in format -99999999.99")
        API-->>UI: 400 Bad Request
    end

    Note over VS: Phase 4 PASSED ✓<br/>Amount format valid

    Note over VS: ═══════════════════════<br/>PHASE 5: Date Validation<br/>BR-AT-09, BR-AT-10<br/>═══════════════════════

    VS->>VS: Phase 5.1: Validate Origination Date format (YYYY-MM-DD)
    alt originationDate format invalid (BR-AT-09)
        VS-->>API: ValidationException(phase=5, field="originationDate",<br/>"Orig Date should be in format YYYY-MM-DD")
        API-->>UI: 400 Bad Request
    end
    VS->>VS: Phase 5.2: Validate Origination Date is valid calendar date
    Note right of VS: Uses java.time.LocalDate.parse()<br/>Replaces legacy CSUTLDTC utility
    alt originationDate is NOT a valid calendar date (BR-AT-10)
        VS-->>API: ValidationException(phase=5, field="originationDate",<br/>"Orig Date - Not a valid date...")
        API-->>UI: 400 Bad Request
    end
    VS->>VS: Phase 5.3: Validate Processing Date format (YYYY-MM-DD)
    alt processingDate format invalid (BR-AT-09)
        VS-->>API: ValidationException(phase=5, field="processingDate",<br/>"Proc Date should be in format YYYY-MM-DD")
        API-->>UI: 400 Bad Request
    end
    VS->>VS: Phase 5.4: Validate Processing Date is valid calendar date
    alt processingDate is NOT a valid calendar date (BR-AT-10)
        VS-->>API: ValidationException(phase=5, field="processingDate",<br/>"Proc Date - Not a valid date...")
        API-->>UI: 400 Bad Request
    end

    Note over VS: Phase 5 PASSED ✓<br/>Both dates valid

    Note over VS: ═══════════════════════<br/>PHASE 6: Merchant ID Numeric Check<br/>BR-AT-11<br/>═══════════════════════

    VS->>VS: Phase 6: Validate Merchant ID is numeric
    alt merchantId is NOT numeric
        VS-->>API: ValidationException(phase=6, field="merchantId",<br/>"Merchant ID must be Numeric...")
        API-->>UI: 400 Bad Request
    end

    Note over VS: Phase 6 PASSED ✓<br/>ALL VALIDATION COMPLETE

    Note over VS: ═══════════════════════<br/>CONFIRMATION GATE<br/>BR-AT-12<br/>═══════════════════════

    VS-->>API: Validation passed, return resolved data
    API->>API: Check confirmation field

    alt confirmation = "Y" or "y"
        Note over API: Confirmation accepted → proceed to write
    else confirmation = "N" or "n" or null or ""
        API-->>UI: 200 OK<br/>{"confirmationRequired": true,<br/>"message": "Confirm to add this transaction..."}
    else confirmation = other value
        API-->>UI: 200 OK<br/>{"confirmationRequired": true,<br/>"message": "Invalid value. Valid values are (Y/N)..."}
    end

    Note over TS: ═══════════════════════<br/>ID GENERATION & RECORD WRITE<br/>BR-AT-13, BR-AT-14<br/>═══════════════════════

    API->>TS: createTransaction(validatedRequest, resolvedXref)
    TS->>DB: SELECT nextval('transaction_id_seq')
    DB-->>TS: nextId (e.g., 151)
    TS->>TS: Format ID: LPAD(151, 16, '0') → "0000000000000151"
    TS->>TS: Build Transaction entity with all fields
    TS->>TR: save(transaction)
    TR->>DB: INSERT INTO transaction VALUES (...)
    alt UNIQUE constraint violation (BR-AT-14)
        DB-->>TR: DuplicateKeyException
        TR-->>TS: DataIntegrityViolationException
        TS-->>API: DuplicateTransactionException<br/>("Tran ID already exist...")
        API-->>UI: 409 Conflict
    else Unexpected DB error
        DB-->>TR: SQLException
        TR-->>TS: DataAccessException
        TS-->>API: TransactionWriteException<br/>("Unable to Add Transaction...")
        API-->>UI: 500 Internal Server Error
    end
    DB-->>TR: Success
    TR-->>TS: Saved Transaction entity
    TS-->>API: TransactionDetailResponse
    API-->>UI: 201 Created<br/>{"transactionId": "0000000000000151",<br/>"message": "Transaction added successfully.<br/>Your Tran ID is 0000000000000151."}
```

---

## 2. Phase 1 Detail — Cross-Reference Resolution

Detailed view of Phase 1 showing both resolution paths (Account → Card and Card → Account).

```mermaid
sequenceDiagram
    autonumber
    participant VS as ValidationService
    participant XS as CrossReferenceService
    participant XR as CardCrossRefRepository
    participant DB as PostgreSQL

    Note over VS: Phase 1: Key Field Validation

    VS->>VS: Check: Is accountId provided AND non-empty?
    VS->>VS: Check: Is cardNumber provided AND non-empty?

    alt NEITHER provided (BR-AT-01)
        VS->>VS: FAIL → "Account or Card Number must be entered..."
    else accountId IS provided (Path A)
        VS->>VS: Check: Is accountId all digits? (BR-AT-02)
        alt NOT numeric
            VS->>VS: FAIL → "Account ID must be Numeric..."
        end
        VS->>XS: resolveByAccountId(accountId)
        XS->>XR: findByAccountId(Long.parseLong(accountId))
        XR->>DB: SELECT card_number, customer_id, account_id<br/>FROM card_cross_reference<br/>WHERE account_id = ?
        Note right of DB: Uses index: idx_card_xref_account_id<br/>(replaces CXACAIX Alternate Index)
        DB-->>XR: 0 or 1 rows
        alt 0 rows → NOT FOUND (BR-AT-04)
            XR-->>XS: Optional.empty()
            XS->>VS: throw NotFoundException<br/>("Account ID NOT found...")
        end
        XR-->>XS: CardCrossReference(cardNumber, customerId, accountId)
        XS-->>VS: CrossReferenceResult(cardNumber="4111...", accountId="000...")
        Note over VS: Auto-populate cardNumber with resolved value (BR-AT-05)
    else cardNumber IS provided (Path B)
        VS->>VS: Check: Is cardNumber all digits? (BR-AT-03)
        alt NOT numeric
            VS->>VS: FAIL → "Card Number must be Numeric..."
        end
        VS->>XS: resolveByCardNumber(cardNumber)
        XS->>XR: findByCardNumber(cardNumber)
        XR->>DB: SELECT card_number, customer_id, account_id<br/>FROM card_cross_reference<br/>WHERE card_number = ?
        Note right of DB: Uses PRIMARY KEY index<br/>(replaces CCXREF KSDS lookup)
        DB-->>XR: 0 or 1 rows
        alt 0 rows → NOT FOUND (BR-AT-04)
            XR-->>XS: Optional.empty()
            XS->>VS: throw NotFoundException<br/>("Card Number NOT found...")
        end
        XR-->>XS: CardCrossReference(cardNumber, customerId, accountId)
        XS-->>VS: CrossReferenceResult(cardNumber="4111...", accountId="000...")
        Note over VS: Auto-populate accountId with resolved value (BR-AT-05)
    end

    Note over VS: Phase 1 PASSED ✓<br/>Both accountId and cardNumber are now resolved
```

---

## 3. Phase 2 Detail — Mandatory Field Validation

```mermaid
sequenceDiagram
    autonumber
    participant VS as ValidationService

    Note over VS: Phase 2: Mandatory Field Checks (BR-AT-06)<br/>11 fields checked in strict order.<br/>First empty field halts the chain.

    VS->>VS: 1. Is typeCode empty?
    alt EMPTY
        VS->>VS: FAIL → field="typeCode", "Type CD can NOT be empty..."
    end

    VS->>VS: 2. Is categoryCode empty?
    alt EMPTY
        VS->>VS: FAIL → field="categoryCode", "Category CD can NOT be empty..."
    end

    VS->>VS: 3. Is source empty?
    alt EMPTY
        VS->>VS: FAIL → field="source", "Source can NOT be empty..."
    end

    VS->>VS: 4. Is description empty?
    alt EMPTY
        VS->>VS: FAIL → field="description", "Description can NOT be empty..."
    end

    VS->>VS: 5. Is amount empty?
    alt EMPTY
        VS->>VS: FAIL → field="amount", "Amount can NOT be empty..."
    end

    VS->>VS: 6. Is originationDate empty?
    alt EMPTY
        VS->>VS: FAIL → field="originationDate", "Orig Date can NOT be empty..."
    end

    VS->>VS: 7. Is processingDate empty?
    alt EMPTY
        VS->>VS: FAIL → field="processingDate", "Proc Date can NOT be empty..."
    end

    VS->>VS: 8. Is merchantId empty?
    alt EMPTY
        VS->>VS: FAIL → field="merchantId", "Merchant ID can NOT be empty..."
    end

    VS->>VS: 9. Is merchantName empty?
    alt EMPTY
        VS->>VS: FAIL → field="merchantName", "Merchant Name can NOT be empty..."
    end

    VS->>VS: 10. Is merchantCity empty?
    alt EMPTY
        VS->>VS: FAIL → field="merchantCity", "Merchant City can NOT be empty..."
    end

    VS->>VS: 11. Is merchantZip empty?
    alt EMPTY
        VS->>VS: FAIL → field="merchantZip", "Merchant Zip can NOT be empty..."
    end

    Note over VS: Phase 2 PASSED ✓<br/>All 11 mandatory fields present
```

---

## 4. Phases 3–6 Detail — Format & Type Validation

```mermaid
sequenceDiagram
    autonumber
    participant VS as ValidationService

    Note over VS: ══════════════════<br/>Phase 3: Numeric Type Checks<br/>BR-AT-07<br/>══════════════════

    VS->>VS: Is typeCode all digits?
    alt NOT numeric
        VS->>VS: FAIL → field="typeCode", "Type CD must be Numeric..."
    end

    VS->>VS: Is categoryCode all digits?
    alt NOT numeric
        VS->>VS: FAIL → field="categoryCode", "Category CD must be Numeric..."
    end

    Note over VS: Phase 3 PASSED ✓

    Note over VS: ══════════════════<br/>Phase 4: Amount Format<br/>BR-AT-08<br/>══════════════════

    VS->>VS: Does amount match regex?<br/>Pattern: ^[+-]?\d{1,8}\.\d{2}$
    Note right of VS: Legacy format: +/-99999999.99<br/>Sign optional, 1-8 integer digits,<br/>mandatory decimal point, exactly 2 decimal digits
    alt DOES NOT match
        VS->>VS: FAIL → field="amount",<br/>"Amount should be in format -99999999.99"
    end

    Note over VS: Phase 4 PASSED ✓

    Note over VS: ══════════════════<br/>Phase 5: Date Validation<br/>BR-AT-09, BR-AT-10<br/>══════════════════

    VS->>VS: Does originationDate match YYYY-MM-DD format?
    alt FORMAT invalid (BR-AT-09)
        VS->>VS: FAIL → field="originationDate",<br/>"Orig Date should be in format YYYY-MM-DD"
    end

    VS->>VS: Is originationDate a valid calendar date?
    Note right of VS: LocalDate.parse(originationDate)<br/>Replaces legacy CSUTLDTC utility call
    alt NOT valid calendar date (e.g., 2024-02-30) (BR-AT-10)
        VS->>VS: FAIL → field="originationDate",<br/>"Orig Date - Not a valid date..."
    end

    VS->>VS: Does processingDate match YYYY-MM-DD format?
    alt FORMAT invalid (BR-AT-09)
        VS->>VS: FAIL → field="processingDate",<br/>"Proc Date should be in format YYYY-MM-DD"
    end

    VS->>VS: Is processingDate a valid calendar date?
    alt NOT valid calendar date (BR-AT-10)
        VS->>VS: FAIL → field="processingDate",<br/>"Proc Date - Not a valid date..."
    end

    Note over VS: Phase 5 PASSED ✓

    Note over VS: ══════════════════<br/>Phase 6: Merchant ID Numeric<br/>BR-AT-11<br/>══════════════════

    VS->>VS: Is merchantId all digits?
    alt NOT numeric
        VS->>VS: FAIL → field="merchantId",<br/>"Merchant ID must be Numeric..."
    end

    Note over VS: Phase 6 PASSED ✓<br/>ALL VALIDATION COMPLETE
```

---

## 5. ID Generation & Record Write

```mermaid
sequenceDiagram
    autonumber
    participant API as TransactionController
    participant TS as TransactionService
    participant TR as TransactionRepository
    participant SEQ as PostgreSQL Sequence
    participant DB as PostgreSQL

    Note over API: All 6 validation phases passed<br/>Confirmation = "Y"

    API->>TS: createTransaction(validatedData, resolvedXref)

    Note over TS: ══════════════════<br/>ID Generation (BR-AT-13)<br/>══════════════════

    TS->>SEQ: SELECT nextval('transaction_id_seq')
    Note right of SEQ: Thread-safe: PostgreSQL sequences<br/>are atomic and concurrent-safe.<br/>Replaces legacy STARTBR HIGH-VALUES<br/>→ READPREV → +1 pattern.
    SEQ-->>TS: 151

    TS->>TS: Format: LPAD('151', 16, '0')<br/>→ "0000000000000151"

    Note over TS: ══════════════════<br/>Build Transaction Record<br/>══════════════════

    TS->>TS: Create Transaction entity
    Note right of TS: transactionId = "0000000000000151"<br/>cardNumber = resolvedXref.cardNumber<br/>typeCode = validatedData.typeCode<br/>categoryCode = Integer.parseInt(validatedData.categoryCode)<br/>source = validatedData.source<br/>description = validatedData.description<br/>amount = new BigDecimal(validatedData.amount)<br/>merchantId = Long.parseLong(validatedData.merchantId)<br/>merchantName = validatedData.merchantName<br/>merchantCity = validatedData.merchantCity<br/>merchantZip = validatedData.merchantZip<br/>originationTimestamp = LocalDate.parse(validatedData.originationDate).atStartOfDay()<br/>processingTimestamp = LocalDate.parse(validatedData.processingDate).atStartOfDay()

    Note over TS: ══════════════════<br/>Record Write (BR-AT-14)<br/>══════════════════

    TS->>TR: save(transaction)
    TR->>DB: INSERT INTO transaction<br/>(transaction_id, card_number, type_code,<br/>category_code, source, description, amount,<br/>merchant_id, merchant_name, merchant_city,<br/>merchant_zip, origination_ts, processing_ts)<br/>VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

    alt SUCCESS
        DB-->>TR: 1 row inserted
        TR-->>TS: Saved Transaction entity
        TS-->>API: AddTransactionResponse<br/>(transactionId="0000000000000151",<br/>message="Transaction added successfully.<br/>Your Tran ID is 0000000000000151.")
    else UNIQUE CONSTRAINT VIOLATION (BR-AT-14)
        DB-->>TR: ERROR: duplicate key value violates<br/>unique constraint "transaction_pkey"
        TR-->>TS: DataIntegrityViolationException
        TS-->>API: DuplicateTransactionException<br/>("Tran ID already exist...")
    else UNEXPECTED ERROR
        DB-->>TR: SQLException
        TR-->>TS: DataAccessException
        TS-->>API: TransactionWriteException<br/>("Unable to Add Transaction...")
    end
```

---

## 6. CT00 List Transactions — Pagination Flow

```mermaid
sequenceDiagram
    autonumber
    participant UI as React Frontend
    participant API as TransactionController
    participant TS as TransactionService
    participant TR as TransactionRepository
    participant DB as PostgreSQL

    Note over UI: CT00 - List Transactions<br/>Replaces COTRN00C

    UI->>API: GET /api/v1/transactions?page=0&size=10

    alt startTransactionId is provided
        API->>API: Validate startTransactionId is numeric (BR-LT-02)
        alt NOT numeric
            API-->>UI: 400 Bad Request<br/>"Tran ID must be Numeric ..."
        end
    end

    API->>TS: listTransactions(page, size, startTransactionId)

    alt startTransactionId is provided (BR-LT-04)
        TS->>TR: findByTransactionIdGreaterThanEqual(startTransactionId, pageable)
        TR->>DB: SELECT * FROM transaction<br/>WHERE transaction_id >= ?<br/>ORDER BY transaction_id ASC<br/>LIMIT 10 OFFSET ?
    else No filter
        TS->>TR: findAll(pageable)
        TR->>DB: SELECT * FROM transaction<br/>ORDER BY transaction_id ASC<br/>LIMIT 10 OFFSET ?
    end

    DB-->>TR: ResultSet (0-10 rows)
    TR-->>TS: Page<Transaction>

    TS-->>API: TransactionListResponse

    API-->>UI: 200 OK<br/>{content: [...], page: 0, size: 10,<br/>totalElements: 150, totalPages: 15,<br/>first: true, last: false,<br/>hasNext: true, hasPrevious: false}

    Note over UI: React renders table with 10 rows (BR-LT-01)<br/>Shows page number (BR-LT-07)

    alt User clicks "Next Page" (PF8 equivalent)
        alt response.last == true (BR-LT-06)
            UI->>UI: Show: "You are already at the bottom of the page..."
        else
            UI->>API: GET /api/v1/transactions?page=1&size=10
        end
    end

    alt User clicks "Previous Page" (PF7 equivalent)
        alt response.first == true (BR-LT-05)
            UI->>UI: Show: "You are already at the top of the page..."
        else
            UI->>API: GET /api/v1/transactions?page={current-1}&size=10
        end
    end

    alt User selects a transaction row (BR-LT-03)
        UI->>UI: Navigate to /transactions/{transactionId}
        Note over UI: Replaces "S" selection + XCTL to COTRN01C
    end
```

---

## 7. CT01 View Transaction — Detail Lookup

```mermaid
sequenceDiagram
    autonumber
    participant UI as React Frontend
    participant API as TransactionController
    participant TS as TransactionService
    participant TR as TransactionRepository
    participant XR as CardCrossRefRepository
    participant DB as PostgreSQL

    Note over UI: CT01 - View Transaction<br/>Replaces COTRN01C

    alt Auto-load from List (BR-VT-03)
        UI->>UI: transactionId from route params<br/>(replaces CDEMO-CT00-TRN-SELECTED in COMMAREA)
    else Manual entry
        UI->>UI: User types Transaction ID in input field
    end

    UI->>API: GET /api/v1/transactions/{transactionId}

    API->>TS: viewTransaction(transactionId)
    TS->>TR: findById(transactionId)
    TR->>DB: SELECT * FROM transaction<br/>WHERE transaction_id = ?
    Note right of DB: Standard SELECT — no FOR UPDATE<br/>Improves on legacy BR-VT-02 which<br/>used READ with UPDATE lock unnecessarily

    alt Transaction NOT found (BR-VT-01)
        DB-->>TR: 0 rows
        TR-->>TS: Optional.empty()
        TS-->>API: NotFoundException<br/>("Transaction ID NOT found...")
        API-->>UI: 404 Not Found
    end

    DB-->>TR: Transaction record
    TR-->>TS: Transaction entity

    Note over TS: Resolve Account ID from cross-reference<br/>for display on View screen

    TS->>XR: findByCardNumber(transaction.cardNumber)
    XR->>DB: SELECT * FROM card_cross_reference<br/>WHERE card_number = ?
    DB-->>XR: CardCrossReference record
    XR-->>TS: accountId from cross-reference

    TS-->>API: TransactionDetailResponse (13 fields + accountId)

    API-->>UI: 200 OK<br/>{transactionId, accountId, cardNumber,<br/>typeCode, categoryCode, source, description,<br/>amount, merchantId, merchantName, merchantCity,<br/>merchantZip, originationTimestamp, processingTimestamp}

    Note over UI: Display all 13 detail fields (BR-VT-03)<br/>Read-only — no edit controls (BR-VT-04)

    alt User clicks "Back to List" (PF5 equivalent, BR-VT-05)
        UI->>UI: Navigate to /transactions<br/>with preserved pagination state
    end

    alt User clicks "Clear" (PF4 equivalent)
        UI->>UI: Clear all displayed fields<br/>Focus on Transaction ID input
    end

    alt User clicks "Back to Menu" (PF3 equivalent)
        UI->>UI: Navigate to main menu
    end
```

---

## 8. CT02 Copy Last Transaction (PF5)

```mermaid
sequenceDiagram
    autonumber
    participant UI as React Frontend
    participant API as TransactionController
    participant VS as ValidationService
    participant XS as CrossReferenceService
    participant TS as TransactionService
    participant TR as TransactionRepository
    participant DB as PostgreSQL

    Note over UI: User clicks "Copy Last" (PF5)<br/>on Add Transaction screen

    UI->>UI: Read current accountId / cardNumber from form

    Note over UI: First validate key fields (US-AT-06 AC#1)

    alt Key field validation would fail
        UI->>API: POST /api/v1/transactions (for validation only)
        API-->>UI: 400 Bad Request (Phase 1 error)
        UI->>UI: Display Phase 1 error message
    else Key fields are valid
        UI->>API: GET /api/v1/transactions/latest
        API->>TS: getLatestTransaction()
        TS->>TR: findFirstByOrderByTransactionIdDesc()
        TR->>DB: SELECT * FROM transaction<br/>ORDER BY transaction_id DESC<br/>LIMIT 1
        Note right of DB: Replaces legacy:<br/>STARTBR TRANSACT HIGH-VALUES<br/>→ READPREV → get record

        alt No transactions exist
            DB-->>TR: 0 rows
            TR-->>TS: Optional.empty()
            TS-->>API: NotFoundException
            API-->>UI: 404 Not Found
            UI->>UI: Show "No transactions available to copy"
        end

        DB-->>TR: Transaction record (highest ID)
        TR-->>TS: Transaction entity
        TS-->>API: LatestTransactionResponse

        API-->>UI: 200 OK<br/>{typeCode, categoryCode, source, description,<br/>amount, originationDate, processingDate,<br/>merchantId, merchantName, merchantCity, merchantZip}

        UI->>UI: Populate 11 data fields from response
        Note over UI: Account ID and Card Number<br/>are NOT overwritten (US-AT-06 AC#4)<br/>Confirmation remains blank (US-AT-06 AC#5)
    end
```

---

## 9. Validation Chain Summary Flow

High-level overview of the 6-phase validation chain as a single diagram for quick reference:

```mermaid
flowchart TD
    START([POST /api/v1/transactions]) --> P1

    subgraph P1[Phase 1: Key Field Validation]
        P1A{Account or Card<br/>provided?} -->|No| P1E1[❌ BR-AT-01<br/>Account or Card Number<br/>must be entered]
        P1A -->|Yes| P1B{Is value<br/>numeric?}
        P1B -->|No| P1E2[❌ BR-AT-02/03<br/>Must be Numeric]
        P1B -->|Yes| P1C{Found in<br/>cross-reference?}
        P1C -->|No| P1E3[❌ BR-AT-04<br/>NOT found]
        P1C -->|Yes| P1D[✓ Resolve counterpart<br/>BR-AT-05]
    end

    P1D --> P2

    subgraph P2[Phase 2: Mandatory Fields]
        P2A{All 11 fields<br/>non-empty?} -->|No| P2E[❌ BR-AT-06<br/>Field can NOT be empty]
        P2A -->|Yes| P2D[✓ All present]
    end

    P2D --> P3

    subgraph P3[Phase 3: Numeric Types]
        P3A{Type Code<br/>numeric?} -->|No| P3E1[❌ BR-AT-07<br/>Type CD must be Numeric]
        P3A -->|Yes| P3B{Category Code<br/>numeric?}
        P3B -->|No| P3E2[❌ BR-AT-07<br/>Category CD must be Numeric]
        P3B -->|Yes| P3D[✓ Types valid]
    end

    P3D --> P4

    subgraph P4[Phase 4: Amount Format]
        P4A{Matches<br/>+/-99999999.99?} -->|No| P4E[❌ BR-AT-08<br/>Amount format invalid]
        P4A -->|Yes| P4D[✓ Amount valid]
    end

    P4D --> P5

    subgraph P5[Phase 5: Date Validation]
        P5A{Orig Date<br/>YYYY-MM-DD?} -->|No| P5E1[❌ BR-AT-09<br/>Format invalid]
        P5A -->|Yes| P5B{Orig Date<br/>valid calendar?}
        P5B -->|No| P5E2[❌ BR-AT-10<br/>Not a valid date]
        P5B -->|Yes| P5C{Proc Date<br/>YYYY-MM-DD?}
        P5C -->|No| P5E3[❌ BR-AT-09<br/>Format invalid]
        P5C -->|Yes| P5D2{Proc Date<br/>valid calendar?}
        P5D2 -->|No| P5E4[❌ BR-AT-10<br/>Not a valid date]
        P5D2 -->|Yes| P5D[✓ Dates valid]
    end

    P5D --> P6

    subgraph P6[Phase 6: Merchant ID]
        P6A{Merchant ID<br/>numeric?} -->|No| P6E[❌ BR-AT-11<br/>Must be Numeric]
        P6A -->|Yes| P6D[✓ Merchant ID valid]
    end

    P6D --> CONF

    subgraph CONF[Confirmation Gate]
        CONFA{confirmation<br/>value?} -->|Y/y| CONFD[✓ Proceed to write]
        CONFA -->|N/n/blank| CONFP[⏸ Prompt: Confirm to add...]
        CONFA -->|Other| CONFE[❌ BR-AT-12<br/>Invalid value]
    end

    CONFD --> WRITE

    subgraph WRITE[Record Write]
        WR1[Generate ID via sequence<br/>BR-AT-13] --> WR2[INSERT INTO transaction]
        WR2 -->|Success| WR3[✅ 201 Created<br/>Transaction added successfully]
        WR2 -->|Duplicate| WR4[❌ BR-AT-14<br/>Tran ID already exist]
        WR2 -->|Error| WR5[❌ Unable to Add Transaction]
    end

    style P1 fill:#ffcdd2
    style P2 fill:#f8bbd0
    style P3 fill:#e1bee7
    style P4 fill:#d1c4e9
    style P5 fill:#c5cae9
    style P6 fill:#bbdefb
    style CONF fill:#b2dfdb
    style WRITE fill:#c8e6c9
```

---

## 10. Error Message to Validation Phase Mapping

Complete mapping of all error messages from BRE Section 8.2 (Add Transaction) to their validation phase:

| Phase | Business Rule | Error Message | Field | HTTP Status |
|---|---|---|---|---|
| 1 | BR-AT-01 | "Account or Card Number must be entered..." | `accountId` | 400 |
| 1 | BR-AT-02 | "Account ID must be Numeric..." | `accountId` | 400 |
| 1 | BR-AT-03 | "Card Number must be Numeric..." | `cardNumber` | 400 |
| 1 | BR-AT-04 | "Account ID NOT found..." | `accountId` | 404 |
| 1 | BR-AT-04 | "Card Number NOT found..." | `cardNumber` | 404 |
| 1 | N/A | "Unable to lookup Acct in XREF AIX file..." | `accountId` | 500 |
| 1 | N/A | "Unable to lookup Card # in XREF file..." | `cardNumber` | 500 |
| 2 | BR-AT-06 | "Type CD can NOT be empty..." | `typeCode` | 400 |
| 2 | BR-AT-06 | "Category CD can NOT be empty..." | `categoryCode` | 400 |
| 2 | BR-AT-06 | "Source can NOT be empty..." | `source` | 400 |
| 2 | BR-AT-06 | "Description can NOT be empty..." | `description` | 400 |
| 2 | BR-AT-06 | "Amount can NOT be empty..." | `amount` | 400 |
| 2 | BR-AT-06 | "Orig Date can NOT be empty..." | `originationDate` | 400 |
| 2 | BR-AT-06 | "Proc Date can NOT be empty..." | `processingDate` | 400 |
| 2 | BR-AT-06 | "Merchant ID can NOT be empty..." | `merchantId` | 400 |
| 2 | BR-AT-06 | "Merchant Name can NOT be empty..." | `merchantName` | 400 |
| 2 | BR-AT-06 | "Merchant City can NOT be empty..." | `merchantCity` | 400 |
| 2 | BR-AT-06 | "Merchant Zip can NOT be empty..." | `merchantZip` | 400 |
| 3 | BR-AT-07 | "Type CD must be Numeric..." | `typeCode` | 400 |
| 3 | BR-AT-07 | "Category CD must be Numeric..." | `categoryCode` | 400 |
| 4 | BR-AT-08 | "Amount should be in format -99999999.99" | `amount` | 400 |
| 5 | BR-AT-09 | "Orig Date should be in format YYYY-MM-DD" | `originationDate` | 400 |
| 5 | BR-AT-09 | "Proc Date should be in format YYYY-MM-DD" | `processingDate` | 400 |
| 5 | BR-AT-10 | "Orig Date - Not a valid date..." | `originationDate` | 400 |
| 5 | BR-AT-10 | "Proc Date - Not a valid date..." | `processingDate` | 400 |
| 6 | BR-AT-11 | "Merchant ID must be Numeric..." | `merchantId` | 400 |
| — | BR-AT-12 | "Confirm to add this transaction..." | `confirmation` | 200 |
| — | BR-AT-12 | "Invalid value. Valid values are (Y/N)..." | `confirmation` | 200 |
| — | BR-AT-14 | "Tran ID already exist..." | `transactionId` | 409 |
| — | N/A | "Unable to Add Transaction..." | `null` | 500 |

**Total: 29 distinct error messages from the Add Transaction error catalog — all mapped.**
