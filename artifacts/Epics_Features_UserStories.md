# CardDemo Application - Epics, Features, and User Stories

## Document Information

| Attribute | Value |
|-----------|-------|
| Document Title | CardDemo Agile Requirements |
| Version | 1.0 |
| Date | January 2026 |
| Purpose | Agile Backlog for Modernization |
| Methodology | Agile/Scrum |

---

## 1. Document Overview

This document contains the complete set of Epics, Features, and User Stories for the CardDemo credit card management application. The requirements are derived from the Functional Specification Document and Technical Specification Document through reverse engineering of the existing mainframe application.

### 1.1 Hierarchy Structure

```
Epic (Large body of work spanning multiple sprints)
  |
  +-- Feature (Deliverable capability in 1-3 sprints)
        |
        +-- User Story (Small, testable piece of functionality)
              |
              +-- Acceptance Criteria (Given/When/Then)
```

### 1.2 Story Point Scale

| Points | Complexity | Effort |
|--------|------------|--------|
| 1 | Trivial | Few hours |
| 2 | Simple | 1 day |
| 3 | Moderate | 2-3 days |
| 5 | Complex | 1 week |
| 8 | Very Complex | 1-2 weeks |
| 13 | Highly Complex | 2+ weeks |

### 1.3 Priority Scale

| Priority | Description |
|----------|-------------|
| P1 - Critical | Must have for MVP |
| P2 - High | Important for release |
| P3 - Medium | Nice to have |
| P4 - Low | Future consideration |

---

## 2. Epic Summary

| Epic ID | Epic Name | Features | Stories | Priority |
|---------|-----------|----------|---------|----------|
| EPIC-001 | User Authentication & Security | 3 | 12 | P1 |
| EPIC-002 | Account Management | 2 | 8 | P1 |
| EPIC-003 | Card Management | 3 | 12 | P1 |
| EPIC-004 | Transaction Management | 3 | 14 | P1 |
| EPIC-005 | Bill Payment Processing | 2 | 6 | P1 |
| EPIC-006 | Reporting & Analytics | 2 | 6 | P2 |
| EPIC-007 | User Administration | 4 | 16 | P1 |
| EPIC-008 | Batch Processing | 5 | 15 | P1 |
| EPIC-009 | Authorization Processing | 4 | 12 | P3 |
| EPIC-010 | Transaction Type Management | 3 | 9 | P3 |
| EPIC-011 | System Integration | 2 | 6 | P3 |

**Total: 11 Epics, 33 Features, 116 User Stories**

---

## 3. EPIC-001: User Authentication & Security

### Epic Description
Provide secure authentication and session management for all users accessing the CardDemo application, with role-based access control for regular users and administrators.

### Business Value
Ensures only authorized users can access the system and protects sensitive credit card data from unauthorized access.

### Acceptance Criteria
- Users can securely log in with credentials
- Session information is maintained throughout user interaction
- Users are routed to appropriate menus based on their role
- Invalid login attempts are handled gracefully

### Traceability
- Functions: F-ONL-001, F-ONL-002, F-ONL-003
- Programs: COSGN00C, COMEN01C, COADM01C

---

### Feature 1.1: User Login

**Feature ID:** FEAT-001-01  
**Description:** Enable users to authenticate using User ID and Password  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 1.1.1: Basic User Authentication

**Story ID:** US-001-01-01  
**Title:** User Login with Credentials  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-001, COSGN00C

**User Story:**
> As a CardDemo user, I want to log in with my User ID and Password, so that I can access my credit card account information securely.

**Acceptance Criteria:**

```gherkin
Scenario: Successful login with valid credentials
  Given I am on the CardDemo sign-on screen (CC00)
  When I enter a valid User ID (8 characters)
  And I enter a valid Password (8 characters)
  And I press Enter
  Then I should be authenticated successfully
  And I should be redirected to the appropriate menu based on my user type

Scenario: Failed login with invalid credentials
  Given I am on the CardDemo sign-on screen (CC00)
  When I enter an invalid User ID or Password
  And I press Enter
  Then I should see the error message "Invalid userid and/or password"
  And I should remain on the sign-on screen

Scenario: Login with empty credentials
  Given I am on the CardDemo sign-on screen (CC00)
  When I leave User ID or Password empty
  And I press Enter
  Then I should see a validation error message
  And I should remain on the sign-on screen
```

---

#### User Story 1.1.2: User ID Validation

**Story ID:** US-001-01-02  
**Title:** Validate User ID Format  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-001, COSGN00C, BR-AUTH-001

**User Story:**
> As a system administrator, I want User IDs to be validated for proper format, so that only correctly formatted credentials are accepted.

**Acceptance Criteria:**

```gherkin
Scenario: Valid User ID format
  Given I am on the sign-on screen
  When I enter a User ID that is exactly 8 characters
  Then the User ID should be accepted for authentication

Scenario: Invalid User ID format - too short
  Given I am on the sign-on screen
  When I enter a User ID that is less than 8 characters
  Then I should see a validation error
  And authentication should not proceed

Scenario: Invalid User ID format - too long
  Given I am on the sign-on screen
  When I enter a User ID that is more than 8 characters
  Then only the first 8 characters should be accepted
```

---

#### User Story 1.1.3: Password Validation

**Story ID:** US-001-01-03  
**Title:** Validate Password Format  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-001, COSGN00C, BR-AUTH-001

**User Story:**
> As a system administrator, I want Passwords to be validated for proper format, so that security standards are maintained.

**Acceptance Criteria:**

```gherkin
Scenario: Valid Password format
  Given I am on the sign-on screen
  When I enter a Password that is exactly 8 characters
  Then the Password should be accepted for authentication

Scenario: Invalid Password format
  Given I am on the sign-on screen
  When I enter a Password that is not 8 characters
  Then I should see a validation error
```

---

### Feature 1.2: Role-Based Routing

**Feature ID:** FEAT-001-02  
**Description:** Route authenticated users to appropriate menus based on their user type  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 1.2.1: Regular User Routing

**Story ID:** US-001-02-01  
**Title:** Route Regular Users to Main Menu  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-001, F-ONL-002, BR-AUTH-002

**User Story:**
> As a regular user (Type 'U'), I want to be directed to the Main Menu after login, so that I can access card management functions.

**Acceptance Criteria:**

```gherkin
Scenario: Regular user routing
  Given I am a user with user type 'U'
  When I successfully authenticate
  Then I should be redirected to the Main Menu (CM00)
  And I should see options for Account, Card, Transaction, Bill Payment, and Reports
```

---

#### User Story 1.2.2: Administrator Routing

**Story ID:** US-001-02-02  
**Title:** Route Administrators to Admin Menu  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-001, F-ONL-003, BR-AUTH-002

**User Story:**
> As an administrator (Type 'A'), I want to be directed to the Admin Menu after login, so that I can access administrative functions.

**Acceptance Criteria:**

```gherkin
Scenario: Administrator routing
  Given I am a user with user type 'A'
  When I successfully authenticate
  Then I should be redirected to the Admin Menu (CA00)
  And I should see options for User Management and system administration
```

---

### Feature 1.3: Session Management

**Feature ID:** FEAT-001-03  
**Description:** Maintain user session information throughout the application  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 1.3.1: Session State Persistence

**Story ID:** US-001-03-01  
**Title:** Maintain Session Information  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-001, COCOM01Y, BR-AUTH-003

**User Story:**
> As a logged-in user, I want my session information to be maintained as I navigate through the application, so that I don't have to re-authenticate for each screen.

**Acceptance Criteria:**

```gherkin
Scenario: Session persistence across screens
  Given I am logged in as a valid user
  When I navigate from one screen to another
  Then my User ID should be preserved
  And my User Type should be preserved
  And my navigation context should be maintained

Scenario: Session data in COMMAREA
  Given I am logged in
  When the system processes my request
  Then CDEMO-USER-ID should contain my User ID
  And CDEMO-USER-TYPE should contain my user type ('A' or 'U')
  And CDEMO-FROM-TRANID should contain the source transaction
```

---

#### User Story 1.3.2: Navigation Context

**Story ID:** US-001-03-02  
**Title:** Track Navigation Context  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** COCOM01Y, BR-AUTH-003

**User Story:**
> As a user, I want the system to track where I came from, so that I can return to previous screens correctly.

**Acceptance Criteria:**

```gherkin
Scenario: Return to previous screen
  Given I navigated from the Main Menu to Account View
  When I press PF3 (Exit)
  Then I should return to the Main Menu
  And my session should remain active
```

---

#### User Story 1.3.3: Program Context Management

**Story ID:** US-001-03-03  
**Title:** Manage Program Entry/Re-entry Context  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** COCOM01Y

**User Story:**
> As a system, I need to distinguish between initial entry and re-entry to a program, so that I can handle user input appropriately.

