import api from './api';
import { Card, ApiResponse, PagedResponse } from '../types';

export const cardService = {
  getAll: async (page = 0, size = 10): Promise<PagedResponse<Card>> => {
    const response = await api.get<ApiResponse<PagedResponse<Card>>>('/cards', {
      params: { page, size },
    });
    return response.data.data;
  },

  getByCardNumber: async (cardNumber: string): Promise<Card> => {
    const response = await api.get<ApiResponse<Card>>(`/cards/${cardNumber}`);
    return response.data.data;
  },

  getByAccountId: async (accountId: number): Promise<Card[]> => {
    const response = await api.get<ApiResponse<Card[]>>(`/cards/account/${accountId}`);
    return response.data.data;
  },

  getByCustomerId: async (customerId: number): Promise<Card[]> => {
    const response = await api.get<ApiResponse<Card[]>>(`/cards/customer/${customerId}`);
    return response.data.data;
  },

  getActive: async (page = 0, size = 10): Promise<PagedResponse<Card>> => {
    const response = await api.get<ApiResponse<PagedResponse<Card>>>('/cards/active', {
      params: { page, size },
    });
    return response.data.data;
  },

  getExpiringSoon: async (days = 30): Promise<Card[]> => {
    const response = await api.get<ApiResponse<Card[]>>('/cards/expiring-soon', {
      params: { days },
    });
    return response.data.data;
  },

  searchByLastFour: async (lastFour: string): Promise<Card[]> => {
    const response = await api.get<ApiResponse<Card[]>>('/cards/search', {
      params: { lastFour },
    });
    return response.data.data;
  },

  create: async (card: {
    accountId: number;
    customerId: number;
    embossedName: string;
    expirationDate: string;
  }): Promise<Card> => {
    const response = await api.post<ApiResponse<Card>>('/cards', card);
    return response.data.data;
  },

  update: async (cardNumber: string, card: Partial<Card>): Promise<Card> => {
    const response = await api.put<ApiResponse<Card>>(`/cards/${cardNumber}`, card);
    return response.data.data;
  },

  activate: async (cardNumber: string): Promise<Card> => {
    const response = await api.post<ApiResponse<Card>>(`/cards/${cardNumber}/activate`);
    return response.data.data;
  },

  deactivate: async (cardNumber: string): Promise<Card> => {
    const response = await api.post<ApiResponse<Card>>(`/cards/${cardNumber}/deactivate`);
    return response.data.data;
  },

  delete: async (cardNumber: string): Promise<void> => {
    await api.delete(`/cards/${cardNumber}`);
  },
};
