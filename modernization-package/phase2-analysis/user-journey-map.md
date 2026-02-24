# User Journey Map: Transaction Add (CT02) Flow

> **Module:** Transaction Processing (CardDemo Modernization)
> **Phase:** 2 - Analysis
> **Source Program:** COTRN02C.cbl
> **CICS Transaction ID:** CT02

---

## 1. High-Level User Journey

```mermaid
journey
    title Transaction Add (CT02) - User Journey
    section Entry
      Navigate to Add Transaction from Main Menu: 3: User
      View blank Add Transaction form: 3: System
    section Key Field Entry
      Enter Account ID or Card Number: 4: User
      System resolves cross-reference (auto-populates other field): 5: System
    section Data Entry
      Enter Type Code: 4: User
      Enter Category Code: 4: User
      Enter Source: 4: User
      Enter Description: 4: User
      Enter Amount: 4: User
      Enter Origination Date: 4: User
      Enter Processing Date: 4: User
      Enter Merchant ID: 4: User
      Enter Merchant Name: 4: User
      Enter Merchant City: 4: User
      Enter Merchant Zip: 4: User
    section Validation
      System validates all fields (6 phases): 5: System
      Fix any validation errors: 2: User
    section Confirmation
      Enter Y to confirm: 4: User
      System generates Transaction ID: 5: System
      System writes record: 5: System
      View success message with new Transaction ID: 5: User
```

---

## 2. Detailed Flow Diagram