**Acceptance Criteria:**

```gherkin
Scenario: Initial program entry
  Given I am entering a program for the first time
  Then CDEMO-PGM-CONTEXT should be set to 0 (Enter)
  And the screen should be initialized with default values

Scenario: Program re-entry after user input
  Given I have submitted data on a screen
  When the program processes my input
  Then CDEMO-PGM-CONTEXT should be set to 1 (Reenter)
  And my input should be validated
```

---

#### User Story 1.3.4: User Logout

**Story ID:** US-001-03-04  
**Title:** Secure User Logout  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-001

**User Story:**
> As a user, I want to securely log out of the application, so that my session is terminated and my data is protected.

**Acceptance Criteria:**

```gherkin
Scenario: User logout
  Given I am logged in to the application
  When I press PF3 from the Main Menu or Admin Menu
  Then I should be logged out
  And I should be returned to the sign-on screen
  And my session data should be cleared
```

---

## 4. EPIC-002: Account Management

### Epic Description
Enable users to view and update credit card account information including balances, credit limits, and account details.

### Business Value
Allows cardholders and customer service representatives to access and maintain accurate account information.

### Acceptance Criteria
- Users can view account details
- Authorized users can update account information
- Account data is validated before updates
- Changes are persisted to the account master file

### Traceability
- Functions: F-ONL-004, F-ONL-005
- Programs: COACTVWC, COACTUPC

---

### Feature 2.1: Account View

**Feature ID:** FEAT-002-01  
**Description:** Display account details including balances and credit limits  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 2.1.1: View Account Details

**Story ID:** US-002-01-01  
**Title:** Display Account Information  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-004, COACTVWC

**User Story:**
> As a cardholder, I want to view my account details, so that I can see my current balance, credit limit, and account status.

**Acceptance Criteria:**

```gherkin
Scenario: View account details
  Given I am logged in as a valid user
  And I select the Account View option (CAVW)
  When I enter a valid Account ID (11 digits)
  And I press Enter
  Then I should see the account details including:
    | Field | Description |
    | Account ID | 11-digit account number |
    | Active Status | Y or N |
    | Current Balance | Current amount owed |
    | Credit Limit | Maximum credit allowed |
    | Cash Credit Limit | Maximum cash advance allowed |
    | Open Date | Account opening date |
    | Expiration Date | Account expiration date |

Scenario: Account not found
  Given I am on the Account View screen
  When I enter an Account ID that does not exist
  And I press Enter
  Then I should see the error message "Account ID NOT found..."
```

---

#### User Story 2.1.2: View Associated Customer Information

**Story ID:** US-002-01-02  
**Title:** Display Customer Details with Account  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** F-ONL-004, COACTVWC

**User Story:**
> As a customer service representative, I want to see customer information along with account details, so that I can verify the account holder's identity.

**Acceptance Criteria:**

```gherkin
Scenario: View customer with account
  Given I am viewing an account
  Then I should also see the associated customer information:
    | Field | Description |
    | Customer ID | 9-digit customer number |
    | Customer Name | First, Middle, Last name |
    | Address | Full mailing address |
    | Phone Numbers | Contact phone numbers |
```

---

#### User Story 2.1.3: Account ID Validation

**Story ID:** US-002-01-03  
**Title:** Validate Account ID Format  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-004, BR-ACCT-001

**User Story:**
> As a system, I need to validate Account ID format, so that only valid account lookups are performed.

**Acceptance Criteria:**

```gherkin
Scenario: Valid Account ID
  Given I am on the Account View screen
  When I enter an Account ID that is exactly 11 numeric digits
  Then the lookup should proceed

Scenario: Invalid Account ID - non-numeric
  Given I am on the Account View screen
  When I enter an Account ID containing non-numeric characters
  Then I should see a validation error
  And the lookup should not proceed
```

---

### Feature 2.2: Account Update

**Feature ID:** FEAT-002-02  
**Description:** Allow authorized users to modify account information  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 2.2.1: Update Account Information

**Story ID:** US-002-02-01  
**Title:** Modify Account Details  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-005, COACTUPC

**User Story:**
> As an authorized user, I want to update account information, so that I can correct errors or make approved changes.

**Acceptance Criteria:**

```gherkin
Scenario: Update account successfully
  Given I am on the Account Update screen (CAUP)
  And I have entered a valid Account ID
  When I modify allowed fields
  And I press PF5 to save
  Then the account should be updated in the ACCTDATA file
  And I should see a success confirmation message

Scenario: Update account validation
  Given I am modifying account information
  When I enter invalid data (e.g., credit limit exceeds maximum)
  Then I should see a validation error
  And the update should not be saved
```

---

#### User Story 2.2.2: Credit Limit Modification

**Story ID:** US-002-02-02  
**Title:** Update Credit Limit  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** F-ONL-005, COACTUPC

**User Story:**
> As a credit analyst, I want to modify an account's credit limit, so that I can adjust it based on the customer's creditworthiness.

**Acceptance Criteria:**

```gherkin
Scenario: Increase credit limit
  Given I am on the Account Update screen
  When I increase the credit limit value
  And I save the changes
  Then the new credit limit should be stored
  And the available credit should be recalculated

Scenario: Credit limit validation
  Given I am modifying the credit limit
  When I enter a value that would make current balance exceed the new limit
  Then I should see a warning message
```

---

#### User Story 2.2.3: Account Status Change

**Story ID:** US-002-02-03  
**Title:** Activate or Deactivate Account  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-005, COACTUPC, BR-ACCT-003

**User Story:**
> As an account manager, I want to change an account's active status, so that I can activate new accounts or deactivate closed accounts.

**Acceptance Criteria:**

```gherkin
Scenario: Deactivate account
  Given I am on the Account Update screen
  And the account status is 'Y' (Active)
  When I change the status to 'N' (Inactive)
  And I save the changes
  Then the account should be marked as inactive
  And no new transactions should be allowed on this account

Scenario: Activate account
  Given I am on the Account Update screen
  And the account status is 'N' (Inactive)
  When I change the status to 'Y' (Active)
  And I save the changes
  Then the account should be marked as active
```

---

#### User Story 2.2.4: Navigation from Account Update

**Story ID:** US-002-02-04  
**Title:** Return to Previous Screen  
**Priority:** P2  
**Story Points:** 2  
**Traceability:** F-ONL-005

**User Story:**
> As a user, I want to return to the previous screen without saving, so that I can cancel my changes if needed.

**Acceptance Criteria:**

```gherkin
Scenario: Cancel without saving
  Given I am on the Account Update screen
  And I have made changes but not saved
  When I press PF3 (Exit)
  Then I should return to the Main Menu
  And my changes should not be saved
```

---

## 5. EPIC-003: Card Management

### Epic Description
Enable users to view, list, and update credit card information including card numbers, expiration dates, and card status.

### Business Value
Allows cardholders to manage their credit cards and customer service to assist with card-related inquiries.

### Acceptance Criteria
- Users can list all cards for an account
- Users can view individual card details
- Authorized users can update card information
- Card data is validated before updates

### Traceability
- Functions: F-ONL-006, F-ONL-007, F-ONL-008
- Programs: COCRDLIC, COCRDSLC, COCRDUPC

---

### Feature 3.1: Card List

**Feature ID:** FEAT-003-01  
**Description:** Display a paginated list of cards for an account  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 3.1.1: List Cards for Account

**Story ID:** US-003-01-01  
**Title:** Display Card List  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-006, COCRDLIC

**User Story:**
> As a cardholder, I want to see a list of all cards associated with my account, so that I can manage multiple cards.

**Acceptance Criteria:**

```gherkin
Scenario: View card list
  Given I am logged in and select Card List (CCLI)
  When I enter a valid Account ID
  And I press Enter
  Then I should see a list of cards showing:
    | Field | Description |
    | Card Number | 16-digit card number |
    | Embossed Name | Name on card |
    | Expiration Date | Card expiry date |
    | Status | Active (Y) or Inactive (N) |

Scenario: No cards found
  Given I am on the Card List screen
  When I enter an Account ID with no associated cards
  Then I should see a message indicating no cards found
```

---

#### User Story 3.1.2: Card List Pagination

**Story ID:** US-003-01-02  
**Title:** Navigate Card List Pages  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** F-ONL-006, COCRDLIC

**User Story:**
> As a user with multiple cards, I want to page through the card list, so that I can see all my cards.

**Acceptance Criteria:**

```gherkin
Scenario: Page down in card list
  Given I am viewing a card list with more than 10 cards
  When I press PF8 (Page Down)
  Then I should see the next page of cards

Scenario: Page up in card list
  Given I am on page 2 of the card list
  When I press PF7 (Page Up)
  Then I should see the previous page of cards

Scenario: Already at top
  Given I am on the first page of the card list
  When I press PF7 (Page Up)
  Then I should see the message "You are already at the top of the page..."

Scenario: Already at bottom
  Given I am on the last page of the card list
  When I press PF8 (Page Down)
  Then I should see the message "You are already at the bottom of the page..."
```

