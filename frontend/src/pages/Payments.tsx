import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentService } from '../services/paymentService';
import LoadingSpinner from '../components/common/LoadingSpinner';
import Pagination from '../components/common/Pagination';
import Modal from '../components/common/Modal';
import Alert from '../components/common/Alert';

export default function Payments() {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<'all' | 'pending' | 'scheduled'>('all');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['payments', page, filter],
    queryFn: () => {
      if (filter === 'pending') return paymentService.getPending(page, 10);
      if (filter === 'scheduled') return paymentService.getScheduled(page, 10);
      return paymentService.getAll(page, 10);
    },
  });

  const processMutation = useMutation({
    mutationFn: paymentService.process,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setAlert({ type: 'success', message: 'Payment processed successfully' });
    },
    onError: () => {
      setAlert({ type: 'error', message: 'Failed to process payment' });
    },
  });

  const cancelMutation = useMutation({
    mutationFn: paymentService.cancel,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setAlert({ type: 'success', message: 'Payment cancelled' });
    },
    onError: () => {
      setAlert({ type: 'error', message: 'Failed to cancel payment' });
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

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'SCHEDULED': return 'bg-blue-100 text-blue-800';
      case 'FAILED': return 'bg-red-100 text-red-800';
      case 'CANCELLED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const maskAccount = (account?: string) => {
    if (!account || account.length < 4) return '****';
    return `****${account.slice(-4)}`;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return <Alert type="error" message="Failed to load payments" />;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Payments</h1>
        <button onClick={() => setIsModalOpen(true)} className="btn-primary">
          Make Payment
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
            All Payments
          </button>
          <button
            onClick={() => { setFilter('pending'); setPage(0); }}
            className={`px-4 py-2 rounded ${filter === 'pending' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Pending
          </button>
          <button
            onClick={() => { setFilter('scheduled'); setPage(0); }}
            className={`px-4 py-2 rounded ${filter === 'scheduled' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Scheduled
          </button>
        </div>
      </div>

      <div className="card overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 table-header">ID</th>
              <th className="px-6 py-3 table-header">Account ID</th>
              <th className="px-6 py-3 table-header">Amount</th>
              <th className="px-6 py-3 table-header">Method</th>
              <th className="px-6 py-3 table-header">Source Account</th>
              <th className="px-6 py-3 table-header">Status</th>
              <th className="px-6 py-3 table-header">Date</th>
              <th className="px-6 py-3 table-header">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {data?.content.map((payment) => (
              <tr key={payment.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  {payment.id}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {payment.accountId}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-green-600">
                  {formatCurrency(payment.amount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {payment.paymentMethod}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-mono">
                  {maskAccount(payment.sourceAccount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(payment.status)}`}>
                    {payment.status}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {payment.processedDate 
                    ? formatDate(payment.processedDate)
                    : payment.scheduledDate 
                      ? `Scheduled: ${formatDate(payment.scheduledDate)}`
                      : formatDate(payment.createdAt)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {(payment.status === 'PENDING' || payment.status === 'SCHEDULED') && (
                    <>
                      <button
                        onClick={() => processMutation.mutate(payment.id)}
                        className="text-green-600 hover:text-green-800 mr-3"
                      >
                        Process
                      </button>
                      <button
                        onClick={() => cancelMutation.mutate(payment.id)}
                        className="text-red-600 hover:text-red-800"
                      >
                        Cancel
                      </button>
                    </>
                  )}
                  {payment.confirmationNumber && (
                    <span className="text-gray-500 text-xs">
                      Conf: {payment.confirmationNumber}
                    </span>
                  )}
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
        title="Make Payment"
        size="md"
      >
        <PaymentForm
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            setIsModalOpen(false);
            queryClient.invalidateQueries({ queryKey: ['payments'] });
            setAlert({ type: 'success', message: 'Payment created successfully' });
          }}
        />
      </Modal>
    </div>
  );
}

interface PaymentFormProps {
  onClose: () => void;
  onSuccess: () => void;
}

function PaymentForm({ onClose, onSuccess }: PaymentFormProps) {
  const [formData, setFormData] = useState({
    accountId: 0,
    amount: 0,
    paymentMethod: 'ACH' as const,
    sourceAccount: '',
    routingNumber: '',
    scheduledDate: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const value = e.target.type === 'number' ? Number(e.target.value) : e.target.value;
    setFormData({ ...formData, [e.target.name]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await paymentService.create({
        ...formData,
        scheduledDate: formData.scheduledDate || undefined,
      });
      onSuccess();
    } catch {
      setError('Failed to create payment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <Alert type="error" message={error} />}
      
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Account ID</label>
        <input
          type="number"
          name="accountId"
          value={formData.accountId}
          onChange={handleChange}
          className="input-field"
          required
        />
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Payment Amount</label>
        <input
          type="number"
          name="amount"
          value={formData.amount}
          onChange={handleChange}
          className="input-field"
          min={0.01}
          step={0.01}
          required
        />
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Payment Method</label>
        <select
          name="paymentMethod"
          value={formData.paymentMethod}
          onChange={handleChange}
          className="input-field"
        >
          <option value="ACH">ACH Transfer</option>
          <option value="DEBIT">Debit Card</option>
          <option value="CHECK">Check</option>
          <option value="CASH">Cash</option>
        </select>
      </div>

      {(formData.paymentMethod === 'ACH' || formData.paymentMethod === 'DEBIT') && (
        <>
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">Source Account Number</label>
            <input
              type="text"
              name="sourceAccount"
              value={formData.sourceAccount}
              onChange={handleChange}
              className="input-field"
              required
            />
          </div>

          {formData.paymentMethod === 'ACH' && (
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Routing Number</label>
              <input
                type="text"
                name="routingNumber"
                value={formData.routingNumber}
                onChange={handleChange}
                className="input-field"
                maxLength={9}
                required
              />
            </div>
          )}
        </>
      )}

      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Schedule Date (optional)
        </label>
        <input
          type="date"
          name="scheduledDate"
          value={formData.scheduledDate}
          onChange={handleChange}
          className="input-field"
          min={new Date().toISOString().split('T')[0]}
        />
        <p className="text-xs text-gray-500 mt-1">Leave empty to process immediately</p>
      </div>

      <div className="flex justify-end gap-3">
        <button type="button" onClick={onClose} className="btn-secondary">
          Cancel
        </button>
        <button type="submit" disabled={loading} className="btn-primary">
          {loading ? 'Processing...' : 'Submit Payment'}
        </button>
      </div>
    </form>
  );
}