```mermaid
flowchart TD
    A[User selects Option 8 from Main Menu<br/>COMEN01C → XCTL → COTRN02C] --> B{First Entry?<br/>CDEMO-PGM-REENTER = 0}

    B -->|Yes| C[Initialize screen to LOW-VALUES<br/>Set cursor on Account ID field]
    B -->|No| D[RECEIVE screen input<br/>COTRN2AI]

    C --> C1{Pre-selected Card Number<br/>in COMMAREA?}
    C1 -->|Yes| C2[Auto-populate Card Number<br/>Run PROCESS-ENTER-KEY]
    C1 -->|No| C3[Display blank form]
    C2 --> E[SEND Add Transaction Screen]
    C3 --> E

    D --> F{Which key pressed?}

    F -->|ENTER| G[PROCESS-ENTER-KEY]
    F -->|PF3| H[Return to caller/menu<br/>XCTL CDEMO-TO-PROGRAM]
    F -->|PF4| I[Clear all fields<br/>INITIALIZE-ALL-FIELDS<br/>SEND blank screen]
    F -->|PF5| J[COPY-LAST-TRAN-DATA<br/>Validate key fields first<br/>Read highest TRAN-ID<br/>Populate data fields from last record]
    F -->|Other| K[Set error: Invalid Key Pressed<br/>SEND screen with error]

    G --> L[Phase 1: VALIDATE-INPUT-KEY-FIELDS]

    L --> L1{Account ID provided?}
    L1 -->|Yes| L2{Is it numeric?}
    L2 -->|No| L2E[Error: Account ID must be Numeric<br/>Cursor → Account ID]
    L2 -->|Yes| L3[READ CXACAIX file<br/>Resolve Account → Card Number]
    L3 --> L3A{Account found<br/>in CXACAIX?}
    L3A -->|No| L3E[Error: Account ID NOT found<br/>Cursor → Account ID]
    L3A -->|Yes| L4[Populate Card Number from XREF-CARD-NUM]

    L1 -->|No| L5{Card Number provided?}
    L5 -->|Yes| L6{Is it numeric?}
    L6 -->|No| L6E[Error: Card Number must be Numeric<br/>Cursor → Card Number]
    L6 -->|Yes| L7[READ CCXREF file<br/>Resolve Card → Account ID]
    L7 --> L7A{Card found<br/>in CCXREF?}
    L7A -->|No| L7E[Error: Card Number NOT found<br/>Cursor → Card Number]
    L7A -->|Yes| L8[Populate Account ID from XREF-ACCT-ID]

    L5 -->|No| L9[Error: Account or Card Number<br/>must be entered<br/>Cursor → Account ID]

    L4 --> M[Phase 2: VALIDATE-INPUT-DATA-FIELDS<br/>Mandatory Field Checks]
    L8 --> M

    M --> M1{Type Code empty?}
    M1 -->|Yes| M1E[Error: Type CD can NOT be empty]
    M1 -->|No| M2{Category Code empty?}
    M2 -->|Yes| M2E[Error: Category CD can NOT be empty]
    M2 -->|No| M3{Source empty?}
    M3 -->|Yes| M3E[Error: Source can NOT be empty]
    M3 -->|No| M4{Description empty?}
    M4 -->|Yes| M4E[Error: Description can NOT be empty]
    M4 -->|No| M5{Amount empty?}
    M5 -->|Yes| M5E[Error: Amount can NOT be empty]
    M5 -->|No| M6{Orig Date empty?}
    M6 -->|Yes| M6E[Error: Orig Date can NOT be empty]
    M6 -->|No| M7{Proc Date empty?}
    M7 -->|Yes| M7E[Error: Proc Date can NOT be empty]
    M7 -->|No| M8{Merchant ID empty?}
    M8 -->|Yes| M8E[Error: Merchant ID can NOT be empty]
    M8 -->|No| M9{Merchant Name empty?}
    M9 -->|Yes| M9E[Error: Merchant Name can NOT be empty]
    M9 -->|No| M10{Merchant City empty?}
    M10 -->|Yes| M10E[Error: Merchant City can NOT be empty]
    M10 -->|No| M11{Merchant Zip empty?}
    M11 -->|Yes| M11E[Error: Merchant Zip can NOT be empty]
    M11 -->|No| N[Phase 3: Numeric Type Checks]

    N --> N1{Type Code numeric?}
    N1 -->|No| N1E[Error: Type CD must be Numeric]
    N1 -->|Yes| N2{Category Code numeric?}
    N2 -->|No| N2E[Error: Category CD must be Numeric]
    N2 -->|Yes| O[Phase 4: Amount Format Validation]

    O --> O1{Amount matches<br/>+/-99999999.99?}
    O1 -->|No| O1E[Error: Amount should be in<br/>format -99999999.99]
    O1 -->|Yes| P[Phase 5: Date Validation]

    P --> P1{Orig Date format<br/>YYYY-MM-DD?}
    P1 -->|No| P1E[Error: Orig Date should be in<br/>format YYYY-MM-DD]
    P1 -->|Yes| P2[Call CSUTLDTC: Validate Orig Date]
    P2 --> P2A{Valid calendar date?}
    P2A -->|No| P2E[Error: Orig Date - Not a valid date]
    P2A -->|Yes| P3{Proc Date format<br/>YYYY-MM-DD?}
    P3 -->|No| P3E[Error: Proc Date should be in<br/>format YYYY-MM-DD]
    P3 -->|Yes| P4[Call CSUTLDTC: Validate Proc Date]
    P4 --> P4A{Valid calendar date?}
    P4A -->|No| P4E[Error: Proc Date - Not a valid date]
    P4A -->|Yes| Q[Phase 6: Merchant ID Numeric Check]

    Q --> Q1{Merchant ID numeric?}
    Q1 -->|No| Q1E[Error: Merchant ID must be Numeric]
    Q1 -->|Yes| R{Confirmation Gate}

    R --> R1{CONFIRMI value?}
    R1 -->|Y/y| S[ADD-TRANSACTION]
    R1 -->|N/n/blank| R2[Message: Confirm to add<br/>this transaction<br/>Cursor → Confirm field]
    R1 -->|Other| R3[Error: Invalid value.<br/>Valid values are Y/N<br/>Cursor → Confirm field]

    S --> S1[STARTBR TRANSACT with HIGH-VALUES]
    S1 --> S2[READPREV to get highest TRAN-ID]
    S2 --> S3[ENDBR TRANSACT]
    S3 --> S4[WS-TRAN-ID-N = highest ID + 1]
    S4 --> S5[Initialize TRAN-RECORD<br/>Populate all fields from screen input]
    S5 --> S6[WRITE TRANSACT file]

    S6 --> S7{Write result?}
    S7 -->|NORMAL| S8[Clear all fields<br/>Green message:<br/>Transaction added successfully.<br/>Your Tran ID is ID.]
    S7 -->|DUPKEY/DUPREC| S9[Error: Tran ID already exist]
    S7 -->|Other| S10[Error: Unable to Add Transaction]

    S8 --> E
    S9 --> E
    S10 --> E

    L2E --> E
    L3E --> E
    L6E --> E
    L7E --> E
    L9 --> E
    M1E --> E
    M2E --> E
    M3E --> E
    M4E --> E
    M5E --> E
    M6E --> E
    M7E --> E
    M8E --> E
    M9E --> E
    M10E --> E
    M11E --> E
    N1E --> E
    N2E --> E
    O1E --> E
    P1E --> E
    P2E --> E
    P3E --> E
    P4E --> E
    Q1E --> E
    R2 --> E
    R3 --> E
    K --> E

    E --> T[EXEC CICS RETURN<br/>TRANSID CT02<br/>COMMAREA CARDDEMO-COMMAREA]
    T --> U((Wait for next<br/>user interaction))
    U --> D
```

