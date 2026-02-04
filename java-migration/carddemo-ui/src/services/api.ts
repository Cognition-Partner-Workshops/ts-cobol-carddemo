import axios from 'axios';
import type { 
  LoginRequest, 
  LoginResponse, 
  Account, 
  Card, 
  Transaction, 
  User,
  Page,
  AccountStatistics,
  CardStatistics,
  UserStatistics,
  AccountSummaryReport,
  TransactionSummaryReport,
  CardStatusReport
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/login', data);
    return response.data;
  },
  validateToken: async (token: string): Promise<boolean> => {
    const response = await api.post<boolean>('/auth/validate', token);
    return response.data;
  },
};

export const accountApi = {
  list: async (page = 0, size = 10): Promise<Page<Account>> => {
    const response = await api.get<Page<Account>>('/accounts', { params: { page, size } });
    return response.data;
  },
  get: async (accountId: number): Promise<Account> => {
    const response = await api.get<Account>(`/accounts/${accountId}`);
    return response.data;
  },
  getStatistics: async (): Promise<AccountStatistics> => {
    const response = await api.get<AccountStatistics>('/accounts/statistics');
    return response.data;
  },
  getOverLimit: async (): Promise<Account[]> => {
    const response = await api.get<Account[]>('/accounts/over-limit');
    return response.data;
  },
  getExpiring: async (days = 30): Promise<Account[]> => {
    const response = await api.get<Account[]>('/accounts/expiring', { params: { days } });
    return response.data;
  },
};

export const cardApi = {
  list: async (page = 0, size = 10): Promise<Page<Card>> => {
    const response = await api.get<Page<Card>>('/cards', { params: { page, size } });
    return response.data;
  },
  listByAccount: async (accountId: number): Promise<Card[]> => {
    const response = await api.get<Card[]>(`/cards/account/${accountId}`);
    return response.data;
  },
  get: async (cardNumber: string): Promise<Card> => {
    const response = await api.get<Card>(`/cards/${cardNumber}`);
    return response.data;
  },
  getStatistics: async (): Promise<CardStatistics> => {
    const response = await api.get<CardStatistics>('/cards/statistics');
    return response.data;
  },
};

export const transactionApi = {
  list: async (page = 0, size = 10): Promise<Page<Transaction>> => {
    const response = await api.get<Page<Transaction>>('/transactions', { params: { page, size } });
    return response.data;
  },
  listByCard: async (cardNumber: string, page = 0, size = 10): Promise<Page<Transaction>> => {
    const response = await api.get<Page<Transaction>>(`/transactions/card/${cardNumber}`, { params: { page, size } });
    return response.data;
  },
  get: async (transactionId: string): Promise<Transaction> => {
    const response = await api.get<Transaction>(`/transactions/${transactionId}`);
    return response.data;
  },
};

export const userApi = {
  list: async (page = 0, size = 10): Promise<Page<User>> => {
    const response = await api.get<Page<User>>('/users', { params: { page, size } });
    return response.data;
  },
  get: async (userId: string): Promise<User> => {
    const response = await api.get<User>(`/users/${userId}`);
    return response.data;
  },
  getStatistics: async (): Promise<UserStatistics> => {
    const response = await api.get<UserStatistics>('/users/statistics');
    return response.data;
  },
};

export const reportApi = {
  getAccountSummary: async (): Promise<AccountSummaryReport> => {
    const response = await api.get<AccountSummaryReport>('/reports/account-summary');
    return response.data;
  },
  getTransactionSummary: async (startDate: string, endDate: string): Promise<TransactionSummaryReport> => {
    const response = await api.get<TransactionSummaryReport>('/reports/transaction-summary', {
      params: { startDate, endDate },
    });
    return response.data;
  },
  getCardStatus: async (): Promise<CardStatusReport> => {
    const response = await api.get<CardStatusReport>('/reports/card-status');
    return response.data;
  },
};

export default api;
