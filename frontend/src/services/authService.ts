import api from './api';
import { LoginRequest, LoginResponse, ApiResponse, User } from '../types';

export const authService = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<ApiResponse<LoginResponse>>('/auth/login', credentials);
    return response.data.data;
  },

  register: async (data: {
    username: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Promise<User> => {
    const response = await api.post<ApiResponse<User>>('/auth/register', data);
    return response.data.data;
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await api.get<ApiResponse<User>>('/auth/me');
    return response.data.data;
  },

  changePassword: async (data: {
    currentPassword: string;
    newPassword: string;
  }): Promise<void> => {
    await api.post('/auth/change-password', data);
  },

  refreshToken: async (refreshToken: string): Promise<{ accessToken: string }> => {
    const response = await api.post<ApiResponse<{ accessToken: string }>>('/auth/refresh', {
      refreshToken,
    });
    return response.data.data;
  },
};