---

#### User Story 3.1.3: Select Card from List

**Story ID:** US-003-01-03  
**Title:** Select Card for Detail View  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-006, COCRDLIC

**User Story:**
> As a user, I want to select a card from the list to view its details, so that I can see complete card information.

**Acceptance Criteria:**

```gherkin
Scenario: Select card for details
  Given I am viewing the card list
  When I enter 'S' next to a card
  And I press Enter
  Then I should be taken to the Card Detail screen (CCDL)
  And I should see the full details of the selected card
```

---

### Feature 3.2: Card Detail View

**Feature ID:** FEAT-003-02  
**Description:** Display detailed information for a single card  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 3.2.1: View Card Details

**Story ID:** US-003-02-01  
**Title:** Display Card Information  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-007, COCRDSLC

**User Story:**
> As a cardholder, I want to view the complete details of a specific card, so that I can verify card information.

**Acceptance Criteria:**

```gherkin
Scenario: View card details
  Given I have selected a card from the list
  When the Card Detail screen (CCDL) is displayed
  Then I should see:
    | Field | Description |
    | Card Number | Full 16-digit number |
    | Account ID | Associated account |
    | CVV Code | 3-digit security code |
    | Embossed Name | Name printed on card |
    | Expiration Date | Card expiry date |
    | Active Status | Y or N |
```

---

#### User Story 3.2.2: Navigate from Card Detail

**Story ID:** US-003-02-02  
**Title:** Return to Card List  
**Priority:** P2  
**Story Points:** 2  
**Traceability:** F-ONL-007, COCRDSLC

**User Story:**
> As a user, I want to return to the card list from the detail view, so that I can view other cards.

**Acceptance Criteria:**

```gherkin
Scenario: Return to card list
  Given I am on the Card Detail screen
  When I press PF3 (Exit)
  Then I should return to the Card List screen
  And my previous search results should be preserved
```

---

### Feature 3.3: Card Update

**Feature ID:** FEAT-003-03  
**Description:** Allow authorized users to modify card information  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 3.3.1: Update Card Information

**Story ID:** US-003-03-01  
**Title:** Modify Card Details  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-008, COCRDUPC

**User Story:**
> As an authorized user, I want to update card information, so that I can correct errors or make approved changes.

**Acceptance Criteria:**

```gherkin
Scenario: Update card successfully
  Given I am on the Card Update screen (CCUP)
  When I modify allowed fields (embossed name, status)
  And I press PF5 to save
  Then the card should be updated in the CARDDATA file
  And I should see a success confirmation message
```

---

#### User Story 3.3.2: Validate Embossed Name

**Story ID:** US-003-03-02  
**Title:** Validate Card Name Format  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-008, COCRDUPC, BR-CARD-003

**User Story:**
> As a system, I need to validate the embossed name format, so that only valid names are stored.

**Acceptance Criteria:**

```gherkin
Scenario: Valid embossed name
  Given I am updating a card
  When I enter an embossed name containing only alphabetic characters and spaces
  Then the name should be accepted

Scenario: Invalid embossed name
  Given I am updating a card
  When I enter an embossed name containing numbers or special characters
  Then I should see the error "Card name must contain only alphabets"
  And the update should not be saved
```

---

#### User Story 3.3.3: Update Card Status

**Story ID:** US-003-03-03  
**Title:** Activate or Deactivate Card  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-008, COCRDUPC, BR-CARD-004

**User Story:**
> As a card administrator, I want to change a card's active status, so that I can activate new cards or deactivate lost/stolen cards.

**Acceptance Criteria:**

```gherkin
Scenario: Deactivate card
  Given I am on the Card Update screen
  When I change the status from 'Y' to 'N'
  And I save the changes
  Then the card should be marked as inactive
  And no new transactions should be allowed on this card

Scenario: Invalid status value
  Given I am on the Card Update screen
  When I enter a status value other than 'Y' or 'N'
  Then I should see the error "Card status must be Y or N"
```

---

#### User Story 3.3.4: Card Number Validation

**Story ID:** US-003-03-04  
**Title:** Validate Card Number Format  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-008, BR-CARD-001

**User Story:**
> As a system, I need to validate card number format, so that only valid card numbers are processed.

**Acceptance Criteria:**

```gherkin
Scenario: Valid card number
  Given I am entering a card number
  When I enter exactly 16 digits
  Then the card number should be accepted

Scenario: Invalid card number
  Given I am entering a card number
  When I enter a number that is not 16 digits
  Then I should see the error "Card number must be 16 digits"
```

---

#### User Story 3.3.5: CVV Validation

**Story ID:** US-003-03-05  
**Title:** Validate CVV Format  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-008, BR-CARD-002

**User Story:**
> As a system, I need to validate CVV format, so that only valid security codes are stored.

**Acceptance Criteria:**

```gherkin
Scenario: Valid CVV
  Given I am entering a CVV code
  When I enter exactly 3 digits
  Then the CVV should be accepted

Scenario: Invalid CVV
  Given I am entering a CVV code
  When I enter a value that is not 3 digits
  Then I should see a validation error
```

---

## 6. EPIC-004: Transaction Management

### Epic Description
Enable users to view, list, and add transactions for credit card accounts including purchases, payments, and other transaction types.

### Business Value
Allows cardholders to track their spending and customer service to assist with transaction inquiries.

### Acceptance Criteria
- Users can list transactions for an account
- Users can view individual transaction details
- Users can add new transactions
- Transaction data is validated before saving

### Traceability
- Functions: F-ONL-009, F-ONL-010, F-ONL-011
- Programs: COTRN00C, COTRN01C, COTRN02C

---

### Feature 4.1: Transaction List

**Feature ID:** FEAT-004-01  
**Description:** Display a paginated list of transactions  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 4.1.1: List Transactions

**Story ID:** US-004-01-01  
**Title:** Display Transaction List  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-009, COTRN00C

**User Story:**
> As a cardholder, I want to see a list of transactions on my account, so that I can track my spending.

**Acceptance Criteria:**

```gherkin
Scenario: View transaction list
  Given I am logged in and select Transaction List (CT00)
  When I enter search criteria (Account ID or Card Number)
  And I press Enter
  Then I should see a list of transactions showing:
    | Field | Description |
    | Transaction ID | Unique identifier |
    | Date | Transaction date |
    | Description | Transaction description |
    | Amount | Transaction amount |
    | Type | Transaction type code |
```

---

#### User Story 4.1.2: Transaction List Pagination

**Story ID:** US-004-01-02  
**Title:** Navigate Transaction List Pages  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** F-ONL-009, COTRN00C

**User Story:**
> As a user with many transactions, I want to page through the transaction list, so that I can see all my transactions.

**Acceptance Criteria:**

```gherkin
Scenario: Page through transactions
  Given I am viewing a transaction list with more than one page
  When I press PF8 (Page Down)
  Then I should see the next page of transactions

Scenario: Page up in transactions
  Given I am on page 2 of the transaction list
  When I press PF7 (Page Up)
  Then I should see the previous page of transactions
```

---

#### User Story 4.1.3: Select Transaction for Detail

**Story ID:** US-004-01-03  
**Title:** Select Transaction for Detail View  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-009, COTRN00C

**User Story:**
> As a user, I want to select a transaction from the list to view its details, so that I can see complete transaction information.

**Acceptance Criteria:**

```gherkin
Scenario: Select transaction for details
  Given I am viewing the transaction list
  When I enter 'S' next to a transaction
  And I press Enter
  Then I should be taken to the Transaction View screen (CT01)
```

---

### Feature 4.2: Transaction View

**Feature ID:** FEAT-004-02  
**Description:** Display detailed information for a single transaction  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 4.2.1: View Transaction Details

**Story ID:** US-004-02-01  
**Title:** Display Transaction Information  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-010, COTRN01C

**User Story:**
> As a cardholder, I want to view the complete details of a specific transaction, so that I can verify transaction information.

**Acceptance Criteria:**

```gherkin
Scenario: View transaction details
  Given I have selected a transaction from the list
  When the Transaction View screen (CT01) is displayed
  Then I should see:
    | Field | Description |
    | Transaction ID | 16-character unique ID |
    | Type Code | 2-character type |
    | Category Code | 4-digit category |
    | Source | Transaction source |
    | Description | Full description |
    | Amount | Transaction amount |
    | Merchant ID | Merchant identifier |
    | Merchant Name | Merchant name |
    | Merchant City | Merchant location |
    | Merchant ZIP | Merchant postal code |
    | Card Number | Card used |
    | Original Date | When transaction occurred |
    | Processed Date | When transaction was posted |
```

