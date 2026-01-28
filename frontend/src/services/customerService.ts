import api from './api';
import { Customer, ApiResponse, PagedResponse } from '../types';

export const customerService = {
  getAll: async (page = 0, size = 10): Promise<PagedResponse<Customer>> => {
    const response = await api.get<ApiResponse<PagedResponse<Customer>>>('/customers', {
      params: { page, size },
    });
    return response.data.data;
  },

  getById: async (id: number): Promise<Customer> => {
    const response = await api.get<ApiResponse<Customer>>(`/customers/${id}`);
    return response.data.data;
  },

  search: async (query: string, page = 0, size = 10): Promise<PagedResponse<Customer>> => {
    const response = await api.get<ApiResponse<PagedResponse<Customer>>>('/customers/search', {
      params: { query, page, size },
    });
    return response.data.data;
  },

  getByState: async (state: string, page = 0, size = 10): Promise<PagedResponse<Customer>> => {
    const response = await api.get<ApiResponse<PagedResponse<Customer>>>('/customers/by-state', {
      params: { state, page, size },
    });
    return response.data.data;
  },

  create: async (customer: Omit<Customer, 'id'>): Promise<Customer> => {
    const response = await api.post<ApiResponse<Customer>>('/customers', customer);
    return response.data.data;
  },

  update: async (id: number, customer: Partial<Customer>): Promise<Customer> => {
    const response = await api.put<ApiResponse<Customer>>(`/customers/${id}`, customer);
    return response.data.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/customers/${id}`);
  },
};
