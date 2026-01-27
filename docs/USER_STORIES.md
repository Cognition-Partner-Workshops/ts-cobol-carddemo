# CardDemo Application - User Stories for Jira

**Document Version:** 1.0  
**Date:** January 27, 2026  
**Application:** CardDemo - Mainframe Credit Card Management System  
**Purpose:** User stories derived from functional requirements for Jira ticket creation

---

## Table of Contents

1. [Epic Overview](#epic-overview)
2. [Authentication Epic](#epic-1-authentication)
3. [Account Management Epic](#epic-2-account-management)
4. [Credit Card Management Epic](#epic-3-credit-card-management)
5. [Transaction Management Epic](#epic-4-transaction-management)
6. [Bill Payment Epic](#epic-5-bill-payment)
7. [Reporting Epic](#epic-6-reporting)
8. [User Administration Epic](#epic-7-user-administration)
9. [Batch Processing Epic](#epic-8-batch-processing)
10. [Optional Modules Epic](#epic-9-optional-modules)

---

## Epic Overview

| Epic ID | Epic Name | Description | Priority |
|---------|-----------|-------------|----------|
| EPIC-01 | Authentication | User sign-on and session management | High |
| EPIC-02 | Account Management | View and update customer accounts | High |
| EPIC-03 | Credit Card Management | Card listing, viewing, and updates | High |
| EPIC-04 | Transaction Management | Transaction listing, viewing, and creation | High |
| EPIC-05 | Bill Payment | Account balance payment processing | Medium |
| EPIC-06 | Reporting | Transaction report generation | Medium |
| EPIC-07 | User Administration | Admin user management functions | High |
| EPIC-08 | Batch Processing | Daily transaction posting, interest calculation, statements | High |
| EPIC-09 | Optional Modules | IMS-DB2-MQ Authorization, DB2 Transaction Types | Low |

---

## EPIC-1: Authentication

### US-AUTH-001: User Sign-On Screen Display

**Title:** Display Sign-On Screen for User Authentication

**As a** user  
**I want to** see a sign-on screen when I access the CardDemo application  
**So that** I can enter my credentials to authenticate

**Acceptance Criteria:**
- Given I access the CardDemo application
- When the sign-on screen loads
- Then I should see input fields for User ID and Password
- And I should see the current date and time displayed
- And I should see the application title "CardDemo"

**Story Points:** 2  
**Priority:** High  
**Labels:** authentication, online, CICS  
**Component:** COSGN00C

---

### US-AUTH-002: User Authentication Validation

**Title:** Validate User Credentials Against Security File

**As a** user  
**I want to** have my credentials validated when I submit them  
**So that** only authorized users can access the system

**Acceptance Criteria:**
- Given I am on the sign-on screen
- When I enter a valid User ID and Password and press Enter
- Then the system should validate my credentials against the USRSEC file
- And if valid, I should be redirected to the appropriate menu
- And if invalid, I should see an appropriate error message

**Story Points:** 3  
**Priority:** High  
**Labels:** authentication, security, validation  
**Component:** COSGN00C

---

### US-AUTH-003: Empty Credential Validation

**Title:** Validate Required Fields on Sign-On

**As a** user  
**I want to** be notified if I leave required fields empty  
**So that** I know what information is needed to sign on

**Acceptance Criteria:**
- Given I am on the sign-on screen
- When I press Enter without entering a User ID
- Then I should see the message "Please enter User ID ..."
- When I enter a User ID but no Password and press Enter
- Then I should see the message "Please enter Password ..."

**Story Points:** 1  
**Priority:** High  
**Labels:** authentication, validation  
**Component:** COSGN00C

---

### US-AUTH-004: Invalid User ID Error Message

**Title:** Display Error for Invalid User ID

**As a** user  
**I want to** see a clear error message when my User ID is not found  
**So that** I know my User ID is incorrect

**Acceptance Criteria:**
- Given I am on the sign-on screen
- When I enter a User ID that does not exist in the system
- Then I should see the message "User not found. Try again ..."
- And the cursor should be positioned on the User ID field

**Story Points:** 1  
**Priority:** High  
**Labels:** authentication, error-handling  
**Component:** COSGN00C

---

### US-AUTH-005: Invalid Password Error Message

**Title:** Display Error for Incorrect Password

**As a** user  
**I want to** see a clear error message when my password is wrong  
**So that** I know to re-enter my password

**Acceptance Criteria:**
- Given I am on the sign-on screen
- When I enter a valid User ID but incorrect Password
- Then I should see the message "Wrong Password. Try again ..."
- And the cursor should be positioned on the Password field

**Story Points:** 1  
**Priority:** High  
**Labels:** authentication, error-handling  
**Component:** COSGN00C

---

### US-AUTH-006: Route Admin Users to Admin Menu

**Title:** Navigate Admin Users to Admin Menu After Sign-On

**As an** administrator  
**I want to** be directed to the Admin Menu after successful sign-on  
**So that** I can access administrative functions

**Acceptance Criteria:**
- Given I am a user with User Type 'A' (Admin)
- When I successfully authenticate
- Then I should be redirected to the Admin Menu (CA00)
- And I should see administrative menu options

**Story Points:** 2  
**Priority:** High  
**Labels:** authentication, navigation, admin  
**Component:** COSGN00C, COADM01C

---

### US-AUTH-007: Route Regular Users to Main Menu

**Title:** Navigate Regular Users to Main Menu After Sign-On

**As a** regular user  
**I want to** be directed to the Main Menu after successful sign-on  
**So that** I can access card management functions

**Acceptance Criteria:**
- Given I am a user with User Type 'U' (User)
- When I successfully authenticate
- Then I should be redirected to the Main Menu (CM00)
- And I should see regular user menu options

**Story Points:** 2  
**Priority:** High  
**Labels:** authentication, navigation  
**Component:** COSGN00C, COMEN01C

---

### US-AUTH-008: Exit Application with PF3

**Title:** Exit Application from Sign-On Screen

**As a** user  
**I want to** exit the application by pressing PF3  
**So that** I can leave without signing in

**Acceptance Criteria:**
- Given I am on the sign-on screen
- When I press PF3
- Then I should see a "Thank You" message
- And the application session should terminate

**Story Points:** 1  
**Priority:** Medium  
**Labels:** authentication, navigation  
**Component:** COSGN00C

---

## EPIC-2: Account Management

### US-ACCT-001: View Account Details

**Title:** View Account Information by Account ID

**As a** user  
**I want to** view account details by entering an Account ID  
**So that** I can see account information and balances

**Acceptance Criteria:**
- Given I am on the Account View screen (CAVW)
- When I enter a valid 11-digit Account ID and press Enter
- Then I should see the account details including:
  - Account ID and Status
  - Customer ID and Name
  - Credit Limit and Cash Credit Limit
  - Current Balance
  - Current Cycle Credit and Debit
  - Account Open Date, Expiration Date, Reissue Date
  - FICO Score and Account Group ID

**Story Points:** 3  
**Priority:** High  
**Labels:** account, view, online  
**Component:** COACTVWC

---

### US-ACCT-002: Validate Account ID Format

**Title:** Validate Account ID is 11-Digit Numeric

**As a** user  
**I want to** receive validation feedback on Account ID format  
**So that** I enter the correct format

**Acceptance Criteria:**
- Given I am on the Account View screen
- When I enter an Account ID that is not 11 digits
- Then I should see an error message about invalid format
- When I enter an Account ID that is all zeros
- Then I should see an error message

**Story Points:** 2  
**Priority:** High  
**Labels:** account, validation  
**Component:** COACTVWC

---

### US-ACCT-003: Display Account Not Found Error

**Title:** Show Error When Account Does Not Exist

**As a** user  
**I want to** see a clear message when an account is not found  
**So that** I know the Account ID is invalid

**Acceptance Criteria:**
- Given I am on the Account View screen
- When I enter an Account ID that does not exist
- Then I should see the message "Account ID NOT found..."

**Story Points:** 1  
**Priority:** High  
**Labels:** account, error-handling  
**Component:** COACTVWC

---

### US-ACCT-004: Update Account Information

**Title:** Update Account Details

**As a** user  
**I want to** update account information  
**So that** I can modify account settings and balances

**Acceptance Criteria:**
- Given I am on the Account Update screen (CAUP)
- When I modify editable fields and press Enter
- Then the system should validate all input fields
- And update the account record in ACCTDAT file
- And display a success message

**Story Points:** 5  
**Priority:** High  
**Labels:** account, update, online  
**Component:** COACTUPC

---

### US-ACCT-005: Validate Account Status Field

**Title:** Validate Account Status as Y or N

**As a** user  
**I want to** only enter valid account status values  
**So that** data integrity is maintained

**Acceptance Criteria:**
- Given I am updating an account
- When I enter a value other than 'Y' or 'N' for Account Status
- Then I should see a validation error message
- And the update should not proceed

**Story Points:** 1  
**Priority:** High  
**Labels:** account, validation  
**Component:** COACTUPC

---

### US-ACCT-006: Validate Numeric Fields

**Title:** Validate Credit Limits and Balance as Numeric

**As a** user  
**I want to** have numeric fields validated  
**So that** only valid numbers are stored

**Acceptance Criteria:**
- Given I am updating an account
- When I enter non-numeric values for Credit Limit, Cash Credit Limit, or Current Balance
- Then I should see a validation error message
- And the update should not proceed

**Story Points:** 2  
**Priority:** High  
**Labels:** account, validation  
**Component:** COACTUPC

---

### US-ACCT-007: Validate Date Fields

**Title:** Validate Date Fields Format

**As a** user  
**I want to** have date fields validated  
**So that** only valid dates are stored

**Acceptance Criteria:**
- Given I am updating an account
- When I enter invalid dates for Open Date, Expiry Date, or Reissue Date
- Then I should see a validation error message
- And the update should not proceed

**Story Points:** 2  
**Priority:** High  
**Labels:** account, validation  
**Component:** COACTUPC

---

### US-ACCT-008: Validate Phone Number Format

**Title:** Validate US Phone Number Format

**As a** user  
**I want to** have phone numbers validated in (###)###-#### format  
**So that** phone numbers are stored consistently

**Acceptance Criteria:**
- Given I am updating customer information
- When I enter a phone number not in (###)###-#### format
- Then I should see a validation error message

**Story Points:** 2  
**Priority:** Medium  
**Labels:** account, validation  
**Component:** COACTUPC

---

### US-ACCT-009: Validate SSN Format

**Title:** Validate US Social Security Number Format

**As a** user  
**I want to** have SSN validated in ###-##-#### format  
**So that** SSN is stored correctly

**Acceptance Criteria:**
- Given I am updating customer information
- When I enter an SSN not in ###-##-#### format
- Then I should see a validation error message
- When I enter SSN Part 1 as 0, 666, or 900-999
- Then I should see a validation error message

**Story Points:** 2  
**Priority:** Medium  
**Labels:** account, validation, security  
**Component:** COACTUPC

---

### US-ACCT-010: Return Without Saving on PF3

**Title:** Cancel Account Update with PF3

**As a** user  
**I want to** cancel my changes by pressing PF3  
**So that** I can exit without saving modifications

**Acceptance Criteria:**
- Given I am on the Account Update screen with unsaved changes
- When I press PF3
- Then I should return to the previous screen
- And my changes should not be saved

**Story Points:** 1  
**Priority:** Medium  
**Labels:** account, navigation  
**Component:** COACTUPC

---

## EPIC-3: Credit Card Management

### US-CARD-001: Display Credit Card List for Admin

**Title:** Admin Views All Credit Cards

**As an** administrator  
**I want to** see all credit cards in the system  
**So that** I can manage any card

**Acceptance Criteria:**
- Given I am an admin user on the Credit Card List screen (CCLI)
- When the screen loads
- Then I should see all credit cards in the system
- And I should see 7 cards per page
- And each card should show Account Number, Card Number, and Status

**Story Points:** 3  
**Priority:** High  
**Labels:** card, list, admin  
**Component:** COCRDLIC

---

### US-CARD-002: Display Credit Card List for Regular User

**Title:** Regular User Views Own Cards Only

**As a** regular user  
**I want to** see only cards associated with my account  
**So that** I can manage my own cards

**Acceptance Criteria:**
- Given I am a regular user on the Credit Card List screen
- When the screen loads
- Then I should see only cards linked to my account
- And I should not see cards belonging to other users

**Story Points:** 3  
**Priority:** High  
**Labels:** card, list, security  
**Component:** COCRDLIC

---

### US-CARD-003: Paginate Credit Card List

**Title:** Navigate Through Card List Pages

**As a** user  
**I want to** page through the credit card list  
**So that** I can view all my cards

**Acceptance Criteria:**
- Given I am on the Credit Card List screen with more than 7 cards
- When I press PF8
- Then I should see the next page of cards
- When I press PF7
- Then I should see the previous page of cards

**Story Points:** 2  
**Priority:** Medium  
**Labels:** card, pagination  
**Component:** COCRDLIC

---

### US-CARD-004: Select Card to View Details

**Title:** View Card Details by Selection

**As a** user  
**I want to** select a card with 'S' to view its details  
**So that** I can see complete card information

**Acceptance Criteria:**
- Given I am on the Credit Card List screen
- When I enter 'S' next to a card and press Enter
- Then I should be navigated to the Credit Card View screen (CCDL)
- And I should see the selected card's details

**Story Points:** 2  
**Priority:** High  
**Labels:** card, navigation  
**Component:** COCRDLIC, COCRDSLC

---

### US-CARD-005: Select Card to Update

**Title:** Update Card by Selection

**As a** user  
**I want to** select a card with 'U' to update it  
**So that** I can modify card information

**Acceptance Criteria:**
- Given I am on the Credit Card List screen
- When I enter 'U' next to a card and press Enter
- Then I should be navigated to the Credit Card Update screen (CCUP)
- And I should see the selected card's information for editing

**Story Points:** 2  
**Priority:** High  
**Labels:** card, navigation  
**Component:** COCRDLIC, COCRDUPC

---

### US-CARD-006: Prevent Multiple Card Selection

**Title:** Allow Only Single Card Selection

**As a** user  
**I want to** be prevented from selecting multiple cards at once  
**So that** I process one card at a time

**Acceptance Criteria:**
- Given I am on the Credit Card List screen
- When I select more than one card
- Then I should see the message "Please select only one record"
- And no navigation should occur

**Story Points:** 1  
**Priority:** Medium  
**Labels:** card, validation  
**Component:** COCRDLIC

---

### US-CARD-007: View Credit Card Details

**Title:** Display Complete Card Information

**As a** user  
**I want to** view all details of a selected credit card  
**So that** I can see complete card information

**Acceptance Criteria:**
- Given I am on the Credit Card View screen (CCDL)
- Then I should see:
  - Card Number
  - Account ID
  - Customer ID
  - Card Status
  - CVV Code
  - Expiration Date
  - Card Embossed Name

**Story Points:** 2  
**Priority:** High  
**Labels:** card, view  
**Component:** COCRDSLC

---

### US-CARD-008: Update Credit Card Information

**Title:** Modify Credit Card Details

**As a** user  
**I want to** update credit card information  
**So that** I can modify card settings

**Acceptance Criteria:**
- Given I am on the Credit Card Update screen (CCUP)
- When I modify editable fields and press Enter
- Then the system should validate all input fields
- And update the card record in CARDDAT file
- And display a success message

**Story Points:** 3  
**Priority:** High  
**Labels:** card, update  
**Component:** COCRDUPC

---

### US-CARD-009: Validate CVV Code

**Title:** Validate CVV as 3-Digit Numeric

**As a** user  
**I want to** have CVV validated as 3 digits  
**So that** only valid CVV codes are stored

**Acceptance Criteria:**
- Given I am updating a credit card
- When I enter a CVV that is not 3 numeric digits
- Then I should see a validation error message

**Story Points:** 1  
**Priority:** High  
**Labels:** card, validation  
**Component:** COCRDUPC

---

### US-CARD-010: Validate Card Expiration Date

**Title:** Validate Card Expiration Date Format

**As a** user  
**I want to** have expiration date validated  
**So that** only valid dates are stored

**Acceptance Criteria:**
- Given I am updating a credit card
- When I enter an invalid expiration date
- Then I should see a validation error message

**Story Points:** 1  
**Priority:** High  
**Labels:** card, validation  
**Component:** COCRDUPC

---

## EPIC-4: Transaction Management

### US-TRAN-001: Display Transaction List

**Title:** View List of Transactions

**As a** user  
**I want to** see a list of transactions  
**So that** I can review transaction history

**Acceptance Criteria:**
- Given I am on the Transaction List screen (CT00)
- When the screen loads
- Then I should see transactions from the TRANSACT file
- And I should see 10 transactions per page
- And each transaction should show Transaction ID, Date, and Amount

**Story Points:** 3  
**Priority:** High  
**Labels:** transaction, list  
**Component:** COTRN00C

---

### US-TRAN-002: Filter Transactions by ID

**Title:** Filter Transaction List by Transaction ID

**As a** user  
**I want to** filter transactions by Transaction ID  
**So that** I can find specific transactions

**Acceptance Criteria:**
- Given I am on the Transaction List screen
- When I enter a Transaction ID filter value
- Then I should see only transactions matching the filter
- And the filter value must be numeric

**Story Points:** 2  
**Priority:** Medium  
**Labels:** transaction, filter  
**Component:** COTRN00C

---

### US-TRAN-003: Paginate Transaction List

**Title:** Navigate Through Transaction Pages

**As a** user  
**I want to** page through the transaction list  
**So that** I can view all transactions

**Acceptance Criteria:**
- Given I am on the Transaction List screen with more than 10 transactions
- When I press PF8
- Then I should see the next page of transactions
- When I press PF7
- Then I should see the previous page of transactions

**Story Points:** 2  
**Priority:** Medium  
**Labels:** transaction, pagination  
**Component:** COTRN00C

---

### US-TRAN-004: Select Transaction to View Details

**Title:** View Transaction Details by Selection

**As a** user  
**I want to** select a transaction with 'S' to view details  
**So that** I can see complete transaction information

**Acceptance Criteria:**
- Given I am on the Transaction List screen
- When I enter 'S' next to a transaction and press Enter
- Then I should be navigated to the Transaction View screen (CT01)
- And I should see the selected transaction's details

**Story Points:** 2  
**Priority:** High  
**Labels:** transaction, navigation  
**Component:** COTRN00C, COTRN01C

---

### US-TRAN-005: View Transaction Details

**Title:** Display Complete Transaction Information

**As a** user  
**I want to** view all details of a selected transaction  
**So that** I can see complete transaction information

**Acceptance Criteria:**
- Given I am on the Transaction View screen (CT01)
- Then I should see:
  - Transaction ID, Type Code, Category Code
  - Transaction Source and Description
  - Transaction Amount
  - Card Number
  - Merchant ID, Name, City, ZIP
  - Original and Processing Timestamps

**Story Points:** 2  
**Priority:** High  
**Labels:** transaction, view  
**Component:** COTRN01C

---

### US-TRAN-006: Add New Transaction

**Title:** Create New Transaction

**As a** user  
**I want to** add a new transaction  
**So that** I can record new card activity

**Acceptance Criteria:**
- Given I am on the Transaction Add screen (CT02)
- When I enter all required fields and press Enter
- Then the system should generate a unique Transaction ID
- And validate all input fields
- And write the new transaction to TRANSACT file
- And set timestamps automatically

**Story Points:** 5  
**Priority:** High  
**Labels:** transaction, create  
**Component:** COTRN02C

---

### US-TRAN-007: Validate Transaction Type Code

**Title:** Validate Transaction Type Code Exists

**As a** user  
**I want to** have transaction type validated  
**So that** only valid types are used

**Acceptance Criteria:**
- Given I am adding a new transaction
- When I enter an invalid Transaction Type Code
- Then I should see a validation error message

**Story Points:** 1  
**Priority:** High  
**Labels:** transaction, validation  
**Component:** COTRN02C

---

### US-TRAN-008: Validate Transaction Category Code

**Title:** Validate Transaction Category Code Exists

**As a** user  
**I want to** have transaction category validated  
**So that** only valid categories are used

**Acceptance Criteria:**
- Given I am adding a new transaction
- When I enter an invalid Transaction Category Code
- Then I should see a validation error message

**Story Points:** 1  
**Priority:** High  
**Labels:** transaction, validation  
**Component:** COTRN02C

---

### US-TRAN-009: Validate Transaction Amount

**Title:** Validate Transaction Amount is Numeric

**As a** user  
**I want to** have transaction amount validated  
**So that** only valid amounts are recorded

**Acceptance Criteria:**
- Given I am adding a new transaction
- When I enter a non-numeric Transaction Amount
- Then I should see a validation error message

**Story Points:** 1  
**Priority:** High  
**Labels:** transaction, validation  
**Component:** COTRN02C

---

### US-TRAN-010: Validate Card Number Exists

**Title:** Validate Card Number Exists in System

**As a** user  
**I want to** have card number validated  
**So that** transactions are linked to valid cards

**Acceptance Criteria:**
- Given I am adding a new transaction
- When I enter a Card Number that does not exist
- Then I should see a validation error message

**Story Points:** 2  
**Priority:** High  
**Labels:** transaction, validation  
**Component:** COTRN02C

---

## EPIC-5: Bill Payment

### US-BILL-001: Enter Account for Bill Payment

**Title:** Enter Account ID for Bill Payment

**As a** user  
**I want to** enter an Account ID to pay my bill  
**So that** I can pay my account balance

**Acceptance Criteria:**
- Given I am on the Bill Payment screen (CB00)
- When I enter a valid Account ID
- Then the system should retrieve the current balance
- And display the balance to me

**Story Points:** 2  
**Priority:** High  
**Labels:** payment, account  
**Component:** COBIL00C

---

### US-BILL-002: Display Zero Balance Message

**Title:** Show Message When Nothing to Pay

**As a** user  
**I want to** be notified when my balance is zero or negative  
**So that** I know there is nothing to pay

**Acceptance Criteria:**
- Given I am on the Bill Payment screen
- When I enter an Account ID with zero or negative balance
- Then I should see the message "You have nothing to pay..."

**Story Points:** 1  
**Priority:** Medium  
**Labels:** payment, validation  
**Component:** COBIL00C

---

### US-BILL-003: Confirm Bill Payment

**Title:** Require Confirmation Before Processing Payment

**As a** user  
**I want to** confirm my payment before it is processed  
**So that** I don't accidentally pay

**Acceptance Criteria:**
- Given I am on the Bill Payment screen with a positive balance
- When I see my balance displayed
- Then I should be prompted to confirm (Y/N)
- And I must enter 'Y' or 'N' to proceed

**Story Points:** 2  
**Priority:** High  
**Labels:** payment, confirmation  
**Component:** COBIL00C

---

### US-BILL-004: Process Bill Payment

**Title:** Process Payment and Update Account

**As a** user  
**I want to** have my payment processed when I confirm  
**So that** my balance is paid off

**Acceptance Criteria:**
- Given I have confirmed my payment with 'Y'
- When the payment is processed
- Then a bill payment transaction should be created with:
  - Transaction Type: 02
  - Transaction Category: 2
  - Source: POS TERM
  - Description: BILL PAYMENT - ONLINE
  - Amount: Current Balance
- And my account balance should be updated to zero

**Story Points:** 3  
**Priority:** High  
**Labels:** payment, transaction  
**Component:** COBIL00C

---

### US-BILL-005: Validate Confirmation Input

**Title:** Validate Y/N Confirmation Input

**As a** user  
**I want to** be notified if I enter invalid confirmation  
**So that** I enter the correct response

**Acceptance Criteria:**
- Given I am prompted to confirm payment
- When I enter a value other than 'Y' or 'N'
- Then I should see the message "Invalid value. Valid values are (Y/N)..."

**Story Points:** 1  
**Priority:** Medium  
**Labels:** payment, validation  
**Component:** COBIL00C

---

### US-BILL-006: Clear Screen with PF4

**Title:** Clear Bill Payment Screen

**As a** user  
**I want to** clear the screen by pressing PF4  
**So that** I can start over

**Acceptance Criteria:**
- Given I am on the Bill Payment screen
- When I press PF4
- Then the screen should be cleared
- And I can enter a new Account ID

**Story Points:** 1  
**Priority:** Low  
**Labels:** payment, navigation  
**Component:** COBIL00C

---

## EPIC-6: Reporting

### US-REPT-001: Select Monthly Report

**Title:** Generate Monthly Transaction Report

**As a** user  
**I want to** generate a monthly transaction report  
**So that** I can see this month's transactions

**Acceptance Criteria:**
- Given I am on the Transaction Reports screen (CR00)
- When I select the Monthly report option
- Then the system should use the current month date range
- And submit a batch job to generate the report

**Story Points:** 3  
**Priority:** Medium  
**Labels:** report, batch  
**Component:** CORPT00C

---

### US-REPT-002: Select Yearly Report

**Title:** Generate Yearly Transaction Report

**As a** user  
**I want to** generate a yearly transaction report  
**So that** I can see this year's transactions

**Acceptance Criteria:**
- Given I am on the Transaction Reports screen
- When I select the Yearly report option
- Then the system should use January 1 to December 31 of current year
- And submit a batch job to generate the report

**Story Points:** 3  
**Priority:** Medium  
**Labels:** report, batch  
**Component:** CORPT00C

---

### US-REPT-003: Enter Custom Date Range

**Title:** Generate Custom Date Range Report

**As a** user  
**I want to** specify a custom date range for my report  
**So that** I can see transactions for a specific period

**Acceptance Criteria:**
- Given I am on the Transaction Reports screen
- When I select the Custom report option
- Then I should be able to enter Start Date and End Date
- And the system should validate the date inputs
- And submit a batch job with the custom date range

**Story Points:** 3  
**Priority:** Medium  
**Labels:** report, batch  
**Component:** CORPT00C

---

### US-REPT-004: Validate Custom Date Inputs

**Title:** Validate Report Date Range Inputs

**As a** user  
**I want to** have my date inputs validated  
**So that** I enter valid date ranges

**Acceptance Criteria:**
- Given I am entering custom date range
- When I enter invalid month (not 01-12)
- Then I should see a validation error
- When I enter invalid day (not 01-31)
- Then I should see a validation error
- When I enter non-numeric year
- Then I should see a validation error

**Story Points:** 2  
**Priority:** Medium  
**Labels:** report, validation  
**Component:** CORPT00C

---

## EPIC-7: User Administration

### US-USER-001: Display User List

**Title:** View List of System Users

**As an** administrator  
**I want to** see a list of all system users  
**So that** I can manage user accounts

**Acceptance Criteria:**
- Given I am an admin on the User List screen (CU00)
- When the screen loads
- Then I should see users from the USRSEC file
- And I should see 10 users per page
- And each user should show User ID, First Name, Last Name, User Type

**Story Points:** 3  
**Priority:** High  
**Labels:** admin, user-management  
**Component:** COUSR00C

---

### US-USER-002: Filter Users by User ID

**Title:** Filter User List by User ID

**As an** administrator  
**I want to** filter users by User ID  
**So that** I can find specific users

**Acceptance Criteria:**
- Given I am on the User List screen
- When I enter a User ID filter value
- Then I should see only users matching the filter

**Story Points:** 2  
**Priority:** Medium  
**Labels:** admin, filter  
**Component:** COUSR00C

---

### US-USER-003: Paginate User List

**Title:** Navigate Through User List Pages

**As an** administrator  
**I want to** page through the user list  
**So that** I can view all users

**Acceptance Criteria:**
- Given I am on the User List screen with more than 10 users
- When I press PF8
- Then I should see the next page of users
- When I press PF7
- Then I should see the previous page of users

**Story Points:** 2  
**Priority:** Medium  
**Labels:** admin, pagination  
**Component:** COUSR00C

---

### US-USER-004: Select User to Update

**Title:** Navigate to User Update by Selection

**As an** administrator  
**I want to** select a user with 'U' to update  
**So that** I can modify user information

**Acceptance Criteria:**
- Given I am on the User List screen
- When I enter 'U' next to a user and press Enter
- Then I should be navigated to the User Update screen (CU02)

**Story Points:** 2  
**Priority:** High  
**Labels:** admin, navigation  
**Component:** COUSR00C, COUSR02C

---

### US-USER-005: Select User to Delete

**Title:** Navigate to User Delete by Selection

**As an** administrator  
**I want to** select a user with 'D' to delete  
**So that** I can remove user accounts

**Acceptance Criteria:**
- Given I am on the User List screen
- When I enter 'D' next to a user and press Enter
- Then I should be navigated to the User Delete screen (CU03)

**Story Points:** 2  
**Priority:** High  
**Labels:** admin, navigation  
**Component:** COUSR00C, COUSR03C

---

### US-USER-006: Add New User

**Title:** Create New User Account

**As an** administrator  
**I want to** add a new user to the system  
**So that** new users can access the application

**Acceptance Criteria:**
- Given I am on the User Add screen (CU01)
- When I enter all required fields:
  - User ID (8 characters, unique)
  - Password (8 characters)
  - First Name (20 characters)
  - Last Name (20 characters)
  - User Type (A or U)
- And press Enter
- Then the new user should be written to USRSEC file
- And I should see a success message

**Story Points:** 3  
**Priority:** High  
**Labels:** admin, create  
**Component:** COUSR01C

---

### US-USER-007: Validate Unique User ID

**Title:** Ensure User ID is Unique

**As an** administrator  
**I want to** be prevented from creating duplicate User IDs  
**So that** each user has a unique identifier

**Acceptance Criteria:**
- Given I am adding a new user
- When I enter a User ID that already exists
- Then I should see an error message
- And the user should not be created

**Story Points:** 2  
**Priority:** High  
**Labels:** admin, validation  
**Component:** COUSR01C

---

### US-USER-008: Validate Required User Fields

**Title:** Validate All Required Fields for New User

**As an** administrator  
**I want to** be notified of missing required fields  
**So that** I provide complete user information

**Acceptance Criteria:**
- Given I am adding a new user
- When I leave User ID empty, I should see an error
- When I leave Password empty, I should see an error
- When I leave First Name empty, I should see an error
- When I leave Last Name empty, I should see an error

**Story Points:** 2  
**Priority:** High  
**Labels:** admin, validation  
**Component:** COUSR01C

---

### US-USER-009: Validate User Type

**Title:** Validate User Type as A or U

**As an** administrator  
**I want to** only enter valid user types  
**So that** users have correct access levels

**Acceptance Criteria:**
- Given I am adding or updating a user
- When I enter a User Type other than 'A' or 'U'
- Then I should see a validation error message

**Story Points:** 1  
**Priority:** High  
**Labels:** admin, validation  
**Component:** COUSR01C, COUSR02C

---

### US-USER-010: Update Existing User

**Title:** Modify User Account Information

**As an** administrator  
**I want to** update existing user information  
**So that** I can modify user details

**Acceptance Criteria:**
- Given I am on the User Update screen (CU02)
- When I modify editable fields and press Enter
- Then the system should validate all fields
- And update the user record in USRSEC file
- And the User ID should not be editable

**Story Points:** 3  
**Priority:** High  
**Labels:** admin, update  
**Component:** COUSR02C

---

### US-USER-011: Delete User with Confirmation

**Title:** Delete User Account After Confirmation

**As an** administrator  
**I want to** delete a user after confirmation  
**So that** I don't accidentally delete users

**Acceptance Criteria:**
- Given I am on the User Delete screen (CU03)
- When I see the user information displayed
- And I confirm the deletion
- Then the user should be deleted from USRSEC file
- And I should return to the User List

**Story Points:** 2  
**Priority:** High  
**Labels:** admin, delete  
**Component:** COUSR03C

---

## EPIC-8: Batch Processing

### US-BATCH-001: Post Daily Transactions

**Title:** Process and Post Daily Transactions

**As a** system  
**I want to** process daily transactions from the input file  
**So that** transactions are posted to the master file

**Acceptance Criteria:**
- Given the POSTTRAN batch job runs
- When transactions are read from DALYTRAN file
- Then each transaction should be validated
- And valid transactions should be written to TRANSACT file
- And invalid transactions should be written to DALYREJS file
- And account balances should be updated
- And transaction category balances should be updated
- And counts of processed and rejected transactions should be displayed

**Story Points:** 8  
**Priority:** High  
**Labels:** batch, transaction  
**Component:** CBTRN02C

---

### US-BATCH-002: Validate Transaction Card Number

**Title:** Validate Card Number in Cross-Reference

**As a** system  
**I want to** validate card numbers against the cross-reference file  
**So that** only valid cards have transactions posted

**Acceptance Criteria:**
- Given a transaction is being processed
- When the card number is not found in XREF file
- Then the transaction should be rejected
- And written to the reject file with reason code

**Story Points:** 3  
**Priority:** High  
**Labels:** batch, validation  
**Component:** CBTRN02C

---

### US-BATCH-003: Validate Transaction Account

**Title:** Validate Account Exists for Transaction

**As a** system  
**I want to** validate accounts exist for transactions  
**So that** only valid accounts have transactions posted

**Acceptance Criteria:**
- Given a transaction is being processed
- When the account is not found in ACCTDAT file
- Then the transaction should be rejected
- And written to the reject file with reason code

**Story Points:** 3  
**Priority:** High  
**Labels:** batch, validation  
**Component:** CBTRN02C

---

### US-BATCH-004: Set Return Code for Rejections

**Title:** Set Return Code 4 When Transactions Rejected

**As a** system  
**I want to** set return code 4 when transactions are rejected  
**So that** job schedulers can detect issues

**Acceptance Criteria:**
- Given the POSTTRAN job completes
- When any transactions were rejected
- Then return code 4 should be set
- When no transactions were rejected
- Then return code 0 should be set

**Story Points:** 1  
**Priority:** Medium  
**Labels:** batch, error-handling  
**Component:** CBTRN02C

---

### US-BATCH-005: Calculate Monthly Interest

**Title:** Calculate Interest Charges on Accounts

**As a** system  
**I want to** calculate interest based on transaction categories  
**So that** accounts are charged appropriate interest

**Acceptance Criteria:**
- Given the INTCALC batch job runs
- When transaction category balances are read
- Then interest rates should be retrieved from DISCGRP file
- And monthly interest should be calculated as (Balance * Rate) / 12
- And account balances should be updated with total interest
- And current cycle credit/debit should be reset to zero

**Story Points:** 5  
**Priority:** High  
**Labels:** batch, interest  
**Component:** CBACT04C

---

### US-BATCH-006: Create Interest Transactions

**Title:** Write Interest Transactions to File

**As a** system  
**I want to** create transaction records for interest charges  
**So that** interest is tracked in transaction history

**Acceptance Criteria:**
- Given interest is calculated for an account
- When the interest amount is determined
- Then an interest transaction should be written to TRANSACT file

**Story Points:** 3  
**Priority:** High  
**Labels:** batch, interest  
**Component:** CBACT04C

---

### US-BATCH-007: Generate Plain Text Statement

**Title:** Create Account Statement in Text Format

**As a** system  
**I want to** generate plain text account statements  
**So that** customers can receive printed statements

**Acceptance Criteria:**
- Given the CREASTMT batch job runs
- When account data is retrieved
- Then a plain text statement should be generated with:
  - Customer name and address
  - Account ID and current balance
  - FICO score
  - Transaction summary with totals
- And output should be 80 characters per line

**Story Points:** 5  
**Priority:** High  
**Labels:** batch, statement  
**Component:** CBSTM03A

---

### US-BATCH-008: Generate HTML Statement

**Title:** Create Account Statement in HTML Format

**As a** system  
**I want to** generate HTML account statements  
**So that** customers can view statements online

**Acceptance Criteria:**
- Given the CREASTMT batch job runs
- When account data is retrieved
- Then an HTML statement should be generated with:
  - Formatted table layout
  - Customer name and address
  - Account details
  - Transaction summary

**Story Points:** 5  
**Priority:** Medium  
**Labels:** batch, statement  
**Component:** CBSTM03A

---

### US-BATCH-009: Generate Transaction Report

**Title:** Create Transaction Report by Date Range

**As a** system  
**I want to** generate transaction reports for a date range  
**So that** users can review transaction history

**Acceptance Criteria:**
- Given the TRANREPT batch job runs with date parameters
- When transactions are filtered by date range
- Then a formatted report should be generated
- And transactions should be sorted by card number and date

**Story Points:** 3  
**Priority:** Medium  
**Labels:** batch, report  
**Component:** CBTRN03C

---

## EPIC-9: Optional Modules

### US-OPT-001: View Pending Authorization Summary

**Title:** Display Pending Authorization Summary

**As a** user  
**I want to** view a summary of pending authorizations  
**So that** I can see outstanding authorization requests

**Acceptance Criteria:**
- Given I am on the Pending Authorization Summary screen (CPVS)
- When the screen loads
- Then I should see pending authorizations with account details
- And I should be able to navigate with PF7/PF8

**Story Points:** 5  
**Priority:** Low  
**Labels:** optional, authorization, IMS  
**Component:** COPAUS0C

---

### US-OPT-002: View Authorization Details

**Title:** Display Authorization Details

**As a** user  
**I want to** view details of a specific authorization  
**So that** I can see complete authorization information

**Acceptance Criteria:**
- Given I select an authorization from the summary
- When I navigate to the Authorization Details screen (CPVD)
- Then I should see complete authorization details

**Story Points:** 3  
**Priority:** Low  
**Labels:** optional, authorization, IMS  
**Component:** COPAUS1C

---

### US-OPT-003: Mark Authorization as Fraud

**Title:** Flag Authorization as Fraudulent

**As a** user  
**I want to** mark an authorization as fraudulent  
**So that** fraud cases are tracked

**Acceptance Criteria:**
- Given I am on the Authorization Details screen
- When I press PF5
- Then the authorization should be marked as fraudulent
- And the fraud record should be stored in DB2

**Story Points:** 3  
**Priority:** Low  
**Labels:** optional, fraud, DB2  
**Component:** COPAUS2C

---

### US-OPT-004: Process Authorization Requests via MQ

**Title:** Handle Authorization Requests from MQ

**As a** system  
**I want to** process authorization requests received via MQ  
**So that** real-time authorizations are handled

**Acceptance Criteria:**
- Given an authorization request arrives on the MQ queue
- When the CICS program is triggered
- Then the request should be validated
- And account/customer data should be retrieved
- And a response should be sent to the reply queue
- And authorization details should be stored in IMS

**Story Points:** 8  
**Priority:** Low  
**Labels:** optional, MQ, IMS  
**Component:** COPAUA0C

---

### US-OPT-005: Purge Expired Authorizations

**Title:** Batch Purge of Expired Authorizations

**As a** system  
**I want to** purge expired authorization records  
**So that** the database stays clean

**Acceptance Criteria:**
- Given the CBPAUP0J batch job runs
- When expired authorizations are identified
- Then they should be deleted from IMS
- And available credit should be adjusted for unmatched authorizations

**Story Points:** 5  
**Priority:** Low  
**Labels:** optional, batch, IMS  
**Component:** CBPAUP0C

---

### US-OPT-006: List Transaction Types from DB2

**Title:** Display Transaction Types from DB2

**As an** administrator  
**I want to** view transaction types stored in DB2  
**So that** I can manage transaction type reference data

**Acceptance Criteria:**
- Given I am on the Transaction Type List screen (CTLI)
- When the screen loads
- Then I should see transaction types from DB2
- And I should be able to page forward/backward
- And I should be able to update or delete types

**Story Points:** 5  
**Priority:** Low  
**Labels:** optional, DB2, admin  
**Component:** COTRTLIC

---

### US-OPT-007: Add/Edit Transaction Type in DB2

**Title:** Create or Modify Transaction Types

**As an** administrator  
**I want to** add or edit transaction types in DB2  
**So that** I can maintain reference data

**Acceptance Criteria:**
- Given I am on the Transaction Type Add/Edit screen (CTTU)
- When I enter transaction type information
- Then the system should validate the input
- And insert or update the record in DB2

**Story Points:** 5  
**Priority:** Low  
**Labels:** optional, DB2, admin  
**Component:** COTRTUPC

---

### US-OPT-008: Inquire System Date via MQ

**Title:** Get System Date Through MQ Request

**As a** system  
**I want to** inquire system date via MQ  
**So that** distributed systems can get synchronized time

**Acceptance Criteria:**
- Given a date inquiry request is sent via MQ
- When the CDRD transaction processes the request
- Then the system date should be returned in the response

**Story Points:** 3  
**Priority:** Low  
**Labels:** optional, MQ  
**Component:** CODATE01

---

### US-OPT-009: Inquire Account Details via MQ

**Title:** Get Account Details Through MQ Request

**As a** system  
**I want to** inquire account details via MQ  
**So that** distributed systems can access account data

**Acceptance Criteria:**
- Given an account inquiry request is sent via MQ
- When the CDRA transaction processes the request
- Then account details should be returned in the response

**Story Points:** 3  
**Priority:** Low  
**Labels:** optional, MQ  
**Component:** COACCT01

---

## Summary Statistics

| Category | Count |
|----------|-------|
| Total Epics | 9 |
| Total User Stories | 75 |
| High Priority Stories | 45 |
| Medium Priority Stories | 20 |
| Low Priority Stories | 10 |

### Story Points by Epic

| Epic | Story Points |
|------|--------------|
| Authentication | 14 |
| Account Management | 17 |
| Credit Card Management | 20 |
| Transaction Management | 21 |
| Bill Payment | 10 |
| Reporting | 11 |
| User Administration | 24 |
| Batch Processing | 36 |
| Optional Modules | 40 |
| **Total** | **193** |

---

## Jira Import Notes

### Recommended Jira Configuration

**Issue Types:**
- Epic: For the 9 main functional areas
- Story: For each user story
- Sub-task: For breaking down larger stories

**Custom Fields:**
- Component: Map to COBOL program name
- Story Points: Use provided estimates
- Priority: High/Medium/Low as specified

**Labels:**
- Use the labels provided for filtering and reporting
- Common labels: authentication, account, card, transaction, payment, report, admin, batch, validation, optional

### Import Process

1. Create the 9 Epics first
2. Import user stories linked to their respective epics
3. Set up components based on COBOL program names
4. Configure sprint planning based on priorities

---

**Document End**

*This user stories document was generated from the CardDemo Functional Requirements Document for use in Jira project management.*