---

### Feature 4.3: Transaction Add

**Feature ID:** FEAT-004-03  
**Description:** Allow users to add new transactions  
**Priority:** P1 - Critical  
**Sprint Estimate:** 2 Sprints

#### User Story 4.3.1: Add New Transaction

**Story ID:** US-004-03-01  
**Title:** Create New Transaction  
**Priority:** P1  
**Story Points:** 8  
**Traceability:** F-ONL-011, COTRN02C

**User Story:**
> As a customer service representative, I want to add a new transaction to an account, so that I can record purchases, credits, or adjustments.

**Acceptance Criteria:**

```gherkin
Scenario: Add transaction successfully
  Given I am on the Transaction Add screen (CT02)
  When I enter all required fields:
    | Field | Value |
    | Account ID | Valid 11-digit account |
    | Card Number | Valid 16-digit card |
    | Type Code | Valid transaction type |
    | Category Code | Valid category |
    | Amount | Valid amount |
    | Description | Transaction description |
  And I press Enter to submit
  Then a new transaction should be created
  And a unique Transaction ID should be generated
  And I should see a success message
```

---

#### User Story 4.3.2: Validate Transaction Amount

**Story ID:** US-004-03-02  
**Title:** Validate Amount Format  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-011, COTRN02C, BR-TRAN-002

**User Story:**
> As a system, I need to validate transaction amount format, so that only valid amounts are recorded.

**Acceptance Criteria:**

```gherkin
Scenario: Valid amount format
  Given I am adding a transaction
  When I enter an amount in format +99999999.99 or -99999999.99
  Then the amount should be accepted

Scenario: Invalid amount format
  Given I am adding a transaction
  When I enter an amount in an invalid format
  Then I should see the error "Amount format invalid"
```

---

#### User Story 4.3.3: Validate Card for Transaction

**Story ID:** US-004-03-03  
**Title:** Validate Card is Active  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-011, COTRN02C, BR-TRAN-003

**User Story:**
> As a system, I need to validate that the card is active before allowing a transaction, so that inactive cards cannot be used.

**Acceptance Criteria:**

```gherkin
Scenario: Transaction on active card
  Given I am adding a transaction
  When I enter a card number for an active card
  Then the transaction should be allowed to proceed

Scenario: Transaction on inactive card
  Given I am adding a transaction
  When I enter a card number for an inactive card
  Then I should see an error message
  And the transaction should not be created
```

---

#### User Story 4.3.4: Credit Limit Check

**Story ID:** US-004-03-04  
**Title:** Validate Against Credit Limit  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-011, COTRN02C, BR-TRAN-002

**User Story:**
> As a system, I need to check that a transaction does not exceed available credit, so that accounts stay within their limits.

**Acceptance Criteria:**

```gherkin
Scenario: Transaction within credit limit
  Given I am adding a purchase transaction
  And the account has sufficient available credit
  When I submit the transaction
  Then the transaction should be created

Scenario: Transaction exceeds credit limit
  Given I am adding a purchase transaction
  And the transaction amount exceeds available credit
  When I submit the transaction
  Then I should see an error about insufficient credit
  And the transaction should not be created
```

---

#### User Story 4.3.5: Transaction ID Generation

**Story ID:** US-004-03-05  
**Title:** Auto-Generate Transaction ID  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-011, COTRN02C, BR-TRAN-001

**User Story:**
> As a system, I need to automatically generate unique transaction IDs, so that each transaction can be uniquely identified.

**Acceptance Criteria:**

```gherkin
Scenario: Generate unique transaction ID
  Given I am creating a new transaction
  When the transaction is saved
  Then a unique 16-character Transaction ID should be generated
  And the ID should not duplicate any existing transaction
```

---

#### User Story 4.3.6: Validate Type and Category Codes

**Story ID:** US-004-03-06  
**Title:** Validate Reference Codes  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-011, COTRN02C, BR-TRAN-004

**User Story:**
> As a system, I need to validate that type and category codes exist, so that only valid codes are used.

**Acceptance Criteria:**

```gherkin
Scenario: Valid type and category codes
  Given I am adding a transaction
  When I enter a type code that exists in the Transaction Type file
  And I enter a category code that exists in the Transaction Category file
  Then the codes should be accepted

Scenario: Invalid type code
  Given I am adding a transaction
  When I enter a type code that does not exist
  Then I should see a validation error
```

---

## 7. EPIC-005: Bill Payment Processing

### Epic Description
Enable cardholders to make payments on their credit card accounts to reduce their balance.

### Business Value
Allows cardholders to pay their bills and maintain their accounts in good standing.

### Acceptance Criteria
- Users can initiate bill payments
- Payment amounts are validated
- Payments are recorded as transactions
- Account balances are updated

### Traceability
- Functions: F-ONL-012
- Programs: COBIL00C

---

### Feature 5.1: Bill Payment Entry

**Feature ID:** FEAT-005-01  
**Description:** Allow users to enter and submit bill payments  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 5.1.1: Make Bill Payment

**Story ID:** US-005-01-01  
**Title:** Process Bill Payment  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-012, COBIL00C

**User Story:**
> As a cardholder, I want to make a payment on my credit card account, so that I can reduce my balance and avoid interest charges.

**Acceptance Criteria:**

```gherkin
Scenario: Successful bill payment
  Given I am on the Bill Payment screen (CB00)
  When I enter a valid Account ID
  And I enter a payment amount
  And I press Enter to submit
  Then the payment should be processed
  And a payment transaction should be created
  And my account balance should be reduced by the payment amount
  And I should see a confirmation message
```

---

#### User Story 5.1.2: Validate Payment Amount

**Story ID:** US-005-01-02  
**Title:** Validate Payment Amount  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-012, COBIL00C

**User Story:**
> As a system, I need to validate payment amounts, so that only valid payments are processed.

**Acceptance Criteria:**

```gherkin
Scenario: Valid payment amount
  Given I am making a bill payment
  When I enter a positive payment amount
  Then the payment should be accepted

Scenario: Invalid payment amount - zero
  Given I am making a bill payment
  When I enter zero as the payment amount
  Then I should see a validation error

Scenario: Invalid payment amount - negative
  Given I am making a bill payment
  When I enter a negative payment amount
  Then I should see a validation error
```

---

#### User Story 5.1.3: Display Account Balance

**Story ID:** US-005-01-03  
**Title:** Show Current Balance Before Payment  
**Priority:** P2  
**Story Points:** 2  
**Traceability:** F-ONL-012, COBIL00C

**User Story:**
> As a cardholder, I want to see my current balance before making a payment, so that I know how much I owe.

**Acceptance Criteria:**

```gherkin
Scenario: Display balance on payment screen
  Given I am on the Bill Payment screen
  When I enter my Account ID
  Then I should see my current balance displayed
  And I should see my minimum payment due
  And I should see my payment due date
```

---

### Feature 5.2: Payment Confirmation

**Feature ID:** FEAT-005-02  
**Description:** Confirm payment processing and update account  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 5.2.1: Payment Confirmation

**Story ID:** US-005-02-01  
**Title:** Confirm Payment Processed  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-012, COBIL00C

**User Story:**
> As a cardholder, I want to receive confirmation that my payment was processed, so that I have a record of the payment.

**Acceptance Criteria:**

```gherkin
Scenario: Payment confirmation
  Given I have submitted a valid payment
  When the payment is processed successfully
  Then I should see a confirmation message
  And I should see the new account balance
  And a payment transaction should be recorded
```

---

#### User Story 5.2.2: Update Account Balance

**Story ID:** US-005-02-02  
**Title:** Reduce Balance After Payment  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-012, COBIL00C

**User Story:**
> As a system, I need to update the account balance after a payment, so that the balance reflects the payment.

**Acceptance Criteria:**

```gherkin
Scenario: Balance update after payment
  Given a payment of $500 is processed
  And the account balance was $1000
  When the payment is complete
  Then the account balance should be $500
  And the current cycle credit should be updated
```

---

## 8. EPIC-006: Reporting & Analytics

### Epic Description
Enable users to generate and view reports on transaction activity and account information.

### Business Value
Provides cardholders and administrators with insights into account activity and spending patterns.

### Acceptance Criteria
- Users can generate transaction reports
- Reports can be filtered by date range
- Reports display summary and detail information

### Traceability
- Functions: F-ONL-013, F-BAT-010
- Programs: CORPT00C, CBTRN03C

---

### Feature 6.1: Online Transaction Reports

**Feature ID:** FEAT-006-01  
**Description:** Generate transaction reports through the online interface  
**Priority:** P2 - High  
**Sprint Estimate:** 1 Sprint

