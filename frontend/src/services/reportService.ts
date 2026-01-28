import api from './api';
import { DashboardSummary, AccountStatement, ApiResponse } from '../types';

interface TransactionReport {
  totalTransactions: number;
  purchaseCount: number;
  paymentCount: number;
  refundCount: number;
  transactionsByType: Record<string, number>;
  transactionsByDay: Record<string, number>;
  amountsByType: Record<string, number>;
}

export const reportService = {
  getDashboard: async (): Promise<DashboardSummary> => {
    const response = await api.get<ApiResponse<DashboardSummary>>('/reports/dashboard');
    return response.data.data;
  },

  getAccountStatement: async (
    accountId: number,
    startDate?: string,
    endDate?: string
  ): Promise<AccountStatement> => {
    const response = await api.get<ApiResponse<AccountStatement>>(
      `/reports/account/${accountId}/statement`,
      { params: { startDate, endDate } }
    );
    return response.data.data;
  },

  getTransactionReport: async (
    startDate?: string,
    endDate?: string
  ): Promise<TransactionReport> => {
    const response = await api.get<ApiResponse<TransactionReport>>('/reports/transactions', {
      params: { startDate, endDate },
    });
    return response.data.data;
  },
};
