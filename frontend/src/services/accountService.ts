import api from './api';
import { Account, ApiResponse, PagedResponse } from '../types';

interface AccountSummary {
  totalAccounts: number;
  activeAccounts: number;
  totalBalance: number;
  totalCreditLimit: number;
  overLimitCount: number;
}

export const accountService = {
  getAll: async (page = 0, size = 10): Promise<PagedResponse<Account>> => {
    const response = await api.get<ApiResponse<PagedResponse<Account>>>('/accounts', {
      params: { page, size },
    });
    return response.data.data;
  },

  getById: async (id: number): Promise<Account> => {
    const response = await api.get<ApiResponse<Account>>(`/accounts/${id}`);
    return response.data.data;
  },

  getByCustomerId: async (customerId: number): Promise<Account[]> => {
    const response = await api.get<ApiResponse<Account[]>>(`/accounts/customer/${customerId}`);
    return response.data.data;
  },

  getActive: async (page = 0, size = 10): Promise<PagedResponse<Account>> => {
    const response = await api.get<ApiResponse<PagedResponse<Account>>>('/accounts/active', {
      params: { page, size },
    });
    return response.data.data;
  },

  getOverLimit: async (page = 0, size = 10): Promise<PagedResponse<Account>> => {
    const response = await api.get<ApiResponse<PagedResponse<Account>>>('/accounts/over-limit', {
      params: { page, size },
    });
    return response.data.data;
  },

  getSummary: async (): Promise<AccountSummary> => {
    const response = await api.get<ApiResponse<AccountSummary>>('/accounts/summary');
    return response.data.data;
  },

  create: async (account: Omit<Account, 'id'>): Promise<Account> => {
    const response = await api.post<ApiResponse<Account>>('/accounts', account);
    return response.data.data;
  },

  update: async (id: number, account: Partial<Account>): Promise<Account> => {
    const response = await api.put<ApiResponse<Account>>(`/accounts/${id}`, account);
    return response.data.data;
  },

  activate: async (id: number): Promise<Account> => {
    const response = await api.post<ApiResponse<Account>>(`/accounts/${id}/activate`);
    return response.data.data;
  },

  deactivate: async (id: number): Promise<Account> => {
    const response = await api.post<ApiResponse<Account>>(`/accounts/${id}/deactivate`);
    return response.data.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/accounts/${id}`);
  },
};
