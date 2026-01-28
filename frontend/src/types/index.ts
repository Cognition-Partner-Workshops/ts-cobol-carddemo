export interface User {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  userType: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Customer {
  id: number;
  firstName: string;
  middleName?: string;
  lastName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  zipCode: string;
  countryCode: string;
  phoneNumber1?: string;
  phoneNumber2?: string;
  ssn: string;
  ficoScore?: number;
  dateOfBirth?: string;
  ecsUserId?: string;
}

export interface Account {
  id: number;
  activeStatus: string;
  currentBalance: number;
  creditLimit: number;
  cashCreditLimit: number;
  openDate: string;
  expirationDate: string;
  reissueDate?: string;
  currentCycleCredit: number;
  currentCycleDebit: number;
  groupId?: string;
}

export interface Card {
  cardNumber: string;
  accountId: number;
  customerId: number;
  cvvCode: string;
  embossedName: string;
  expirationDate: string;
  activeStatus: string;
}

export interface Transaction {
  id: string;
  cardNumber: string;
  transactionTypeCode: string;
  transactionCategoryCode: number;
  transactionSource: string;
  transactionDescription: string;
  transactionAmount: number;
  merchantId?: string;
  merchantName?: string;
  merchantCity?: string;
  merchantZip?: string;
  originTimestamp: string;
  processingTimestamp?: string;
}

export interface Payment {
  id: number;
  accountId: number;
  amount: number;
  paymentMethod: 'ACH' | 'DEBIT' | 'CHECK' | 'CASH';
  sourceAccount?: string;
  routingNumber?: string;
  confirmationNumber?: string;
  status: 'PENDING' | 'SCHEDULED' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  scheduledDate?: string;
  processedDate?: string;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface DashboardSummary {
  totalCustomers: number;
  totalAccounts: number;
  activeAccounts: number;
  totalCards: number;
  activeCards: number;
  totalTransactionsToday: number;
  totalTransactionsThisMonth: number;
  totalBalance: number;
  totalCreditLimit: number;
  utilizationRate: number;
  overLimitAccounts: number;
  expiringCardsThisMonth: number;
}

export interface AccountStatement {
  accountId: number;
  statementDate: string;
  openingBalance: number;
  closingBalance: number;
  totalDebits: number;
  totalCredits: number;
  minimumPaymentDue: number;
  paymentDueDate: string;
  transactions: Transaction[];
}
