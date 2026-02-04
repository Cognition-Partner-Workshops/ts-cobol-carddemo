import { useState, useEffect, useCallback } from 'react';
import { authApi } from '../services/api';
import type { LoginRequest, LoginResponse, User } from '../types';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

export function useAuth() {
  const [state, setState] = useState<AuthState>({
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: true,
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr) as User;
        setState({
          user,
          token,
          isAuthenticated: true,
          isLoading: false,
        });
      } catch {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setState(prev => ({ ...prev, isLoading: false }));
      }
    } else {
      setState(prev => ({ ...prev, isLoading: false }));
    }
  }, []);

  const login = useCallback(async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await authApi.login(credentials);
    
    const user: User = {
      userId: response.userId,
      firstName: response.firstName,
      lastName: response.lastName,
      userType: response.userType,
      enabled: true,
      admin: response.userType === 'A',
    };

    localStorage.setItem('token', response.token);
    localStorage.setItem('user', JSON.stringify(user));

    setState({
      user,
      token: response.token,
      isAuthenticated: true,
      isLoading: false,
    });

    return response;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setState({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
    });
  }, []);

  return {
    ...state,
    login,
    logout,
  };
}
