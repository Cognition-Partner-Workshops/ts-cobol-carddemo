import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { accountService } from '../services/accountService';
import { Account } from '../types';
import LoadingSpinner from '../components/common/LoadingSpinner';
import Pagination from '../components/common/Pagination';
import Modal from '../components/common/Modal';
import Alert from '../components/common/Alert';

export default function Accounts() {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<'all' | 'active' | 'overlimit'>('all');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['accounts', page, filter],
    queryFn: () => {
      if (filter === 'active') return accountService.getActive(page, 10);
      if (filter === 'overlimit') return accountService.getOverLimit(page, 10);
      return accountService.getAll(page, 10);
    },
  });

  const activateMutation = useMutation({
    mutationFn: accountService.activate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      setAlert({ type: 'success', message: 'Account activated' });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: accountService.deactivate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      setAlert({ type: 'success', message: 'Account deactivated' });
    },
  });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString();
  };

  const handleEdit = (account: Account) => {
    setSelectedAccount(account);
    setIsModalOpen(true);
  };

  const handleAddNew = () => {
    setSelectedAccount(null);
    setIsModalOpen(true);
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return <Alert type="error" message="Failed to load accounts" />;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Accounts</h1>
        <button onClick={handleAddNew} className="btn-primary">
          Add Account
        </button>
      </div>

      {alert && (
        <Alert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />
      )}

      <div className="card mb-6">
        <div className="flex gap-4">
          <button
            onClick={() => { setFilter('all'); setPage(0); }}
            className={`px-4 py-2 rounded ${filter === 'all' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            All Accounts
          </button>
          <button
            onClick={() => { setFilter('active'); setPage(0); }}
            className={`px-4 py-2 rounded ${filter === 'active' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Active Only
          </button>
          <button
            onClick={() => { setFilter('overlimit'); setPage(0); }}
            className={`px-4 py-2 rounded ${filter === 'overlimit' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Over Limit
          </button>
        </div>
      </div>

      <div className="card overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 table-header">ID</th>
              <th className="px-6 py-3 table-header">Status</th>
              <th className="px-6 py-3 table-header">Current Balance</th>
              <th className="px-6 py-3 table-header">Credit Limit</th>
              <th className="px-6 py-3 table-header">Available</th>
              <th className="px-6 py-3 table-header">Open Date</th>
              <th className="px-6 py-3 table-header">Expiration</th>
              <th className="px-6 py-3 table-header">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {data?.content.map((account) => {
              const available = account.creditLimit - account.currentBalance;
              const isOverLimit = account.currentBalance > account.creditLimit;
              
              return (
                <tr key={account.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    {account.id}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs rounded-full ${
                      account.activeStatus === 'Y' 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {account.activeStatus === 'Y' ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm ${isOverLimit ? 'text-red-600 font-semibold' : ''}`}>
                    {formatCurrency(account.currentBalance)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {formatCurrency(account.creditLimit)}
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm ${available < 0 ? 'text-red-600' : 'text-green-600'}`}>
                    {formatCurrency(available)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {formatDate(account.openDate)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {formatDate(account.expirationDate)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={() => handleEdit(account)}
                      className="text-blue-600 hover:text-blue-800 mr-3"
                    >
                      Edit
                    </button>
                    {account.activeStatus === 'Y' ? (
                      <button
                        onClick={() => deactivateMutation.mutate(account.id)}
                        className="text-red-600 hover:text-red-800"
                      >
                        Deactivate
                      </button>
                    ) : (
                      <button
                        onClick={() => activateMutation.mutate(account.id)}
                        className="text-green-600 hover:text-green-800"
                      >
                        Activate
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {data && (
          <Pagination
            currentPage={page}
            totalPages={data.totalPages}
            onPageChange={setPage}
          />
        )}
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={selectedAccount ? 'Edit Account' : 'Add Account'}
        size="md"
      >
        <AccountForm
          account={selectedAccount}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            setIsModalOpen(false);
            queryClient.invalidateQueries({ queryKey: ['accounts'] });
            setAlert({ type: 'success', message: selectedAccount ? 'Account updated' : 'Account created' });
          }}
        />
      </Modal>
    </div>
  );
}

interface AccountFormProps {
  account: Account | null;
  onClose: () => void;
  onSuccess: () => void;
}

function AccountForm({ account, onClose, onSuccess }: AccountFormProps) {
  const [formData, setFormData] = useState({
    creditLimit: account?.creditLimit || 5000,
    cashCreditLimit: account?.cashCreditLimit || 1000,
    expirationDate: account?.expirationDate?.split('T')[0] || '',
    groupId: account?.groupId || '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.type === 'number' ? Number(e.target.value) : e.target.value;
    setFormData({ ...formData, [e.target.name]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      if (account) {
        await accountService.update(account.id, formData);
      } else {
        await accountService.create({
          ...formData,
          activeStatus: 'Y',
          currentBalance: 0,
          openDate: new Date().toISOString(),
          currentCycleCredit: 0,
          currentCycleDebit: 0,
        } as Omit<Account, 'id'>);
      }
      onSuccess();
    } catch {
      setError('Failed to save account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <Alert type="error" message={error} />}
      
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Credit Limit</label>
        <input
          type="number"
          name="creditLimit"
          value={formData.creditLimit}
          onChange={handleChange}
          className="input-field"
          min={0}
          step={100}
          required
        />
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Cash Credit Limit</label>
        <input
          type="number"
          name="cashCreditLimit"
          value={formData.cashCreditLimit}
          onChange={handleChange}
          className="input-field"
          min={0}
          step={100}
          required
        />
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Expiration Date</label>
        <input
          type="date"
          name="expirationDate"
          value={formData.expirationDate}
          onChange={handleChange}
          className="input-field"
          required
        />
      </div>

      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-1">Group ID</label>
        <input
          type="text"
          name="groupId"
          value={formData.groupId}
          onChange={handleChange}
          className="input-field"
        />
      </div>

      <div className="flex justify-end gap-3">
        <button type="button" onClick={onClose} className="btn-secondary">
          Cancel
        </button>
        <button type="submit" disabled={loading} className="btn-primary">
          {loading ? 'Saving...' : 'Save'}
        </button>
      </div>
    </form>
  );
}
