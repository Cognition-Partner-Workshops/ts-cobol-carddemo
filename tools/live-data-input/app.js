/**
 * CardDemo Live Data Input - Application Logic
 * Handles data entry, validation, and export to fixed-width format files
 */

// Data storage
const dataStore = {
    transactions: [],
    customers: [],
    accounts: [],
    cards: []
};

// Initialize application
document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initForms();
    loadFromLocalStorage();
    updateAllCounts();
    generateTransactionId();
});

// Tab Navigation
function initTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.dataset.tab;

            tabButtons.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            document.getElementById(tabId).classList.add('active');

            if (tabId === 'export') {
                updateExportCounts();
            }
        });
    });
}

// Form Initialization
function initForms() {
    // Transaction Form
    document.getElementById('transaction-form').addEventListener('submit', (e) => {
        e.preventDefault();
        addTransaction();
    });

    // Customer Form
    document.getElementById('customer-form').addEventListener('submit', (e) => {
        e.preventDefault();
        addCustomer();
    });

    // Account Form
    document.getElementById('account-form').addEventListener('submit', (e) => {
        e.preventDefault();
        addAccount();
    });

    // Card Form
    document.getElementById('card-form').addEventListener('submit', (e) => {
        e.preventDefault();
        addCard();
    });
}

// Utility Functions
function padRight(str, length) {
    str = String(str || '');
    return str.substring(0, length).padEnd(length, ' ');
}

function padLeft(str, length, char = '0') {
    str = String(str || '');
    return str.substring(0, length).padStart(length, char);
}

function formatAmount(amount, length = 11) {
    // Format amount as signed numeric with implied decimal (S9(9)V99)
    // Positive amounts use '{' for last digit 0, 'A'-'I' for 1-9
    // Negative amounts use '}' for last digit 0, 'J'-'R' for 1-9
    const num = Math.round(Math.abs(parseFloat(amount || 0) * 100));
    const isNegative = parseFloat(amount || 0) < 0;
    let numStr = padLeft(String(num), length, '0');
    
    const lastDigit = parseInt(numStr.charAt(numStr.length - 1));
    const signedChars = isNegative ? '}JKLMNOPQR' : '{ABCDEFGHI';
    
    return numStr.substring(0, numStr.length - 1) + signedChars.charAt(lastDigit);
}

function formatDate(dateStr) {
    if (!dateStr) return '          ';
    return padRight(dateStr, 10);
}