#### User Story 6.1.1: Generate Transaction Report

**Story ID:** US-006-01-01  
**Title:** Create Transaction Report  
**Priority:** P2  
**Story Points:** 5  
**Traceability:** F-ONL-013, CORPT00C

**User Story:**
> As a cardholder, I want to generate a report of my transactions, so that I can review my spending history.

**Acceptance Criteria:**

```gherkin
Scenario: Generate transaction report
  Given I am on the Transaction Reports screen (CR00)
  When I enter search criteria (Account ID, date range)
  And I press Enter
  Then I should see a report of transactions matching my criteria
  And the report should show transaction details and totals
```

---

#### User Story 6.1.2: Filter Report by Date Range

**Story ID:** US-006-01-02  
**Title:** Filter Transactions by Date  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** F-ONL-013, CORPT00C

**User Story:**
> As a user, I want to filter my transaction report by date range, so that I can see transactions for a specific period.

**Acceptance Criteria:**

```gherkin
Scenario: Filter by date range
  Given I am generating a transaction report
  When I enter a start date and end date
  Then only transactions within that date range should be displayed
```

---

### Feature 6.2: Batch Transaction Reports

**Feature ID:** FEAT-006-02  
**Description:** Generate detailed transaction reports through batch processing  
**Priority:** P2 - High  
**Sprint Estimate:** 1 Sprint

#### User Story 6.2.1: Batch Report Generation

**Story ID:** US-006-02-01  
**Title:** Generate Batch Transaction Report  
**Priority:** P2  
**Story Points:** 5  
**Traceability:** F-BAT-010, CBTRN03C

**User Story:**
> As a system administrator, I want to generate batch transaction reports, so that I can produce detailed reports for analysis.

**Acceptance Criteria:**

```gherkin
Scenario: Generate batch report
  Given the TRANREPT job is submitted
  When the job completes successfully
  Then a detailed transaction report should be generated
  And the report should include headers, detail lines, and totals
```

---

#### User Story 6.2.2: Report Output Format

**Story ID:** US-006-02-02  
**Title:** Format Report Output  
**Priority:** P3  
**Story Points:** 3  
**Traceability:** F-BAT-010, CVTRA07Y

**User Story:**
> As a report consumer, I want reports to be formatted consistently, so that they are easy to read and analyze.

**Acceptance Criteria:**

```gherkin
Scenario: Report formatting
  Given a transaction report is generated
  Then it should include:
    | Section | Content |
    | Header | Report title, date, account info |
    | Detail | Individual transaction lines |
    | Summary | Totals by category and type |
    | Footer | Page numbers, end of report |
```

---

## 9. EPIC-007: User Administration

### Epic Description
Enable administrators to manage system users including creating, updating, and deleting user accounts.

### Business Value
Allows system administrators to control access to the CardDemo application and manage user credentials.

### Acceptance Criteria
- Administrators can list all users
- Administrators can add new users
- Administrators can update user information
- Administrators can delete users

### Traceability
- Functions: F-ONL-014, F-ONL-015, F-ONL-016, F-ONL-017
- Programs: COUSR00C, COUSR01C, COUSR02C, COUSR03C

---

### Feature 7.1: User List

**Feature ID:** FEAT-007-01  
**Description:** Display a paginated list of system users  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 7.1.1: List All Users

**Story ID:** US-007-01-01  
**Title:** Display User List  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-014, COUSR00C

**User Story:**
> As an administrator, I want to see a list of all system users, so that I can manage user accounts.

**Acceptance Criteria:**

```gherkin
Scenario: View user list
  Given I am logged in as an administrator
  And I select User List (CU00) from the Admin Menu
  Then I should see a list of users showing:
    | Field | Description |
    | User ID | 8-character user ID |
    | First Name | User's first name |
    | Last Name | User's last name |
    | User Type | A (Admin) or U (User) |
```

---

#### User Story 7.1.2: User List Pagination

**Story ID:** US-007-01-02  
**Title:** Navigate User List Pages  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** F-ONL-014, COUSR00C

**User Story:**
> As an administrator, I want to page through the user list, so that I can see all users.

**Acceptance Criteria:**

```gherkin
Scenario: Page through users
  Given I am viewing a user list with more than 10 users
  When I press PF8 (Page Down)
  Then I should see the next page of users
```

---

#### User Story 7.1.3: Select User for Action

**Story ID:** US-007-01-03  
**Title:** Select User for Update or Delete  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-014, COUSR00C

**User Story:**
> As an administrator, I want to select a user from the list to update or delete, so that I can manage individual users.

**Acceptance Criteria:**

```gherkin
Scenario: Select user for update
  Given I am viewing the user list
  When I enter 'U' next to a user
  And I press Enter
  Then I should be taken to the User Update screen (CU02)

Scenario: Select user for delete
  Given I am viewing the user list
  When I enter 'D' next to a user
  And I press Enter
  Then I should be taken to the User Delete screen (CU03)
```

---

### Feature 7.2: User Add

**Feature ID:** FEAT-007-02  
**Description:** Allow administrators to create new user accounts  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 7.2.1: Add New User

**Story ID:** US-007-02-01  
**Title:** Create New User Account  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-015, COUSR01C

**User Story:**
> As an administrator, I want to add a new user to the system, so that new employees can access the application.

**Acceptance Criteria:**

```gherkin
Scenario: Add user successfully
  Given I am on the User Add screen (CU01)
  When I enter all required fields:
    | Field | Value |
    | User ID | 8 characters |
    | First Name | Up to 20 characters |
    | Last Name | Up to 20 characters |
    | Password | 8 characters |
    | User Type | A or U |
  And I press Enter to submit
  Then the new user should be created
  And I should see a success message
```

---

#### User Story 7.2.2: Validate User ID Uniqueness

**Story ID:** US-007-02-02  
**Title:** Check for Duplicate User ID  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-015, COUSR01C

**User Story:**
> As a system, I need to ensure User IDs are unique, so that each user has a distinct identifier.

**Acceptance Criteria:**

```gherkin
Scenario: Unique User ID
  Given I am adding a new user
  When I enter a User ID that does not exist
  Then the user should be created

Scenario: Duplicate User ID
  Given I am adding a new user
  When I enter a User ID that already exists
  Then I should see the error "User ID already exist..."
  And the user should not be created
```

---

#### User Story 7.2.3: Validate Required Fields

**Story ID:** US-007-02-03  
**Title:** Require All User Fields  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-015, COUSR01C

**User Story:**
> As a system, I need to ensure all required fields are provided, so that user records are complete.

**Acceptance Criteria:**

```gherkin
Scenario: Missing required field
  Given I am adding a new user
  When I leave any required field empty
  Then I should see the error "[Field] can NOT be empty..."
  And the user should not be created
```

---

### Feature 7.3: User Update

**Feature ID:** FEAT-007-03  
**Description:** Allow administrators to modify user information  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 7.3.1: Update User Information

**Story ID:** US-007-03-01  
**Title:** Modify User Details  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-ONL-016, COUSR02C

**User Story:**
> As an administrator, I want to update user information, so that I can correct errors or change user details.

**Acceptance Criteria:**

```gherkin
Scenario: Update user successfully
  Given I am on the User Update screen (CU02)
  And I have selected a user to update
  When I modify user fields (name, password, type)
  And I press PF5 to save
  Then the user should be updated
  And I should see a success message
```

---

#### User Story 7.3.2: Change User Password

**Story ID:** US-007-03-02  
**Title:** Reset User Password  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-016, COUSR02C

**User Story:**
> As an administrator, I want to reset a user's password, so that users who forgot their password can regain access.

**Acceptance Criteria:**

```gherkin
Scenario: Reset password
  Given I am updating a user
  When I enter a new password (8 characters)
  And I save the changes
  Then the user's password should be updated
  And the user should be able to log in with the new password
```

---

#### User Story 7.3.3: Change User Type

**Story ID:** US-007-03-03  
**Title:** Promote or Demote User  
**Priority:** P2  
**Story Points:** 2  
**Traceability:** F-ONL-016, COUSR02C

**User Story:**
> As an administrator, I want to change a user's type, so that I can grant or revoke administrative privileges.

**Acceptance Criteria:**

```gherkin
Scenario: Promote user to admin
  Given I am updating a user with type 'U'
  When I change the type to 'A'
  And I save the changes
  Then the user should have administrator access

Scenario: Demote admin to user
  Given I am updating a user with type 'A'
  When I change the type to 'U'
  And I save the changes
  Then the user should have regular user access only
```

---

### Feature 7.4: User Delete

**Feature ID:** FEAT-007-04  
**Description:** Allow administrators to remove user accounts  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 7.4.1: Delete User

**Story ID:** US-007-04-01  
**Title:** Remove User Account  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-ONL-017, COUSR03C

