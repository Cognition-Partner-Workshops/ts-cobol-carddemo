import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Layout from '../components/Layout';
import { api } from '../services/api';
import { Account } from '../types';
import { ArrowLeft, Edit, Loader2 } from 'lucide-react';

export default function AccountDetail() {
  const { accountId } = useParams<{ accountId: string }>();
  const [account, setAccount] = useState<Account | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (accountId) {
      loadAccount(accountId);
    }
  }, [accountId]);

  const loadAccount = async (id: string) => {
    try {
      const response = await api.getAccount(id);
      if (response.success) {
        setAccount(response.data);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load account');
    } finally {
      setIsLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
        </div>
      </Layout>
    );
  }

  if (error || !account) {
    return (
      <Layout>
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error || 'Account not found'}
        </div>
        <Link to="/accounts" className="mt-4 inline-flex items-center text-blue-600 hover:text-blue-800">
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Accounts
        </Link>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link to="/accounts" className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <ArrowLeft className="w-5 h-5 text-gray-600" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Account Details</h1>
            <p className="text-gray-600">Account ID: {account.accountId}</p>
          </div>
        </div>
        <Link
          to={`/accounts/${account.accountId}/edit`}
          className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Edit className="w-5 h-5 mr-2" />
          Edit Account
        </Link>
      </div>

      <div className="bg-white rounded-xl shadow-md p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-500">Account ID</label>
            <p className="mt-1 text-lg font-semibold text-gray-900">{account.accountId}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Status</label>
            <span className={`mt-1 inline-flex px-3 py-1 text-sm font-semibold rounded-full ${
              account.activeStatus === 'Y' 
                ? 'bg-green-100 text-green-800' 
                : 'bg-red-100 text-red-800'
            }`}>
              {account.activeStatus === 'Y' ? 'Active' : 'Inactive'}
            </span>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Group ID</label>
            <p className="mt-1 text-lg text-gray-900">{account.groupId || 'N/A'}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Current Balance</label>
            <p className="mt-1 text-lg font-semibold text-gray-900">{formatCurrency(account.currentBalance)}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Credit Limit</label>
            <p className="mt-1 text-lg text-gray-900">{formatCurrency(account.creditLimit)}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Cash Credit Limit</label>
            <p className="mt-1 text-lg text-gray-900">{formatCurrency(account.cashCreditLimit)}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Current Cycle Credit</label>
            <p className="mt-1 text-lg text-gray-900">{formatCurrency(account.currentCycleCredit)}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Current Cycle Debit</label>
            <p className="mt-1 text-lg text-gray-900">{formatCurrency(account.currentCycleDebit)}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Zip Code</label>
            <p className="mt-1 text-lg text-gray-900">{account.zipCode}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Open Date</label>
            <p className="mt-1 text-lg text-gray-900">{account.openDate}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Expiration Date</label>
            <p className="mt-1 text-lg text-gray-900">{account.expirationDate}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-500">Reissue Date</label>
            <p className="mt-1 text-lg text-gray-900">{account.reissueDate || 'N/A'}</p>
          </div>
        </div>
      </div>
    </Layout>
  );
}
