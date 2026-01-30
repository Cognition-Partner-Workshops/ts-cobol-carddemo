import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { transactionService } from '../services/transactionService';
import { Transaction } from '../types';
import LoadingSpinner from '../components/common/LoadingSpinner';
import Pagination from '../components/common/Pagination';
import Modal from '../components/common/Modal';
import Alert from '../components/common/Alert';

export default function Transactions() {
  const [page, setPage] = useState(0);
  const [cardNumber, setCardNumber] = useState('');
  const [dateRange, setDateRange] = useState({ start: '', end: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['transactions', page, cardNumber, dateRange],
    queryFn: () => {
      if (cardNumber) {
        return transactionService.getByCardNumber(cardNumber, page, 10);
      }
      if (dateRange.start && dateRange.end) {
        return transactionService.getByDateRange(dateRange.start, dateRange.end, page, 10);
      }
      return transactionService.getAll(page, 10);
    },
  });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatDateTime = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const maskCardNumber = (cardNum: string) => {
    if (!cardNum || cardNum.length < 4) return '****';
    return `**** ${cardNum.slice(-4)}`;
  };

  const handleViewDetails = (transaction: Transaction) => {
    setSelectedTransaction(transaction);
    setIsModalOpen(true);
  };

  const handleAddNew = () => {
    setSelectedTransaction(null);
    setIsModalOpen(true);
  };

  const clearFilters = () => {
    setCardNumber('');
    setDateRange({ start: '', end: '' });
    setPage(0);
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return <Alert type="error" message="Failed to load transactions" />;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Transactions</h1>
        <button onClick={handleAddNew} className="btn-primary">
          Add Transaction
        </button>
      </div>

      {alert && (
        <Alert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />
      )}

      <div className="card mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Card Number</label>
            <input
              type="text"
              placeholder="Enter card number..."
              value={cardNumber}
              onChange={(e) => { setCardNumber(e.target.value); setPage(0); }}
              className="input-field"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
            <input
              type="date"
              value={dateRange.start}
              onChange={(e) => { setDateRange({ ...dateRange, start: e.target.value }); setPage(0); }}
              className="input-field"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
            <input
              type="date"
              value={dateRange.end}
              onChange={(e) => { setDateRange({ ...dateRange, end: e.target.value }); setPage(0); }}
              className="input-field"
            />
          </div>
          <div className="flex items-end">
            <button onClick={clearFilters} className="btn-secondary w-full">
              Clear Filters
            </button>
          </div>
        </div>
      </div>

      <div className="card overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 table-header">ID</th>
              <th className="px-6 py-3 table-header">Card</th>
              <th className="px-6 py-3 table-header">Type</th>
              <th className="px-6 py-3 table-header">Description</th>
              <th className="px-6 py-3 table-header">Amount</th>
              <th className="px-6 py-3 table-header">Merchant</th>
              <th className="px-6 py-3 table-header">Date</th>
              <th className="px-6 py-3 table-header">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {data?.content.map((transaction) => (
              <tr key={transaction.id || transaction.transactionId} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono">
                  {String(transaction.id || transaction.transactionId || '').slice(0, 8)}...
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono">
                  {maskCardNumber(transaction.cardNumber)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs rounded-full ${
                    transaction.transactionTypeCode === 'CR' 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-blue-100 text-blue-800'
                  }`}>
                    {transaction.transactionTypeCode}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm max-w-xs truncate">
                  {transaction.transactionDescription || transaction.description || '-'}
                </td>
                <td className={`px-6 py-4 whitespace-nowrap text-sm font-semibold ${
                  transaction.transactionTypeCode === 'CR' ? 'text-green-600' : 'text-red-600'
                }`}>
                  {transaction.transactionTypeCode === 'CR' ? '+' : '-'}
                  {formatCurrency(transaction.transactionAmount ?? transaction.amount ?? 0)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {transaction.merchantName || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {formatDateTime(transaction.originTimestamp || transaction.originationTimestamp || '')}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <button
                    onClick={() => handleViewDetails(transaction)}
                    className="text-blue-600 hover:text-blue-800"
                  >
                    Details
                  </button>
                </td>
              </tr>
            ))}
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
        title={selectedTransaction ? 'Transaction Details' : 'Add Transaction'}
        size="md"
      >
        {selectedTransaction ? (
          <TransactionDetails transaction={selectedTransaction} />
        ) : (
          <TransactionForm
            onClose={() => setIsModalOpen(false)}
            onSuccess={() => {
              setIsModalOpen(false);
              queryClient.invalidateQueries({ queryKey: ['transactions'] });
              setAlert({ type: 'success', message: 'Transaction created' });
            }}
          />
        )}
      </Modal>
    </div>
  );
}

function TransactionDetails({ transaction }: { transaction: Transaction }) {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  };

  const getAmount = () => transaction.transactionAmount ?? transaction.amount ?? 0;
  const getDescription = () => transaction.transactionDescription ?? transaction.description ?? '';
  const getOriginTime = () => transaction.originTimestamp ?? transaction.originationTimestamp ?? '';

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-sm text-gray-500">Transaction ID</p>
          <p className="font-mono">{transaction.id || transaction.transactionId}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">Card Number</p>
          <p className="font-mono">**** **** **** {transaction.cardNumber?.slice(-4) || '****'}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">Type</p>
          <p>{transaction.transactionTypeCode}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">Category</p>
          <p>{transaction.transactionCategoryCode}</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">Amount</p>
          <p className={`font-semibold ${
            transaction.transactionTypeCode === 'CR' ? 'text-green-600' : 'text-red-600'
          }`}>
            {formatCurrency(getAmount())}
          </p>
        </div>
        <div>
          <p className="text-sm text-gray-500">Source</p>
          <p>{transaction.transactionSource}</p>
        </div>
      </div>
      
      <div>
        <p className="text-sm text-gray-500">Description</p>
        <p>{getDescription()}</p>
      </div>

      {transaction.merchantName && (
        <div className="border-t pt-4">
          <p className="text-sm font-medium text-gray-700 mb-2">Merchant Information</p>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-gray-500">Name</p>
              <p>{transaction.merchantName}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">ID</p>
              <p>{transaction.merchantId || '-'}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">City</p>
              <p>{transaction.merchantCity || '-'}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">ZIP</p>
              <p>{transaction.merchantZip || '-'}</p>
            </div>
          </div>
        </div>
      )}

      <div className="border-t pt-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-gray-500">Origin Time</p>
            <p>{getOriginTime() ? new Date(getOriginTime()).toLocaleString() : '-'}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Processed Time</p>
            <p>{transaction.processingTimestamp 
              ? new Date(transaction.processingTimestamp).toLocaleString() 
              : 'Pending'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

interface TransactionFormProps {
  onClose: () => void;
  onSuccess: () => void;
}

function TransactionForm({ onClose, onSuccess }: TransactionFormProps) {
  const [formData, setFormData] = useState({
    cardNumber: '',
    transactionTypeCode: 'DR',
    transactionCategoryCode: 1,
    transactionAmount: 0,
    transactionDescription: '',
    merchantId: '',
    merchantName: '',
    merchantCity: '',
    merchantZip: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const value = e.target.type === 'number' ? Number(e.target.value) : e.target.value;
    setFormData({ ...formData, [e.target.name]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await transactionService.create(formData);
      onSuccess();
    } catch {
      setError('Failed to create transaction');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <Alert type="error" message={error} />}
      
      <div className="grid grid-cols-2 gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Card Number</label>
          <input
            type="text"
            name="cardNumber"
            value={formData.cardNumber}
            onChange={handleChange}
            className="input-field"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
          <select
            name="transactionTypeCode"
            value={formData.transactionTypeCode}
            onChange={handleChange}
            className="input-field"
          >
            <option value="DR">Debit (DR)</option>
            <option value="CR">Credit (CR)</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Amount</label>
          <input
            type="number"
            name="transactionAmount"
            value={formData.transactionAmount}
            onChange={handleChange}
            className="input-field"
            min={0}
            step={0.01}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Category Code</label>
          <input
            type="number"
            name="transactionCategoryCode"
            value={formData.transactionCategoryCode}
            onChange={handleChange}
            className="input-field"
            min={1}
            required
          />
        </div>
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
        <textarea
          name="transactionDescription"
          value={formData.transactionDescription}
          onChange={handleChange}
          className="input-field"
          rows={2}
          required
        />
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Merchant Name</label>
          <input
            type="text"
            name="merchantName"
            value={formData.merchantName}
            onChange={handleChange}
            className="input-field"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Merchant ID</label>
          <input
            type="text"
            name="merchantId"
            value={formData.merchantId}
            onChange={handleChange}
            className="input-field"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Merchant City</label>
          <input
            type="text"
            name="merchantCity"
            value={formData.merchantCity}
            onChange={handleChange}
            className="input-field"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Merchant ZIP</label>
          <input
            type="text"
            name="merchantZip"
            value={formData.merchantZip}
            onChange={handleChange}
            className="input-field"
          />
        </div>
      </div>

      <div className="flex justify-end gap-3">
        <button type="button" onClick={onClose} className="btn-secondary">
          Cancel
        </button>
        <button type="submit" disabled={loading} className="btn-primary">
          {loading ? 'Creating...' : 'Create Transaction'}
        </button>
      </div>
    </form>
  );
}
