# Issue #21: Account Group Displayed as Spaces

## Issue Summary

The "Account Group" field was displaying as blank spaces on the CICS screens for both the View Account and Update Account screens, instead of showing the actual account group value.

**Original Issue:** [aws-samples/aws-mainframe-modernization-carddemo#21](https://github.com/aws-samples/aws-mainframe-modernization-carddemo/issues/21)

## Root Cause Analysis

After thorough investigation of the codebase, the root cause was identified as a **data issue** rather than a code defect. The sample account data file (`app/data/ASCII/acctdata.txt`) did not have the Account Group ID field populated - it contained only spaces in that field position.

### Technical Details

The Account Group ID field is defined in the following locations:

1. **Account Record Copybook** (`app/cpy/CVACT01Y.cpy`, line 16):
   ```cobol
   05  ACCT-GROUP-ID                     PIC X(10).
   ```

2. **BMS Screen Definition** (`app/bms/COACTUP.bms`, lines 229-232):
   ```
   AADDGRP DFHMDF ATTRB=(UNPROT),
                  HILIGHT=UNDERLINE,
                  LENGTH=10,
                  POS=(10,23)
   ```

3. **BMS Copybook** (`app/cpy-bms/COACTUP.CPY`, line 150):
   ```cobol
   02  AADDGRPI  PIC X(10).
   ```

The field definitions are consistent across all files (LENGTH=10), confirming there was no mismatch between the BMS screen definition and the COBOL copybook.

### Data File Structure

The account data file has the following structure (300 bytes per record):

| Field | Position | Length | Description |
|-------|----------|--------|-------------|
| ACCT-ID | 0-10 | 11 | Account identifier |
| ACCT-ACTIVE-STATUS | 11 | 1 | Account status (Y/N) |
| ACCT-CURR-BAL | 12-23 | 12 | Current balance |
| ACCT-CREDIT-LIMIT | 24-35 | 12 | Credit limit |
| ACCT-CASH-CREDIT-LIMIT | 36-47 | 12 | Cash credit limit |
| ACCT-OPEN-DATE | 48-57 | 10 | Account open date |
| ACCT-EXPIRAION-DATE | 58-67 | 10 | Expiration date |
| ACCT-REISSUE-DATE | 68-77 | 10 | Reissue date |
| ACCT-CURR-CYC-CREDIT | 78-89 | 12 | Current cycle credit |
| ACCT-CURR-CYC-DEBIT | 90-101 | 12 | Current cycle debit |
| ACCT-ADDR-ZIP | 102-111 | 10 | Address ZIP code |
| **ACCT-GROUP-ID** | **112-121** | **10** | **Account Group ID** |
| FILLER | 122-299 | 178 | Reserved space |

## Resolution

The fix involved populating the Account Group ID field in the sample data file with meaningful values. Account groups were assigned based on credit limit tiers:

| Group | Credit Limit Range | Description |
|-------|-------------------|-------------|
| PLATINUM | >= $800 | Premium tier accounts |
| GOLD | >= $500 | High-value accounts |
| SILVER | >= $300 | Mid-tier accounts |
| STANDARD | < $300 | Standard accounts |

### Distribution After Fix

After applying the fix, the 50 sample accounts are distributed as follows:

- PLATINUM: 7 accounts
- GOLD: 17 accounts
- SILVER: 10 accounts
- STANDARD: 16 accounts

## Files Modified

1. `app/data/ASCII/acctdata.txt` - Updated to include Account Group IDs for all 50 sample accounts

## Testing Recommendations

To verify the fix:

1. Load the updated account data file into the VSAM dataset
2. Navigate to the View Account screen (COACTVW) and verify the Account Group field displays the assigned group
3. Navigate to the Update Account screen (COACTUPC) and verify the Account Group field is editable and displays correctly

## Common Mistakes to Avoid

Based on this issue, here are common mistakes to watch for in mainframe COBOL/CICS applications:

1. **Missing sample data** - Always ensure sample data files have all fields populated with meaningful test values
2. **Field length mismatches** - Verify that BMS screen definitions, copybooks, and data structures all use consistent field lengths
3. **Data position errors** - When working with fixed-length records, carefully verify field positions match the copybook definitions
4. **EBCDIC/ASCII conversion** - Be aware of character encoding differences when working with mainframe data files

## Related Files

- `app/cbl/COACTUPC.cbl` - Update Account COBOL program
- `app/cbl/COACTVWC.cbl` - View Account COBOL program
- `app/bms/COACTUP.bms` - Update Account BMS map
- `app/bms/COACTVW.bms` - View Account BMS map
- `app/cpy/CVACT01Y.cpy` - Account record copybook
- `app/cpy-bms/COACTUP.CPY` - Update Account BMS copybook
- `app/cpy-bms/COACTVW.CPY` - View Account BMS copybook