---

## 3. Validation Chain Summary (6 Phases)

```mermaid
graph LR
    P1[Phase 1<br/>Key Field<br/>Validation] --> P2[Phase 2<br/>Mandatory<br/>Field Checks]
    P2 --> P3[Phase 3<br/>Numeric Type<br/>Checks]
    P3 --> P4[Phase 4<br/>Amount Format<br/>Validation]
    P4 --> P5[Phase 5<br/>Date<br/>Validation]
    P5 --> P6[Phase 6<br/>Merchant ID<br/>Numeric Check]
    P6 --> CG[Confirmation<br/>Gate Y/N]
    CG --> WR[Write<br/>Record]

    style P1 fill:#ffcdd2
    style P2 fill:#f8bbd0
    style P3 fill:#e1bee7
    style P4 fill:#d1c4e9
    style P5 fill:#c5cae9
    style P6 fill:#bbdefb
    style CG fill:#b2dfdb
    style WR fill:#c8e6c9
```

### Phase Details

| Phase | Name | Fields Checked | Error Behavior |
|---|---|---|---|
| **1** | Key Field Validation | Account ID, Card Number | Validates numericity, existence in XREF files, auto-resolves counterpart field |
| **2** | Mandatory Field Checks | 11 data fields (Type CD, Category CD, Source, Description, Amount, Orig Date, Proc Date, Merchant ID/Name/City/Zip) | Each empty field produces a specific error; checks stop at first failure |
| **3** | Numeric Type Checks | Type Code, Category Code | Must be numeric values |
| **4** | Amount Format Validation | Amount | Must match `[+/-]99999999.99` pattern |
| **5** | Date Validation | Origination Date, Processing Date | Format check (YYYY-MM-DD) then calendar validity via CSUTLDTC utility |
| **6** | Merchant ID Numeric Check | Merchant ID | Must be numeric |

---

## 4. REST API Mapping (Modernized Flow)

### Legacy vs. Modernized Interaction Comparison

| Step | Legacy (CICS/3270) | Modernized (REST/React) |
|---|---|---|
| Entry | XCTL from COMEN01C, COMMAREA passed | `GET /api/transactions/add` (load form) or React route navigation |
| Key Resolution | CICS READ on CXACAIX/CCXREF files | `GET /api/cross-references/resolve?accountId={id}` or `?cardNumber={num}` |
| Field Entry | 3270 screen fields, pseudo-conversational | React form with real-time client-side validation |
| Validation | 6-phase server-side COBOL logic | `POST /api/transactions` with server-side validation (Spring `@Valid` + custom validator) |
| Confirmation | Y/N field on same screen | Confirmation modal/dialog in React UI |
| ID Generation | STARTBR HIGH-VALUES → READPREV → +1 | PostgreSQL SEQUENCE or `SELECT MAX(transaction_id) + 1` with row-level locking |
| Write | CICS WRITE to TRANSACT VSAM | JPA `save()` → PostgreSQL INSERT |
| Success Feedback | Green message on 3270 screen | HTTP 201 Created with JSON response body containing new transaction ID |
| Error Feedback | Error message + cursor repositioning | HTTP 400/422 with structured error response (field-level error messages) |

### Proposed REST Endpoints

| Method | Endpoint | Legacy Equivalent | Description |
|---|---|---|---|
| `GET` | `/api/transactions` | COTRN00C List | Paginated transaction listing |
| `GET` | `/api/transactions/{id}` | COTRN01C View | View single transaction detail |
| `POST` | `/api/transactions` | COTRN02C Add | Create new transaction (with full validation) |
| `GET` | `/api/cross-references/resolve` | CXACAIX/CCXREF READ | Resolve Account ID <-> Card Number |
| `GET` | `/api/transactions/latest` | READPREV from HIGH-VALUES | Get most recent transaction (for PF5 copy feature) |

---

## 5. Pseudo-Conversational State Mapping

The legacy CT02 flow is pseudo-conversational: the CICS program processes one interaction, sends the screen, and returns to CICS. State is preserved in COMMAREA between interactions.

In the modernized architecture:

| Legacy Mechanism | Modernized Replacement |
|---|---|
| COMMAREA persistence across interactions | Stateless REST: All needed data is in the request body / JWT token |
| `CDEMO-PGM-REENTER` flag | Not needed: React SPA maintains UI state client-side |
| `WS-ERR-FLG` error flag | HTTP status codes (400, 422) + structured error response |
| Screen field cursor positioning (`MOVE -1 TO fieldL`) | React form focus management (`ref.current.focus()`) |
| `EXEC CICS RETURN TRANSID COMMAREA` | HTTP response (stateless) + React state management |