**User Story:**
> As an administrator, I want to delete a user from the system, so that former employees no longer have access.

**Acceptance Criteria:**

```gherkin
Scenario: Delete user successfully
  Given I am on the User Delete screen (CU03)
  And I have selected a user to delete
  When I press PF5 to confirm deletion
  Then the user should be removed from the system
  And I should see a success message
  And the user should no longer be able to log in
```

---

#### User Story 7.4.2: Confirm User Deletion

**Story ID:** US-007-04-02  
**Title:** Require Deletion Confirmation  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-ONL-017, COUSR03C

**User Story:**
> As an administrator, I want to confirm before deleting a user, so that I don't accidentally delete the wrong user.

**Acceptance Criteria:**

```gherkin
Scenario: Confirm deletion
  Given I am on the User Delete screen
  When the user details are displayed
  Then I should see a prompt to confirm deletion
  And I must press PF5 to confirm
  And pressing PF3 should cancel without deleting
```

---

#### User Story 7.4.3: User Not Found

**Story ID:** US-007-04-03  
**Title:** Handle User Not Found  
**Priority:** P2  
**Story Points:** 2  
**Traceability:** F-ONL-017, COUSR03C

**User Story:**
> As a system, I need to handle cases where a user to delete is not found, so that appropriate feedback is provided.

**Acceptance Criteria:**

```gherkin
Scenario: User not found
  Given I am trying to delete a user
  When the User ID does not exist
  Then I should see the error "User ID NOT found..."
```

---

## 10. EPIC-008: Batch Processing

### Epic Description
Provide batch processing capabilities for daily transaction posting, interest calculation, statement generation, and data management.

### Business Value
Automates end-of-day processing and ensures accurate account maintenance.

### Acceptance Criteria
- Daily transactions are posted to master files
- Interest is calculated and applied monthly
- Statements are generated for customers
- Data can be exported and imported for migration

### Traceability
- Functions: F-BAT-001 through F-BAT-014
- Programs: CBTRN02C, CBACT04C, CBSTM03A/B, CBEXPORT, CBIMPORT, etc.

---

### Feature 8.1: Transaction Posting

**Feature ID:** FEAT-008-01  
**Description:** Post daily transactions to master files  
**Priority:** P1 - Critical  
**Sprint Estimate:** 2 Sprints

#### User Story 8.1.1: Post Daily Transactions

**Story ID:** US-008-01-01  
**Title:** Process Transaction Posting Job  
**Priority:** P1  
**Story Points:** 8  
**Traceability:** F-BAT-001, CBTRN02C

**User Story:**
> As a batch operator, I want to run the transaction posting job, so that daily transactions are applied to account balances.

**Acceptance Criteria:**

```gherkin
Scenario: Successful transaction posting
  Given the POSTTRAN job is submitted
  And the daily transaction file (DALYTRAN) contains transactions
  When the job completes successfully
  Then all valid transactions should be posted to TRANSACT
  And account balances should be updated in ACCTDATA
  And category balances should be updated in TCATBALF
  And the job should return code 0
```

---

#### User Story 8.1.2: Validate Transactions Before Posting

**Story ID:** US-008-01-02  
**Title:** Validate Daily Transactions  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-BAT-009, CBTRN01C

**User Story:**
> As a system, I need to validate transactions before posting, so that only valid transactions are applied.

**Acceptance Criteria:**

```gherkin
Scenario: Transaction validation
  Given a daily transaction is being processed
  When the card number is looked up in XREFFILE
  And the account is looked up in ACCTFILE
  Then valid transactions should proceed to posting
  And invalid transactions should be written to DALYREJS with reason codes
```

---

#### User Story 8.1.3: Handle Rejected Transactions

**Story ID:** US-008-01-03  
**Title:** Write Rejected Transactions  
**Priority:** P1  
**Story Points:** 3  
**Traceability:** F-BAT-001, CBTRN02C

**User Story:**
> As a batch operator, I want rejected transactions to be logged, so that I can investigate and correct issues.

**Acceptance Criteria:**

```gherkin
Scenario: Rejected transaction handling
  Given a transaction fails validation
  Then it should be written to the DALYREJS file
  And the rejection reason should be included
  And the job should continue processing remaining transactions
```

---

### Feature 8.2: Interest Calculation

**Feature ID:** FEAT-008-02  
**Description:** Calculate and apply monthly interest charges  
**Priority:** P1 - Critical  
**Sprint Estimate:** 2 Sprints

#### User Story 8.2.1: Calculate Monthly Interest

**Story ID:** US-008-02-01  
**Title:** Process Interest Calculation Job  
**Priority:** P1  
**Story Points:** 8  
**Traceability:** F-BAT-002, CBACT04C

**User Story:**
> As a batch operator, I want to run the interest calculation job, so that monthly interest is applied to accounts.

**Acceptance Criteria:**

```gherkin
Scenario: Successful interest calculation
  Given the INTCALC job is submitted
  When the job completes successfully
  Then interest should be calculated for each account
  And interest transactions should be created
  And account balances should be updated
```

---

#### User Story 8.2.2: Apply Interest Rates by Category

**Story ID:** US-008-02-02  
**Title:** Use Disclosure Group Rates  
**Priority:** P1  
**Story Points:** 5  
**Traceability:** F-BAT-002, CBACT04C, BR-INT-002

**User Story:**
> As a system, I need to apply the correct interest rate based on the account's disclosure group and transaction category.

**Acceptance Criteria:**

```gherkin
Scenario: Interest rate lookup
  Given an account belongs to a disclosure group
  When interest is calculated
  Then the rate from DISCGRP for that group and category should be used
  And different rates should apply to purchases vs cash advances
```

---

### Feature 8.3: Statement Generation

**Feature ID:** FEAT-008-03  
**Description:** Generate customer statements  
**Priority:** P2 - High  
**Sprint Estimate:** 1 Sprint

#### User Story 8.3.1: Generate Customer Statements

**Story ID:** US-008-03-01  
**Title:** Process Statement Generation Job  
**Priority:** P2  
**Story Points:** 5  
**Traceability:** F-BAT-003, CBSTM03A/B

**User Story:**
> As a batch operator, I want to run the statement generation job, so that customers receive their monthly statements.

**Acceptance Criteria:**

```gherkin
Scenario: Successful statement generation
  Given the CREASTMT job is submitted
  When the job completes successfully
  Then statements should be generated for all active accounts
  And statements should include transaction history
  And statements should show current balance and minimum payment
```

---

### Feature 8.4: Data Export/Import

**Feature ID:** FEAT-008-04  
**Description:** Export and import data for migration purposes  
**Priority:** P2 - High  
**Sprint Estimate:** 1 Sprint

#### User Story 8.4.1: Export Data

**Story ID:** US-008-04-01  
**Title:** Export Data for Migration  
**Priority:** P2  
**Story Points:** 5  
**Traceability:** F-BAT-004, CBEXPORT

**User Story:**
> As a system administrator, I want to export data from the system, so that it can be migrated to another environment.

**Acceptance Criteria:**

```gherkin
Scenario: Successful data export
  Given the CBEXPORT job is submitted
  When the job completes successfully
  Then all master file data should be exported
  And the export file should be in the defined format
```

---

#### User Story 8.4.2: Import Data

**Story ID:** US-008-04-02  
**Title:** Import Data from Migration  
**Priority:** P2  
**Story Points:** 5  
**Traceability:** F-BAT-005, CBIMPORT

**User Story:**
> As a system administrator, I want to import data into the system, so that migrated data can be loaded.

**Acceptance Criteria:**

```gherkin
Scenario: Successful data import
  Given the CBIMPORT job is submitted
  And the import file contains valid data
  When the job completes successfully
  Then all data should be loaded into the master files
```

---

### Feature 8.5: File Management

**Feature ID:** FEAT-008-05  
**Description:** Manage CICS file access for batch processing  
**Priority:** P1 - Critical  
**Sprint Estimate:** 1 Sprint

#### User Story 8.5.1: Close Files for Batch

**Story ID:** US-008-05-01  
**Title:** Close CICS File Access  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-BAT-006, CLOSEFIL

**User Story:**
> As a batch operator, I want to close CICS file access before batch processing, so that batch jobs have exclusive access to files.

**Acceptance Criteria:**

```gherkin
Scenario: Close files
  Given the CLOSEFIL job is submitted
  When the job completes
  Then CICS should no longer have access to the VSAM files
  And batch jobs can proceed with exclusive access
```

---

#### User Story 8.5.2: Open Files After Batch

**Story ID:** US-008-05-02  
**Title:** Restore CICS File Access  
**Priority:** P1  
**Story Points:** 2  
**Traceability:** F-BAT-007, OPENFIL

**User Story:**
> As a batch operator, I want to restore CICS file access after batch processing, so that online users can access the system.

