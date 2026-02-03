export interface User {
  id?: string;
  userId: string;
  firstName: string;
  lastName: string;
  password?: string;
  userType: 'ADMIN' | 'USER';
}

export interface Customer {
  id?: string;
  customerId: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  addressLine1: string;
  addressLine2?: string;
  addressLine3?: string;
  addressCity: string;
  addressState: string;
  stateCode: string;
  countryCode: string;
  zipCode: string;
  phoneNumber1: string;
  phoneNumber2?: string;
  ssn: string;
  govtIssuedId: string;
  dateOfBirth: string;
  eftAccountId?: string;
  primaryCardHolderInd: string;
  ficoCreditScore: number;
}

export interface Account {
  id?: string;
  accountId: string;
  activeStatus: string;
  currentBalance: number;
  creditLimit: number;
  cashCreditLimit: number;
  openDate: string;
  expirationDate: string;
  reissueDate?: string;
  currentCycleCredit: number;
  currentCycleDebit: number;
  zipCode: string;
  groupId?: string;
}

export interface Card {
  id?: string;
  cardNumber: string;
  accountId: string;
  cvvCode: string;
  embossedName: string;
  expirationDate: string;
  activeStatus: string;
}

export interface Transaction {
  id?: string;
  transactionId: string;
  typeCode: string;
  categoryCode: number;
  source: string;
  description: string;
  amount: number;
  merchantId: string;
  merchantName: string;
  merchantCity: string;
  merchantZip: string;
  cardNumber: string;
  originTimestamp: string;
  processTimestamp: string;
}

export interface LoginRequest {
  userId: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: string;
  firstName: string;
  lastName: string;
  userType: 'ADMIN' | 'USER';
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface TransactionRequest {
  accountId?: string;
  cardNumber: string;
  typeCode: string;
  categoryCode: number;
  source: string;
  description: string;
  amount: number;
  merchantId: string;
  merchantName: string;
  merchantCity: string;
  merchantZip: string;
}

export interface BillPaymentRequest {
  accountId: string;
  amount?: number;
}