function formatTimestamp() {
    const now = new Date();
    const year = now.getFullYear();
    const month = padLeft(now.getMonth() + 1, 2);
    const day = padLeft(now.getDate(), 2);
    const hours = padLeft(now.getHours(), 2);
    const minutes = padLeft(now.getMinutes(), 2);
    const seconds = padLeft(now.getSeconds(), 2);
    const ms = padLeft(now.getMilliseconds(), 3) + '000';
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${ms}`;
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type + ' show';
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function saveToLocalStorage() {
    localStorage.setItem('cardDemoData', JSON.stringify(dataStore));
}

function loadFromLocalStorage() {
    const saved = localStorage.getItem('cardDemoData');
    if (saved) {
        const data = JSON.parse(saved);
        dataStore.transactions = data.transactions || [];
        dataStore.customers = data.customers || [];
        dataStore.accounts = data.accounts || [];
        dataStore.cards = data.cards || [];
        
        renderTransactionList();
        renderCustomerList();
        renderAccountList();
        renderCardList();
    }
}

function updateAllCounts() {
    document.getElementById('tran-count').textContent = dataStore.transactions.length;
    document.getElementById('cust-count').textContent = dataStore.customers.length;
    document.getElementById('acct-count').textContent = dataStore.accounts.length;
    document.getElementById('card-count').textContent = dataStore.cards.length;
}

function updateExportCounts() {
    document.getElementById('export-tran-count').textContent = dataStore.transactions.length;
    document.getElementById('export-cust-count').textContent = dataStore.customers.length;
    document.getElementById('export-acct-count').textContent = dataStore.accounts.length;
    document.getElementById('export-card-count').textContent = dataStore.cards.length;
}

// Transaction Functions
function generateTransactionId() {
    const timestamp = Date.now();
    const random = Math.floor(Math.random() * 1000);
    const id = padLeft(String(timestamp).slice(-13) + padLeft(random, 3), 16);
    document.getElementById('tran-id').value = id;
}

function addTransaction() {
    const transaction = {
        id: document.getElementById('tran-id').value || generateTransactionId(),
        typeCode: document.getElementById('tran-type').value,
        catCode: padLeft(document.getElementById('tran-cat').value, 4),
        source: document.getElementById('tran-source').value,
        description: document.getElementById('tran-desc').value,
        amount: parseFloat(document.getElementById('tran-amt').value) || 0,
        merchantId: padLeft(document.getElementById('tran-merchant-id').value, 9),
        merchantName: document.getElementById('tran-merchant-name').value,
        merchantCity: document.getElementById('tran-merchant-city').value,
        merchantZip: document.getElementById('tran-merchant-zip').value,
        cardNum: document.getElementById('tran-card').value,
        origTimestamp: formatTimestamp(),
        procTimestamp: ''
    };

    if (!transaction.cardNum || transaction.cardNum.length !== 16) {
        showToast('Please enter a valid 16-digit card number', 'error');
        return;
    }

    dataStore.transactions.push(transaction);
    saveToLocalStorage();
    renderTransactionList();
    updateAllCounts();
    
    document.getElementById('transaction-form').reset();
    generateTransactionId();
    showToast('Transaction added successfully');
}

function renderTransactionList() {
    const list = document.getElementById('transaction-list');
    if (dataStore.transactions.length === 0) {
        list.innerHTML = '<div class="empty-state"><div class="empty-state-icon">&#128179;</div><p>No transactions in queue</p></div>';
        return;
    }

    list.innerHTML = dataStore.transactions.map((t, index) => `
        <div class="record-item">
            <div class="record-info">
                <div class="record-title">${t.description || 'Transaction'} - $${t.amount.toFixed(2)}</div>
                <div class="record-details">Card: ${t.cardNum} | ${t.source} | ${t.origTimestamp}</div>
            </div>
            <div class="record-actions">
                <button class="btn-delete" onclick="deleteTransaction(${index})">Delete</button>
            </div>
        </div>
    `).join('');
}

function deleteTransaction(index) {
    dataStore.transactions.splice(index, 1);
    saveToLocalStorage();
    renderTransactionList();
    updateAllCounts();
    showToast('Transaction deleted');
}

// Customer Functions
function addCustomer() {
    const customer = {
        id: padLeft(document.getElementById('cust-id').value, 9),
        firstName: document.getElementById('cust-first').value,
        middleName: document.getElementById('cust-middle').value,
        lastName: document.getElementById('cust-last').value,
        addrLine1: document.getElementById('cust-addr1').value,
        addrLine2: document.getElementById('cust-addr2').value,
        addrLine3: document.getElementById('cust-addr3').value,
        state: document.getElementById('cust-state').value.toUpperCase(),
        country: document.getElementById('cust-country').value.toUpperCase(),
        zip: document.getElementById('cust-zip').value,
        phone1: document.getElementById('cust-phone1').value,
        phone2: document.getElementById('cust-phone2').value,
        ssn: padLeft(document.getElementById('cust-ssn').value, 9),
        govtId: document.getElementById('cust-govt-id').value,
        dob: document.getElementById('cust-dob').value,
        eftAccountId: document.getElementById('cust-eft').value,
        primaryHolder: document.getElementById('cust-primary').value,
        ficoScore: padLeft(document.getElementById('cust-fico').value, 3)
    };

    if (!customer.id || customer.id === '000000000') {
        showToast('Please enter a valid Customer ID', 'error');
        return;
    }

    dataStore.customers.push(customer);
    saveToLocalStorage();
    renderCustomerList();
    updateAllCounts();
    
    document.getElementById('customer-form').reset();
    document.getElementById('cust-country').value = 'USA';
    showToast('Customer added successfully');
}

function renderCustomerList() {
    const list = document.getElementById('customer-list');
    if (dataStore.customers.length === 0) {
        list.innerHTML = '<div class="empty-state"><div class="empty-state-icon">&#128100;</div><p>No customers in queue</p></div>';
        return;
    }

    list.innerHTML = dataStore.customers.map((c, index) => `
        <div class="record-item">
            <div class="record-info">
                <div class="record-title">${c.firstName} ${c.middleName} ${c.lastName}</div>
                <div class="record-details">ID: ${c.id} | ${c.addrLine3}, ${c.state} ${c.zip}</div>
            </div>
            <div class="record-actions">
                <button class="btn-delete" onclick="deleteCustomer(${index})">Delete</button>
            </div>
        </div>
    `).join('');
}

function deleteCustomer(index) {
    dataStore.customers.splice(index, 1);
    saveToLocalStorage();
    renderCustomerList();
    updateAllCounts();
    showToast('Customer deleted');
}

// Account Functions
function addAccount() {
    const account = {
        id: padLeft(document.getElementById('acct-id').value, 11),
        status: document.getElementById('acct-status').value,
        balance: parseFloat(document.getElementById('acct-balance').value) || 0,
        creditLimit: parseFloat(document.getElementById('acct-credit-limit').value) || 0,
        cashLimit: parseFloat(document.getElementById('acct-cash-limit').value) || 0,
        openDate: document.getElementById('acct-open-date').value,
        expDate: document.getElementById('acct-exp-date').value,
        reissueDate: document.getElementById('acct-reissue-date').value,
        cycCredit: parseFloat(document.getElementById('acct-cyc-credit').value) || 0,
        cycDebit: parseFloat(document.getElementById('acct-cyc-debit').value) || 0,
        zip: document.getElementById('acct-zip').value,
        groupId: document.getElementById('acct-group').value
    };

    if (!account.id || account.id === '00000000000') {
        showToast('Please enter a valid Account ID', 'error');
        return;
    }

    dataStore.accounts.push(account);
    saveToLocalStorage();
    renderAccountList();
    updateAllCounts();
    
    document.getElementById('account-form').reset();
    document.getElementById('acct-group').value = 'A000000000';
    showToast('Account added successfully');
}

function renderAccountList() {
    const list = document.getElementById('account-list');
    if (dataStore.accounts.length === 0) {
        list.innerHTML = '<div class="empty-state"><div class="empty-state-icon">&#128179;</div><p>No accounts in queue</p></div>';
        return;
    }

    list.innerHTML = dataStore.accounts.map((a, index) => `
        <div class="record-item">
            <div class="record-info">
                <div class="record-title">Account ${a.id}</div>
                <div class="record-details">Balance: $${a.balance.toFixed(2)} | Limit: $${a.creditLimit.toFixed(2)} | Status: ${a.status}</div>
            </div>
            <div class="record-actions">
                <button class="btn-delete" onclick="deleteAccount(${index})">Delete</button>
            </div>
        </div>
    `).join('');
}

function deleteAccount(index) {
    dataStore.accounts.splice(index, 1);
    saveToLocalStorage();
    renderAccountList();
    updateAllCounts();
    showToast('Account deleted');
}

// Card Functions
function generateCardNumber() {
    let cardNum = '';
    for (let i = 0; i < 16; i++) {
        cardNum += Math.floor(Math.random() * 10);
    }
    document.getElementById('card-num').value = cardNum;
}

function generateCVV() {
    const cvv = padLeft(Math.floor(Math.random() * 1000), 3);
    document.getElementById('card-cvv').value = cvv;
}

function addCard() {
    const card = {
        cardNum: document.getElementById('card-num').value,
        acctId: padLeft(document.getElementById('card-acct-id').value, 11),
        cvv: padLeft(document.getElementById('card-cvv').value, 3),
        embossedName: document.getElementById('card-name').value.toUpperCase(),
        expDate: document.getElementById('card-exp').value,
        status: document.getElementById('card-status').value
    };

    if (!card.cardNum || card.cardNum.length !== 16) {
        showToast('Please enter a valid 16-digit card number', 'error');
        return;
    }

    dataStore.cards.push(card);
    saveToLocalStorage();
    renderCardList();
    updateAllCounts();
    
    document.getElementById('card-form').reset();
    showToast('Card added successfully');
}

function renderCardList() {
    const list = document.getElementById('card-list');
    if (dataStore.cards.length === 0) {
        list.innerHTML = '<div class="empty-state"><div class="empty-state-icon">&#128179;</div><p>No cards in queue</p></div>';
        return;
    }

    list.innerHTML = dataStore.cards.map((c, index) => `
        <div class="record-item">
            <div class="record-info">
                <div class="record-title">${c.cardNum.replace(/(\d{4})/g, '$1 ').trim()}</div>
                <div class="record-details">Account: ${c.acctId} | ${c.embossedName} | Exp: ${c.expDate}</div>
            </div>
            <div class="record-actions">
                <button class="btn-delete" onclick="deleteCard(${index})">Delete</button>
            </div>
        </div>
    `).join('');
}

function deleteCard(index) {
    dataStore.cards.splice(index, 1);
    saveToLocalStorage();
    renderCardList();
    updateAllCounts();
    showToast('Card deleted');
}

// Export Functions
function formatTransactionRecord(t) {
    // CVTRA06Y - 350 bytes
    let record = '';
    record += padRight(t.id, 16);                    // DALYTRAN-ID (1-16)
    record += padRight(t.typeCode, 2);               // DALYTRAN-TYPE-CD (17-18)
    record += padLeft(t.catCode, 4);                 // DALYTRAN-CAT-CD (19-22)
    record += padRight(t.source, 10);                // DALYTRAN-SOURCE (23-32)
    record += padRight(t.description, 100);          // DALYTRAN-DESC (33-132)
    record += formatAmount(t.amount, 11);            // DALYTRAN-AMT (133-143)
    record += padLeft(t.merchantId, 9);              // DALYTRAN-MERCHANT-ID (144-152)
    record += padRight(t.merchantName, 50);          // DALYTRAN-MERCHANT-NAME (153-202)
    record += padRight(t.merchantCity, 50);          // DALYTRAN-MERCHANT-CITY (203-252)
    record += padRight(t.merchantZip, 10);           // DALYTRAN-MERCHANT-ZIP (253-262)
    record += padRight(t.cardNum, 16);               // DALYTRAN-CARD-NUM (263-278)
    record += padRight(t.origTimestamp, 26);         // DALYTRAN-ORIG-TS (279-304)
    record += padRight(t.procTimestamp, 26);         // DALYTRAN-PROC-TS (305-330)
    record += padRight('', 20);                      // FILLER (331-350)
    return record;
}

function formatCustomerRecord(c) {
    // CVCUS01Y - 500 bytes
    let record = '';
    record += padLeft(c.id, 9);                      // CUST-ID (1-9)
    record += padRight(c.firstName, 25);             // CUST-FIRST-NAME (10-34)
    record += padRight(c.middleName, 25);            // CUST-MIDDLE-NAME (35-59)
    record += padRight(c.lastName, 25);              // CUST-LAST-NAME (60-84)
    record += padRight(c.addrLine1, 50);             // CUST-ADDR-LINE-1 (85-134)
    record += padRight(c.addrLine2, 50);             // CUST-ADDR-LINE-2 (135-184)
    record += padRight(c.addrLine3, 50);             // CUST-ADDR-LINE-3 (185-234)
    record += padRight(c.state, 2);                  // CUST-ADDR-STATE-CD (235-236)
    record += padRight(c.country, 3);                // CUST-ADDR-COUNTRY-CD (237-239)
    record += padRight(c.zip, 10);                   // CUST-ADDR-ZIP (240-249)
    record += padRight(c.phone1, 15);                // CUST-PHONE-NUM-1 (250-264)
    record += padRight(c.phone2, 15);                // CUST-PHONE-NUM-2 (265-279)
    record += padLeft(c.ssn, 9);                     // CUST-SSN (280-288)
    record += padRight(c.govtId, 20);                // CUST-GOVT-ISSUED-ID (289-308)
    record += formatDate(c.dob);                     // CUST-DOB-YYYY-MM-DD (309-318)
    record += padRight(c.eftAccountId, 10);          // CUST-EFT-ACCOUNT-ID (319-328)
    record += padRight(c.primaryHolder, 1);          // CUST-PRI-CARD-HOLDER-IND (329)
    record += padLeft(c.ficoScore, 3);               // CUST-FICO-CREDIT-SCORE (330-332)
    record += padRight('', 168);                     // FILLER (333-500)
    return record;
}

function formatAccountRecord(a) {
    // CVACT01Y - 300 bytes
    let record = '';
    record += padLeft(a.id, 11);                     // ACCT-ID (1-11)
    record += padRight(a.status, 1);                 // ACCT-ACTIVE-STATUS (12)
    record += formatAmount(a.balance, 12);           // ACCT-CURR-BAL (13-24)
    record += formatAmount(a.creditLimit, 12);       // ACCT-CREDIT-LIMIT (25-36)
    record += formatAmount(a.cashLimit, 12);         // ACCT-CASH-CREDIT-LIMIT (37-48)
    record += formatDate(a.openDate);                // ACCT-OPEN-DATE (49-58)
    record += formatDate(a.expDate);                 // ACCT-EXPIRAION-DATE (59-68)
    record += formatDate(a.reissueDate);             // ACCT-REISSUE-DATE (69-78)
    record += formatAmount(a.cycCredit, 12);         // ACCT-CURR-CYC-CREDIT (79-90)
    record += formatAmount(a.cycDebit, 12);          // ACCT-CURR-CYC-DEBIT (91-102)
    record += padRight(a.zip, 10);                   // ACCT-ADDR-ZIP (103-112)
    record += padRight(a.groupId, 10);               // ACCT-GROUP-ID (113-122)
    record += padRight('', 178);                     // FILLER (123-300)
    return record;
}

function formatCardRecord(c) {
    // CVACT02Y - 150 bytes
    let record = '';
    record += padRight(c.cardNum, 16);               // CARD-NUM (1-16)
    record += padLeft(c.acctId, 11);                 // CARD-ACCT-ID (17-27)
    record += padLeft(c.cvv, 3);                     // CARD-CVV-CD (28-30)
    record += padRight(c.embossedName, 50);          // CARD-EMBOSSED-NAME (31-80)
    record += formatDate(c.expDate);                 // CARD-EXPIRAION-DATE (81-90)
    record += padRight(c.status, 1);                 // CARD-ACTIVE-STATUS (91)
    record += padRight('', 59);                      // FILLER (92-150)
    return record;
}

function downloadFile(content, filename) {
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

function exportTransactions() {
    if (dataStore.transactions.length === 0) {
        showToast('No transactions to export', 'warning');
        return;
    }

    const content = dataStore.transactions.map(t => formatTransactionRecord(t)).join('\n');
    downloadFile(content, 'dailytran.txt');
    showToast(`Exported ${dataStore.transactions.length} transactions`);
}

function exportCustomers() {
    if (dataStore.customers.length === 0) {
        showToast('No customers to export', 'warning');
        return;
    }

    const content = dataStore.customers.map(c => formatCustomerRecord(c)).join('\n');
    downloadFile(content, 'custdata.txt');
    showToast(`Exported ${dataStore.customers.length} customers`);
}

function exportAccounts() {
    if (dataStore.accounts.length === 0) {
        showToast('No accounts to export', 'warning');
        return;
    }

    const content = dataStore.accounts.map(a => formatAccountRecord(a)).join('\n');
    downloadFile(content, 'acctdata.txt');
    showToast(`Exported ${dataStore.accounts.length} accounts`);
}

function exportCards() {
    if (dataStore.cards.length === 0) {
        showToast('No cards to export', 'warning');
        return;
    }

    const content = dataStore.cards.map(c => formatCardRecord(c)).join('\n');
    downloadFile(content, 'carddata.txt');
    showToast(`Exported ${dataStore.cards.length} cards`);
}

function exportAll() {
    const hasData = dataStore.transactions.length > 0 || 
                    dataStore.customers.length > 0 || 
                    dataStore.accounts.length > 0 || 
                    dataStore.cards.length > 0;

    if (!hasData) {
        showToast('No data to export', 'warning');
        return;
    }

    // Export each file type that has data
    if (dataStore.transactions.length > 0) {
        const content = dataStore.transactions.map(t => formatTransactionRecord(t)).join('\n');
        downloadFile(content, 'dailytran.txt');
    }
    if (dataStore.customers.length > 0) {
        const content = dataStore.customers.map(c => formatCustomerRecord(c)).join('\n');
        downloadFile(content, 'custdata.txt');
    }
    if (dataStore.accounts.length > 0) {
        const content = dataStore.accounts.map(a => formatAccountRecord(a)).join('\n');
        downloadFile(content, 'acctdata.txt');
    }
    if (dataStore.cards.length > 0) {
        const content = dataStore.cards.map(c => formatCardRecord(c)).join('\n');
        downloadFile(content, 'carddata.txt');
    }

    showToast('All data exported successfully');
}

function clearAllData() {
    if (!confirm('Are you sure you want to clear all queued data? This cannot be undone.')) {
        return;
    }

    dataStore.transactions = [];
    dataStore.customers = [];
    dataStore.accounts = [];
    dataStore.cards = [];

    saveToLocalStorage();
    renderTransactionList();
    renderCustomerList();
    renderAccountList();
    renderCardList();
    updateAllCounts();
    updateExportCounts();

    showToast('All data cleared');
}

function loadSampleData() {
    // Add sample transactions
    dataStore.transactions.push({
        id: padLeft(Date.now().toString().slice(-16), 16),
        typeCode: '01',
        catCode: '0001',
        source: 'POS TERM',
        description: 'Purchase at Sample Store',
        amount: 125.50,
        merchantId: '800000001',
        merchantName: 'Sample Store',
        merchantCity: 'Springfield',
        merchantZip: '12345',
        cardNum: '4111111111111111',
        origTimestamp: formatTimestamp(),
        procTimestamp: ''
    });

    // Add sample customer
    dataStore.customers.push({
        id: '000000051',
        firstName: 'John',
        middleName: 'Michael',
        lastName: 'Smith',
        addrLine1: '123 Main Street',
        addrLine2: 'Apt. 456',
        addrLine3: 'Springfield',
        state: 'IL',
        country: 'USA',
        zip: '62701',
        phone1: '(555)123-4567',
        phone2: '(555)987-6543',
        ssn: '123456789',
        govtId: 'DL12345678',
        dob: '1985-06-15',
        eftAccountId: '0012345678',
        primaryHolder: 'Y',
        ficoScore: '720'
    });

    // Add sample account
    dataStore.accounts.push({
        id: '00000000051',
        status: 'Y',
        balance: 1250.00,
        creditLimit: 10000.00,
        cashLimit: 2000.00,
        openDate: '2020-01-15',
        expDate: '2027-01-15',
        reissueDate: '2027-01-15',
        cycCredit: 0,
        cycDebit: 0,
        zip: '62701',
        groupId: 'A000000000'
    });

    // Add sample card
    dataStore.cards.push({
        cardNum: '4111111111111111',
        acctId: '00000000051',
        cvv: '123',
        embossedName: 'JOHN M SMITH',
        expDate: '2027-01-15',
        status: 'Y'
    });

    saveToLocalStorage();
    renderTransactionList();
    renderCustomerList();
    renderAccountList();
    renderCardList();
    updateAllCounts();
    updateExportCounts();

    showToast('Sample data loaded');
}