**Acceptance Criteria:**

```gherkin
Scenario: Open files
  Given the OPENFIL job is submitted
  When the job completes
  Then CICS should have access to the VSAM files
  And online transactions can proceed
```

---

#### User Story 8.5.3: Backup Transaction Files

**Story ID:** US-008-05-03  
**Title:** Create Transaction Backup  
**Priority:** P2  
**Story Points:** 3  
**Traceability:** F-BAT-008, TRANBKP

**User Story:**
> As a batch operator, I want to backup transaction files, so that data can be recovered if needed.

**Acceptance Criteria:**

```gherkin
Scenario: Backup transactions
  Given the TRANBKP job is submitted
  When the job completes
  Then a backup of the transaction file should be created
  And the backup should be stored in the GDG
```

---

## 11. EPIC-009: Authorization Processing (Optional)

### Epic Description
Process credit card authorization requests in real-time using MQ messaging, IMS database storage, and DB2 fraud tracking.

### Business Value
Enables real-time authorization of credit card transactions with fraud detection capabilities.

### Acceptance Criteria
- Authorization requests are received via MQ
- Authorizations are validated and stored in IMS
- Fraudulent transactions can be marked and tracked in DB2
- Expired authorizations are purged

### Traceability
- Functions: F-OPT-001 through F-OPT-005
- Programs: COPAUA0C, COPAUS0C, COPAUS1C, COPAUS2C, CBPAUP0C

---

### Feature 9.1: Authorization Request Processing

**Feature ID:** FEAT-009-01  
**Description:** Process incoming authorization requests via MQ  
**Priority:** P3 - Medium  
**Sprint Estimate:** 2 Sprints

#### User Story 9.1.1: Process Authorization Request

**Story ID:** US-009-01-01  
**Title:** Handle MQ Authorization Request  
**Priority:** P3  
**Story Points:** 8  
**Traceability:** F-OPT-001, COPAUA0C

**User Story:**
> As a merchant system, I want to submit authorization requests via MQ, so that credit card transactions can be approved in real-time.

**Acceptance Criteria:**

```gherkin
Scenario: Successful authorization
  Given an authorization request is received on the MQ request queue
  When the card and account are validated
  And sufficient credit is available
  Then the authorization should be approved
  And the authorization should be stored in IMS
  And a response should be sent to the MQ reply queue
```

---

#### User Story 9.1.2: Decline Authorization

**Story ID:** US-009-01-02  
**Title:** Decline Invalid Authorization  
**Priority:** P3  
**Story Points:** 5  
**Traceability:** F-OPT-001, COPAUA0C

**User Story:**
> As a system, I need to decline authorizations that fail validation, so that invalid transactions are not approved.

**Acceptance Criteria:**

```gherkin
Scenario: Declined authorization
  Given an authorization request is received
  When the card is invalid or insufficient credit is available
  Then the authorization should be declined
  And a decline response should be sent with reason code
```

---

### Feature 9.2: Authorization Summary View

**Feature ID:** FEAT-009-02  
**Description:** Display pending authorizations for an account  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 9.2.1: View Pending Authorizations

**Story ID:** US-009-02-01  
**Title:** Display Authorization Summary  
**Priority:** P3  
**Story Points:** 5  
**Traceability:** F-OPT-002, COPAUS0C

**User Story:**
> As a customer service representative, I want to view pending authorizations for an account, so that I can assist customers with authorization inquiries.

**Acceptance Criteria:**

```gherkin
Scenario: View authorization summary
  Given I am on the Pending Authorization Summary screen (CPVS)
  When I enter an Account ID
  Then I should see a list of pending authorizations from IMS
  And I should see authorization amounts and merchant information
```

---

### Feature 9.3: Authorization Detail View

**Feature ID:** FEAT-009-03  
**Description:** Display detailed authorization information  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 9.3.1: View Authorization Details

**Story ID:** US-009-03-01  
**Title:** Display Authorization Detail  
**Priority:** P3  
**Story Points:** 3  
**Traceability:** F-OPT-003, COPAUS1C

**User Story:**
> As a customer service representative, I want to view detailed authorization information, so that I can investigate specific authorizations.

**Acceptance Criteria:**

```gherkin
Scenario: View authorization details
  Given I have selected an authorization from the summary
  When the Authorization Detail screen (CPVD) is displayed
  Then I should see complete authorization information
  And I should have the option to mark as fraudulent
```

---

### Feature 9.4: Fraud Management

**Feature ID:** FEAT-009-04  
**Description:** Mark and track fraudulent authorizations  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 9.4.1: Mark Authorization as Fraud

**Story ID:** US-009-04-01  
**Title:** Flag Fraudulent Transaction  
**Priority:** P3  
**Story Points:** 5  
**Traceability:** F-OPT-004, COPAUS2C

**User Story:**
> As a fraud analyst, I want to mark an authorization as fraudulent, so that it is logged for investigation.

**Acceptance Criteria:**

```gherkin
Scenario: Mark as fraud
  Given I am viewing an authorization detail
  When I press PF5 to mark as fraud
  Then the authorization should be flagged as fraudulent
  And the fraud record should be inserted into DB2 AUTHFRDS table
```

---

#### User Story 9.4.2: Purge Expired Authorizations

**Story ID:** US-009-04-02  
**Title:** Remove Expired Authorizations  
**Priority:** P3  
**Story Points:** 5  
**Traceability:** F-OPT-005, CBPAUP0C

**User Story:**
> As a batch operator, I want to purge expired authorizations, so that old authorizations don't consume resources.

**Acceptance Criteria:**

```gherkin
Scenario: Purge expired authorizations
  Given the CBPAUP0J job is submitted
  When the job completes
  Then authorizations older than 7 days should be deleted from IMS
  And available credit should be adjusted for unmatched authorizations
```

---

## 12. EPIC-010: Transaction Type Management (Optional)

### Epic Description
Maintain transaction type reference data in DB2 with online and batch management capabilities.

### Business Value
Enables administrators to manage transaction type codes used throughout the application.

### Acceptance Criteria
- Administrators can list transaction types
- Administrators can add/edit transaction types
- Transaction types can be maintained via batch
- Data can be extracted to VSAM for runtime use

### Traceability
- Functions: F-OPT-006 through F-OPT-009
- Programs: COTRTLIC, COTRTUPC, COBTUPDT

---

### Feature 10.1: Transaction Type List

**Feature ID:** FEAT-010-01  
**Description:** Display transaction types from DB2  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 10.1.1: List Transaction Types

**Story ID:** US-010-01-01  
**Title:** Display Transaction Type List  
**Priority:** P3  
**Story Points:** 5  
**Traceability:** F-OPT-006, COTRTLIC

**User Story:**
> As an administrator, I want to view a list of transaction types, so that I can manage the reference data.

**Acceptance Criteria:**

```gherkin
Scenario: View transaction type list
  Given I am on the Transaction Type List screen (CTLI)
  Then I should see a list of transaction types from DB2
  And I should be able to page forward and backward using cursors
```

---

### Feature 10.2: Transaction Type Add/Edit

**Feature ID:** FEAT-010-02  
**Description:** Add or edit transaction types in DB2  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 10.2.1: Add Transaction Type

**Story ID:** US-010-02-01  
**Title:** Create New Transaction Type  
**Priority:** P3  
**Story Points:** 3  
**Traceability:** F-OPT-007, COTRTUPC

**User Story:**
> As an administrator, I want to add a new transaction type, so that new types can be used in the system.

**Acceptance Criteria:**

```gherkin
Scenario: Add transaction type
  Given I am on the Transaction Type Add/Edit screen (CTTU)
  When I enter a new type code and description
  And I save the changes
  Then the new transaction type should be inserted into DB2
```

---

#### User Story 10.2.2: Edit Transaction Type

**Story ID:** US-010-02-02  
**Title:** Modify Transaction Type  
**Priority:** P3  
**Story Points:** 3  
**Traceability:** F-OPT-007, COTRTUPC

**User Story:**
> As an administrator, I want to edit an existing transaction type, so that I can update descriptions.

**Acceptance Criteria:**

```gherkin
Scenario: Edit transaction type
  Given I am editing an existing transaction type
  When I modify the description
  And I save the changes
  Then the transaction type should be updated in DB2
```

---

### Feature 10.3: Transaction Type Batch Maintenance

**Feature ID:** FEAT-010-03  
**Description:** Maintain transaction types via batch processing  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 10.3.1: Batch Update Transaction Types

**Story ID:** US-010-03-01  
**Title:** Process Batch Maintenance  
**Priority:** P3  
**Story Points:** 5  
**Traceability:** F-OPT-008, COBTUPDT

**User Story:**
> As a batch operator, I want to run batch maintenance on transaction types, so that bulk updates can be performed.

