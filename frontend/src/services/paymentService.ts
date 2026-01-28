import api from './api';
import { Payment, ApiResponse, PagedResponse } from '../types';

export const paymentService = {
  getAll: async (page = 0, size = 10): Promise<PagedResponse<Payment>> => {
    const response = await api.get<ApiResponse<PagedResponse<Payment>>>('/payments', {
      params: { page, size },
    });
    return response.data.data;
  },

  getById: async (id: number): Promise<Payment> => {
    const response = await api.get<ApiResponse<Payment>>(`/payments/${id}`);
    return response.data.data;
  },

  getByAccountId: async (
    accountId: number,
    page = 0,
    size = 10
  ): Promise<PagedResponse<Payment>> => {
    const response = await api.get<ApiResponse<PagedResponse<Payment>>>(
      `/payments/account/${accountId}`,
      { params: { page, size } }
    );
    return response.data.data;
  },

  getByStatus: async (
    status: string,
    page = 0,
    size = 10
  ): Promise<PagedResponse<Payment>> => {
    const response = await api.get<ApiResponse<PagedResponse<Payment>>>('/payments/by-status', {
      params: { status, page, size },
    });
    return response.data.data;
  },

  getPending: async (page = 0, size = 10): Promise<PagedResponse<Payment>> => {
    const response = await api.get<ApiResponse<PagedResponse<Payment>>>('/payments/pending', {
      params: { page, size },
    });
    return response.data.data;
  },

  getScheduled: async (page = 0, size = 10): Promise<PagedResponse<Payment>> => {
    const response = await api.get<ApiResponse<PagedResponse<Payment>>>('/payments/scheduled', {
      params: { page, size },
    });
    return response.data.data;
  },

  getTotalByAccount: async (accountId: number): Promise<number> => {
    const response = await api.get<ApiResponse<number>>(`/payments/account/${accountId}/total`);
    return response.data.data;
  },

  create: async (payment: {
    accountId: number;
    amount: number;
    paymentMethod: 'ACH' | 'DEBIT' | 'CHECK' | 'CASH';
    sourceAccount?: string;
    routingNumber?: string;
    scheduledDate?: string;
  }): Promise<Payment> => {
    const response = await api.post<ApiResponse<Payment>>('/payments', payment);
    return response.data.data;
  },

  process: async (id: number): Promise<Payment> => {
    const response = await api.post<ApiResponse<Payment>>(`/payments/${id}/process`);
    return response.data.data;
  },

  cancel: async (id: number): Promise<Payment> => {
    const response = await api.post<ApiResponse<Payment>>(`/payments/${id}/cancel`);
    return response.data.data;
  },
};
