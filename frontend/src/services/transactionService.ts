import api from './api';
import { Transaction, ApiResponse, PagedResponse } from '../types';

interface TransactionSummary {
  totalTransactions: number;
  totalDebits: number;
  totalCredits: number;
  netAmount: number;
}

export const transactionService = {
  getAll: async (page = 0, size = 10): Promise<PagedResponse<Transaction>> => {
    const response = await api.get<ApiResponse<PagedResponse<Transaction>>>('/transactions', {
      params: { page, size },
    });
    return response.data.data;
  },

  getById: async (id: string): Promise<Transaction> => {
    const response = await api.get<ApiResponse<Transaction>>(`/transactions/${id}`);
    return response.data.data;
  },

  getByCardNumber: async (
    cardNumber: string,
    page = 0,
    size = 10
  ): Promise<PagedResponse<Transaction>> => {
    const response = await api.get<ApiResponse<PagedResponse<Transaction>>>(
      `/transactions/card/${cardNumber}`,
      { params: { page, size } }
    );
    return response.data.data;
  },

  getByDateRange: async (
    startDate: string,
    endDate: string,
    page = 0,
    size = 10
  ): Promise<PagedResponse<Transaction>> => {
    const response = await api.get<ApiResponse<PagedResponse<Transaction>>>(
      '/transactions/date-range',
      { params: { startDate, endDate, page, size } }
    );
    return response.data.data;
  },

  getByType: async (
    typeCode: string,
    page = 0,
    size = 10
  ): Promise<PagedResponse<Transaction>> => {
    const response = await api.get<ApiResponse<PagedResponse<Transaction>>>(
      '/transactions/by-type',
      { params: { typeCode, page, size } }
    );
    return response.data.data;
  },

  getByMerchant: async (
    merchantName: string,
    page = 0,
    size = 10
  ): Promise<PagedResponse<Transaction>> => {
    const response = await api.get<ApiResponse<PagedResponse<Transaction>>>(
      '/transactions/by-merchant',
      { params: { merchantName, page, size } }
    );
    return response.data.data;
  },

  getSummary: async (cardNumber: string): Promise<TransactionSummary> => {
    const response = await api.get<ApiResponse<TransactionSummary>>(
      `/transactions/card/${cardNumber}/summary`
    );
    return response.data.data;
  },

  create: async (transaction: {
    cardNumber: string;
    transactionTypeCode: string;
    transactionCategoryCode: number;
    transactionAmount: number;
    transactionDescription: string;
    merchantId?: string;
    merchantName?: string;
    merchantCity?: string;
    merchantZip?: string;
  }): Promise<Transaction> => {
    const response = await api.post<ApiResponse<Transaction>>('/transactions', transaction);
    return response.data.data;
  },
};
