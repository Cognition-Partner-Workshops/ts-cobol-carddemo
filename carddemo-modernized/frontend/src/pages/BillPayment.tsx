import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { api } from '../services/api';
import { Account, BillPaymentRequest } from '../types';
import { ArrowLeft, Loader2, DollarSign, CheckCircle } from 'lucide-react';

export default function BillPayment() {
  const navigate = useNavigate();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingAccounts, setIsLoadingAccounts] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<string>('');
  const [paymentAmount, setPaymentAmount] = useState<number>(0);

  useEffect(() => {
    loadAccounts();
  }, []);

  const loadAccounts = async () => {
    try {
      const response = await api.getAccounts();
      if (response.success) {
        setAccounts(response.data.filter(a => a.activeStatus === 'Y' && a.currentBalance > 0));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load accounts');
    } finally {
      setIsLoadingAccounts(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setIsLoading(true);

    try {
      const request: BillPaymentRequest = {
        accountId: selectedAccount,
        amount: paymentAmount,
      };
      const response = await api.processBillPayment(request);
      if (response.success) {
        setSuccess(true);
        setPaymentAmount(0);
        loadAccounts();
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process payment');
    } finally {
      setIsLoading(false);
    }
  };

  const selectedAccountData = accounts.find(a => a.accountId === selectedAccount);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  if (isLoadingAccounts) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="mb-6 flex items-center space-x-4">
        <button
          onClick={() => navigate('/dashboard')}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Bill Payment</h1>
          <p className="text-gray-600">Make a payment on your credit card account</p>
        </div>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center">
          <CheckCircle className="w-5 h-5 mr-2" />
          Payment processed successfully!
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Payment Details</h2>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Account
              </label>
              <select
                value={selectedAccount}
                onChange={(e) => setSelectedAccount(e.target.value)}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">Select an account</option>
                {accounts.map(account => (
                  <option key={account.accountId} value={account.accountId}>
                    {account.accountId} - Balance: {formatCurrency(account.currentBalance)}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Payment Amount
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <DollarSign className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  type="number"
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(Number(e.target.value))}
                  required
                  min="0.01"
                  max={selectedAccountData?.currentBalance || 0}
                  step="0.01"
                  className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="0.00"
                />
              </div>
              {selectedAccountData && (
                <p className="mt-1 text-sm text-gray-500">
                  Maximum: {formatCurrency(selectedAccountData.currentBalance)}
                </p>
              )}
            </div>

            <div className="flex space-x-4">
              {selectedAccountData && (
                <>
                  <button
                    type="button"
                    onClick={() => setPaymentAmount(selectedAccountData.currentBalance)}
                    className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                  >
                    Pay Full Balance
                  </button>
                  <button
                    type="button"
                    onClick={() => setPaymentAmount(Math.min(100, selectedAccountData.currentBalance))}
                    className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                  >
                    Pay $100
                  </button>
                </>
              )}
            </div>
          </div>

          <div className="mt-6">
            <button
              type="submit"
              disabled={isLoading || !selectedAccount || paymentAmount <= 0}
              className="w-full inline-flex items-center justify-center px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isLoading ? (
                <Loader2 className="w-5 h-5 mr-2 animate-spin" />
              ) : (
                <DollarSign className="w-5 h-5 mr-2" />
              )}
              Process Payment
            </button>
          </div>
        </form>

        {selectedAccountData && (
          <div className="bg-white rounded-xl shadow-md p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Account Summary</h2>
            <div className="space-y-4">
              <div className="flex justify-between py-2 border-b border-gray-100">
                <span className="text-gray-600">Account ID</span>
                <span className="font-medium">{selectedAccountData.accountId}</span>
              </div>
              <div className="flex justify-between py-2 border-b border-gray-100">
                <span className="text-gray-600">Current Balance</span>
                <span className="font-medium text-red-600">{formatCurrency(selectedAccountData.currentBalance)}</span>
              </div>
              <div className="flex justify-between py-2 border-b border-gray-100">
                <span className="text-gray-600">Credit Limit</span>
                <span className="font-medium">{formatCurrency(selectedAccountData.creditLimit)}</span>
              </div>
              <div className="flex justify-between py-2 border-b border-gray-100">
                <span className="text-gray-600">Available Credit</span>
                <span className="font-medium text-green-600">
                  {formatCurrency(selectedAccountData.creditLimit - selectedAccountData.currentBalance)}
                </span>
              </div>
              <div className="flex justify-between py-2">
                <span className="text-gray-600">Payment Amount</span>
                <span className="font-bold text-blue-600">{formatCurrency(paymentAmount)}</span>
              </div>
              {paymentAmount > 0 && (
                <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                  <p className="text-sm text-blue-800">
                    After payment, your new balance will be:{' '}
                    <span className="font-bold">
                      {formatCurrency(selectedAccountData.currentBalance - paymentAmount)}
                    </span>
                  </p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
