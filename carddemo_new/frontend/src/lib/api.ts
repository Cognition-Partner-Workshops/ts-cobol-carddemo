import axios, { AxiosInstance, AxiosError } from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export const authApi = {
  login: (userId: string, password: string) =>
    api.post<ApiResponse<{ token: string; userId: string; userType: string; firstName: string; lastName: string; redirectUrl: string }>>('/api/auth/login', { userId, password }),
  logout: () => api.post<ApiResponse<void>>('/api/auth/logout'),
  getCurrentUser: () => api.get<ApiResponse<{ userId: string; firstName: string; lastName: string; userType: string }>>('/api/auth/me'),
};

export const accountApi = {
  getAll: (page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<Account>>>(`/api/accounts?page=${page}&size=${size}`),
  getById: (accountId: string) =>
    api.get<ApiResponse<Account>>(`/api/accounts/${accountId}`),
  getByCustomer: (customerId: string) =>
    api.get<ApiResponse<Account[]>>(`/api/accounts/customer/${customerId}`),
  update: (accountId: string, data: Partial<Account>) =>
    api.put<ApiResponse<Account>>(`/api/accounts/${accountId}`, data),
};

export const cardApi = {
  getAll: (page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<Card>>>(`/api/cards?page=${page}&size=${size}`),
  getById: (cardNumber: string) =>
    api.get<ApiResponse<Card>>(`/api/cards/${cardNumber}`),
  getByAccount: (accountId: string, page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<Card>>>(`/api/cards/account/${accountId}?page=${page}&size=${size}`),
  update: (cardNumber: string, data: Partial<Card>) =>
    api.put<ApiResponse<Card>>(`/api/cards/${cardNumber}`, data),
};

export const transactionApi = {
  getAll: (page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<Transaction>>>(`/api/transactions?page=${page}&size=${size}`),
  getById: (transactionId: string) =>
    api.get<ApiResponse<Transaction>>(`/api/transactions/${transactionId}`),
  getByAccount: (accountId: string, page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<Transaction>>>(`/api/transactions/account/${accountId}?page=${page}&size=${size}`),
  create: (data: CreateTransactionRequest) =>
    api.post<ApiResponse<Transaction>>('/api/transactions', data),
  getTypes: () => api.get<ApiResponse<TransactionType[]>>('/api/transactions/types'),
  getCategories: () => api.get<ApiResponse<TransactionCategory[]>>('/api/transactions/categories'),
};

export const paymentApi = {
  getByAccount: (accountId: string, page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<Payment>>>(`/api/payments/account/${accountId}?page=${page}&size=${size}`),
  create: (data: CreatePaymentRequest) =>
    api.post<ApiResponse<Payment>>('/api/payments', data),
  process: (paymentId: string) =>
    api.post<ApiResponse<Payment>>(`/api/payments/${paymentId}/process`),
};

export const reportApi = {
  getTransactionSummary: (accountId: string, startDate: string, endDate: string) =>
    api.get<ApiResponse<TransactionSummaryReport>>(`/api/reports/transaction-summary/${accountId}?startDate=${startDate}&endDate=${endDate}`),
  getAccountStatement: (accountId: string, month: number, year: number) =>
    api.get<ApiResponse<AccountStatementReport>>(`/api/reports/statement/${accountId}?month=${month}&year=${year}`),
};

export const adminApi = {
  getUsers: (page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<AdminUser>>>(`/api/admin/users?page=${page}&size=${size}`),
  getUserById: (userId: string) =>
    api.get<ApiResponse<AdminUser>>(`/api/admin/users/${userId}`),
  createUser: (data: CreateUserRequest) =>
    api.post<ApiResponse<AdminUser>>('/api/admin/users', data),
  updateUser: (userId: string, data: Partial<AdminUser>) =>
    api.put<ApiResponse<AdminUser>>(`/api/admin/users/${userId}`, data),
  deleteUser: (userId: string) =>
    api.delete<ApiResponse<void>>(`/api/admin/users/${userId}`),
};

export interface Account {
  accountId: string;
  activeStatus: string;
  currentBalance: number;
  creditLimit: number;
  cashCreditLimit: number;
  openDate: string;
  expirationDate: string;
  availableCredit: number;
  availableCash: number;
  customerId: string;
}

export interface Card {
  cardNumber: string;
  maskedCardNumber: string;
  accountId: string;
  embossedName: string;
  expirationDate: string;
  activeStatus: string;
  customerId: string;
  expired: boolean;
}

export interface Transaction {
  transactionId: string;
  typeCode: string;
  typeDescription: string;
  categoryCode: string;
  categoryDescription: string;
  description: string;
  amount: number;
  merchantName: string;
  merchantCity: string;
  cardNumber: string;
  maskedCardNumber: string;
  accountId: string;
  originalTimestamp: string;
  status: string;
}

export interface TransactionType {
  typeCode: string;
  description: string;
}

export interface TransactionCategory {
  categoryCode: string;
  description: string;
}

export interface CreateTransactionRequest {
  cardNumber: string;
  typeCode: string;
  categoryCode: string;
  amount: number;
  description?: string;
  merchantId?: string;
  merchantName?: string;
  merchantCity?: string;
  merchantZip?: string;
}

export interface Payment {
  paymentId: string;
  accountId: string;
  amount: number;
  paymentSource: string;
  sourceAccount: string;
  confirmationNumber: string;
  status: string;
  scheduledDate: string;
  processedDate: string;
}

export interface CreatePaymentRequest {
  accountId: string;
  amount: number;
  paymentSource: string;
  sourceAccount?: string;
}

export interface TransactionSummaryReport {
  accountId: string;
  startDate: string;
  endDate: string;
  totalTransactions: number;
  totalPurchases: number;
  totalPayments: number;
  netChange: number;
}

export interface AccountStatementReport {
  accountId: string;
  customerName: string;
  statementDate: string;
  previousBalance: number;
  newBalance: number;
  minimumPaymentDue: number;
  paymentDueDate: string;
}

export interface AdminUser {
  userId: string;
  firstName: string;
  lastName: string;
  userType: string;
  userTypeDescription: string;
  active: boolean;
  createdAt: string;
  lastLogin: string;
}

export interface CreateUserRequest {
  userId: string;
  firstName: string;
  lastName: string;
  password: string;
  userType: string;
}

export default api;
