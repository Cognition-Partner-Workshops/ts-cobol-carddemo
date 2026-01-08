# CardDemo Live Data Input Tool

A web-based data entry tool for the CardDemo mainframe credit card management application. This tool allows users to input live data through a user-friendly interface and export it in the fixed-width format required by the CardDemo mainframe batch processing jobs.

## Features

- **Transaction Entry**: Add credit card transactions with full merchant details
- **Customer Management**: Create customer records with complete demographic information
- **Account Creation**: Set up credit card accounts with balances and limits
- **Card Registration**: Register new credit cards linked to accounts
- **Data Export**: Export data in fixed-width format compatible with CardDemo COBOL copybooks
- **Local Storage**: Data persists in browser local storage between sessions
- **Sample Data**: Load sample data to test the export functionality

## Getting Started

### Running Locally

Simply open `index.html` in a web browser:

```bash
# From the repository root
open tools/live-data-input/index.html

# Or use a local server
cd tools/live-data-input
python -m http.server 8080
# Then open http://localhost:8080
```

### Using the Tool

1. **Navigate** between tabs to access different data entry forms:
   - Transactions
   - Customers
   - Accounts
   - Cards
   - Export Data

2. **Enter Data** using the forms. Each field shows its maximum length and format requirements.

3. **Review** your queued data in the preview section below each form.

4. **Export** your data from the Export tab:
   - Export individual file types (transactions, customers, accounts, cards)
   - Export all data at once
   - Files are downloaded in the correct fixed-width format

## File Formats

The tool exports data in formats matching the CardDemo COBOL copybooks:

| File | Copybook | Record Length | Description |
|------|----------|---------------|-------------|
| dailytran.txt | CVTRA06Y | 350 bytes | Daily transaction records |
| custdata.txt | CVCUS01Y | 500 bytes | Customer master records |
| acctdata.txt | CVACT01Y | 300 bytes | Account master records |
| carddata.txt | CVACT02Y | 150 bytes | Card master records |

## Integration with CardDemo

The exported files can be used with the CardDemo batch processing jobs:

1. **Transactions**: Upload `dailytran.txt` to replace `AWS.M2.CARDDEMO.DALYTRAN.PS` and run the `POSTTRAN` job
2. **Customers**: Upload `custdata.txt` and run the `CUSTFILE` job to load into VSAM
3. **Accounts**: Upload `acctdata.txt` and run the `ACCTFILE` job to load into VSAM
4. **Cards**: Upload `carddata.txt` and run the `CARDFILE` job to load into VSAM

## Data Validation

The tool performs basic validation:
- Required field checking
- Length constraints based on COBOL field definitions
- Numeric format validation for IDs and amounts
- Date format validation

## Browser Compatibility

The tool works in all modern browsers:
- Chrome (recommended)
- Firefox
- Safari
- Edge

## Technical Details

### Amount Formatting

Amounts are formatted using COBOL signed numeric representation:
- Positive amounts: Last digit uses `{ABCDEFGHI` for 0-9
- Negative amounts: Last digit uses `}JKLMNOPQR` for 0-9

### Timestamp Format

Timestamps are generated in the format: `YYYY-MM-DD HH:MM:SS.NNNNNN`

### Local Storage

Data is automatically saved to browser local storage under the key `cardDemoData`. Use the "Clear All Queued Data" button to reset.

## Contributing

This tool is part of the AWS Mainframe Modernization CardDemo project. See the main repository's CONTRIBUTING.md for guidelines.

## License

This project is licensed under the Apache 2.0 License - see the LICENSE file in the repository root for details.