**Acceptance Criteria:**

```gherkin
Scenario: Batch maintenance
  Given the MNTTRDB2 job is submitted
  When the job completes
  Then transaction types should be updated in DB2 based on input
```

---

#### User Story 10.3.2: Extract to VSAM

**Story ID:** US-010-03-02  
**Title:** Extract Transaction Types to VSAM  
**Priority:** P3  
**Story Points:** 3  
**Traceability:** F-OPT-009, TRANEXTR

**User Story:**
> As a batch operator, I want to extract transaction types from DB2 to VSAM, so that online programs can use the data.

**Acceptance Criteria:**

```gherkin
Scenario: Extract to VSAM
  Given the TRANEXTR job is submitted
  When the job completes
  Then transaction types should be extracted from DB2
  And VSAM-compatible files should be created
```

---

## 13. EPIC-011: System Integration (Optional)

### Epic Description
Enable integration with external systems via MQ messaging for data extraction and system inquiries.

### Business Value
Allows external systems to query CardDemo data through asynchronous messaging.

### Acceptance Criteria
- System date can be queried via MQ
- Account details can be retrieved via MQ
- Request/response patterns are implemented

### Traceability
- Functions: F-OPT-010, F-OPT-011
- Programs: CODATE01, COACCT01

---

### Feature 11.1: System Date Inquiry

**Feature ID:** FEAT-011-01  
**Description:** Query system date via MQ  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 11.1.1: Inquire System Date

**Story ID:** US-011-01-01  
**Title:** Get System Date via MQ  
**Priority:** P3  
**Story Points:** 3  
**Traceability:** F-OPT-010, CODATE01

**User Story:**
> As an external system, I want to query the system date via MQ, so that I can synchronize dates.

**Acceptance Criteria:**

```gherkin
Scenario: System date inquiry
  Given a date request is sent to the MQ request queue
  When the CDRD transaction processes the request
  Then the current system date should be returned on the response queue
```

---

### Feature 11.2: Account Details Inquiry

**Feature ID:** FEAT-011-02  
**Description:** Query account details via MQ  
**Priority:** P3 - Medium  
**Sprint Estimate:** 1 Sprint

#### User Story 11.2.1: Inquire Account Details

**Story ID:** US-011-02-01  
**Title:** Get Account Details via MQ  
**Priority:** P3  
**Story Points:** 5  
**Traceability:** F-OPT-011, COACCT01

**User Story:**
> As an external system, I want to query account details via MQ, so that I can retrieve account information.

**Acceptance Criteria:**

```gherkin
Scenario: Account details inquiry
  Given an account request with Account ID is sent to the MQ request queue
  When the CDRA transaction processes the request
  Then the account details should be retrieved from VSAM
  And the account data should be returned on the response queue
```

---

## 14. Traceability Matrix

### 14.1 Function to User Story Mapping

| Function ID | Function Name | Epic | Feature | User Stories |
|-------------|---------------|------|---------|--------------|
| F-ONL-001 | User Sign-on | EPIC-001 | FEAT-001-01, FEAT-001-02 | US-001-01-01 to US-001-02-02 |
| F-ONL-002 | Main Menu | EPIC-001 | FEAT-001-02 | US-001-02-01 |
| F-ONL-003 | Admin Menu | EPIC-001 | FEAT-001-02 | US-001-02-02 |
| F-ONL-004 | Account View | EPIC-002 | FEAT-002-01 | US-002-01-01 to US-002-01-03 |
| F-ONL-005 | Account Update | EPIC-002 | FEAT-002-02 | US-002-02-01 to US-002-02-04 |
| F-ONL-006 | Card List | EPIC-003 | FEAT-003-01 | US-003-01-01 to US-003-01-03 |
| F-ONL-007 | Card Detail | EPIC-003 | FEAT-003-02 | US-003-02-01, US-003-02-02 |
| F-ONL-008 | Card Update | EPIC-003 | FEAT-003-03 | US-003-03-01 to US-003-03-05 |
| F-ONL-009 | Transaction List | EPIC-004 | FEAT-004-01 | US-004-01-01 to US-004-01-03 |
| F-ONL-010 | Transaction View | EPIC-004 | FEAT-004-02 | US-004-02-01 |
| F-ONL-011 | Transaction Add | EPIC-004 | FEAT-004-03 | US-004-03-01 to US-004-03-06 |
| F-ONL-012 | Bill Payment | EPIC-005 | FEAT-005-01, FEAT-005-02 | US-005-01-01 to US-005-02-02 |
| F-ONL-013 | Transaction Reports | EPIC-006 | FEAT-006-01 | US-006-01-01, US-006-01-02 |
| F-ONL-014 | User List | EPIC-007 | FEAT-007-01 | US-007-01-01 to US-007-01-03 |
| F-ONL-015 | User Add | EPIC-007 | FEAT-007-02 | US-007-02-01 to US-007-02-03 |
| F-ONL-016 | User Update | EPIC-007 | FEAT-007-03 | US-007-03-01 to US-007-03-03 |
| F-ONL-017 | User Delete | EPIC-007 | FEAT-007-04 | US-007-04-01 to US-007-04-03 |
| F-BAT-001 | Transaction Posting | EPIC-008 | FEAT-008-01 | US-008-01-01 to US-008-01-03 |
| F-BAT-002 | Interest Calculation | EPIC-008 | FEAT-008-02 | US-008-02-01, US-008-02-02 |
| F-BAT-003 | Statement Generation | EPIC-008 | FEAT-008-03 | US-008-03-01 |
| F-BAT-004 | Data Export | EPIC-008 | FEAT-008-04 | US-008-04-01 |
| F-BAT-005 | Data Import | EPIC-008 | FEAT-008-04 | US-008-04-02 |
| F-BAT-006 | File Close | EPIC-008 | FEAT-008-05 | US-008-05-01 |
| F-BAT-007 | File Open | EPIC-008 | FEAT-008-05 | US-008-05-02 |
| F-BAT-008 | Transaction Backup | EPIC-008 | FEAT-008-05 | US-008-05-03 |
| F-BAT-010 | Transaction Report | EPIC-006 | FEAT-006-02 | US-006-02-01, US-006-02-02 |
| F-OPT-001 | Authorization Processing | EPIC-009 | FEAT-009-01 | US-009-01-01, US-009-01-02 |
| F-OPT-002 | Pending Auth Summary | EPIC-009 | FEAT-009-02 | US-009-02-01 |
| F-OPT-003 | Pending Auth Detail | EPIC-009 | FEAT-009-03 | US-009-03-01 |
| F-OPT-004 | Fraud Marking | EPIC-009 | FEAT-009-04 | US-009-04-01 |
| F-OPT-005 | Auth Purge Batch | EPIC-009 | FEAT-009-04 | US-009-04-02 |
| F-OPT-006 | Transaction Type List | EPIC-010 | FEAT-010-01 | US-010-01-01 |
| F-OPT-007 | Transaction Type Add/Edit | EPIC-010 | FEAT-010-02 | US-010-02-01, US-010-02-02 |
| F-OPT-008 | Transaction Type Batch | EPIC-010 | FEAT-010-03 | US-010-03-01 |
| F-OPT-009 | DB2 Data Extract | EPIC-010 | FEAT-010-03 | US-010-03-02 |
| F-OPT-010 | System Date Inquiry | EPIC-011 | FEAT-011-01 | US-011-01-01 |
| F-OPT-011 | Account Details Inquiry | EPIC-011 | FEAT-011-02 | US-011-02-01 |

---

## 15. Appendix: Story Point Summary

### 15.1 By Epic

| Epic | Total Stories | Total Points | Avg Points/Story |
|------|---------------|--------------|------------------|
| EPIC-001 | 12 | 36 | 3.0 |
| EPIC-002 | 8 | 26 | 3.3 |
| EPIC-003 | 12 | 36 | 3.0 |
| EPIC-004 | 14 | 48 | 3.4 |
| EPIC-005 | 6 | 19 | 3.2 |
| EPIC-006 | 6 | 24 | 4.0 |
| EPIC-007 | 16 | 44 | 2.8 |
| EPIC-008 | 15 | 56 | 3.7 |
| EPIC-009 | 12 | 49 | 4.1 |
| EPIC-010 | 9 | 27 | 3.0 |
| EPIC-011 | 6 | 16 | 2.7 |
| **Total** | **116** | **381** | **3.3** |

### 15.2 By Priority

| Priority | Stories | Points | % of Total |
|----------|---------|--------|------------|
| P1 - Critical | 68 | 228 | 60% |
| P2 - High | 24 | 78 | 20% |
| P3 - Medium | 24 | 75 | 20% |

---

*Document generated from CardDemo Functional and Technical Specifications following Agile best practices for modernization planning.*
