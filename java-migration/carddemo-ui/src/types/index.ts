export interface User {
  userId: string;
  firstName: string;
  lastName: string;
  userType: string;
  enabled: boolean;
  admin: boolean;
}

export interface LoginRequest {
  userId: string;
  password: string;
}

export interface LoginResponse {
  userId: string;
  firstName: string;
  lastName: string;
  userType: string;
  token: string;
  expiresIn: number;
}

export interface Account {
  accountId: number;
  activeStatus: string;
  currentBalance: number;
  creditLimit: number;
  cashCreditLimit: number;
  openDate: string;
  expirationDate: string;
  reissueDate: string;
  currentCycleCredit: number;
  currentCycleDebit: number;
  zipCode: string;
  groupId: string;
  availableCredit: number;
  overLimit: boolean;
}

export interface Card {
  cardNumber: string;
  maskedCardNumber: string;
  accountId: number;
  embossedName: string;
  expirationDate: string;
  activeStatus: string;
  expired: boolean;
}

export interface Transaction {
  transactionId: string;
  transactionTypeCode: string;
  transactionCategoryCode: number;
  transactionSource: string;
  description: string;
  amount: number;
  merchantId: string;
  merchantName: string;
  merchantCity: string;
  merchantZip: string;
  cardNumber: string;
  maskedCardNumber: string;
  originTimestamp: string;
  processTimestamp: string;
}

export interface Customer {
  customerId: number;
  firstName: string;
  middleName: string;
  lastName: string;
  fullName: string;
  addressLine1: string;
  addressLine2: string;
  addressLine3: string;
  stateCode: string;
  countryCode: string;
  zipCode: string;
  phoneNumber1: string;
  phoneNumber2: string;
  govtIssuedId: string;
  dateOfBirth: string;
  eftAccountId: string;
  primaryCardHolder: string;
  ficoCreditScore: number;
}

export interface AccountStatistics {
  totalAccounts: number;
  activeAccounts: number;
  totalBalance: number;
  totalCreditLimit: number;
  overLimitCount: number;
}

export interface CardStatistics {
  totalCards: number;
  activeCards: number;
  expiringCards: number;
}

export interface UserStatistics {
  totalUsers: number;
  totalAdmins: number;
  totalEnabledUsers: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface AccountSummaryReport {
  reportDate: string;
  totalAccounts: number;
  activeAccounts: number;
  inactiveAccounts: number;
  totalBalance: number;
  totalCreditLimit: number;
  creditUtilizationRate: number;
  overLimitAccountCount: number;
}

export interface TransactionSummaryReport {
  reportDate: string;
  startDate: string;
  endDate: string;
  totalTransactions: number;
  totalAmount: number;
  totalCredits: number;
  totalDebits: number;
  averageTransactionAmount: number;
  transactionsByType: TransactionTypeSummary[];
}

export interface TransactionTypeSummary {
  typeCode: string;
  count: number;
  totalAmount: number;
}

export interface CardStatusReport {
  reportDate: string;
  totalCards: number;
  activeCards: number;
  inactiveCards: number;
  expiredActiveCards: number;
  expiringWithin30Days: number;
}
