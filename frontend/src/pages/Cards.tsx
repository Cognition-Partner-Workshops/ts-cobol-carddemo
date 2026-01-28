import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cardService } from '../services/cardService';
import { Card } from '../types';
import LoadingSpinner from '../components/common/LoadingSpinner';
import Pagination from '../components/common/Pagination';
import Modal from '../components/common/Modal';
import Alert from '../components/common/Alert';

export default function Cards() {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<'all' | 'active' | 'expiring'>('all');
  const [searchLastFour, setSearchLastFour] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedCard, setSelectedCard] = useState<Card | null>(null);
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['cards', page, filter],
    queryFn: () => {
      if (filter === 'active') return cardService.getActive(page, 10);
      return cardService.getAll(page, 10);
    },
    enabled: !searchLastFour,
  });

  const { data: searchResults } = useQuery({
    queryKey: ['cards', 'search', searchLastFour],
    queryFn: () => cardService.searchByLastFour(searchLastFour),
    enabled: searchLastFour.length === 4,
  });

  const { data: expiringCards } = useQuery({
    queryKey: ['cards', 'expiring'],
    queryFn: () => cardService.getExpiringSoon(30),
    enabled: filter === 'expiring',
  });

  const activateMutation = useMutation({
    mutationFn: cardService.activate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cards'] });
      setAlert({ type: 'success', message: 'Card activated' });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: cardService.deactivate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cards'] });
      setAlert({ type: 'success', message: 'Card deactivated' });
    },
  });

  const maskCardNumber = (cardNumber: string) => {
    if (!cardNumber || cardNumber.length < 4) return '****';
    return `**** **** **** ${cardNumber.slice(-4)}`;
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString();
  };

  const handleEdit = (card: Card) => {
    setSelectedCard(card);
    setIsModalOpen(true);
  };

  const handleAddNew = () => {
    setSelectedCard(null);
    setIsModalOpen(true);
  };

  const displayCards = searchLastFour.length === 4 
    ? searchResults 
    : filter === 'expiring' 
      ? expiringCards 
      : data?.content;

  if (isLoading && !searchLastFour) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return <Alert type="error" message="Failed to load cards" />;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Cards</h1>
        <button onClick={handleAddNew} className="btn-primary">
          Issue New Card
        </button>
      </div>

      {alert && (
        <Alert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />
      )}

      <div className="card mb-6">
        <div className="flex gap-4 items-center">
          <button
            onClick={() => { setFilter('all'); setPage(0); setSearchLastFour(''); }}
            className={`px-4 py-2 rounded ${filter === 'all' && !searchLastFour ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            All Cards
          </button>
          <button
            onClick={() => { setFilter('active'); setPage(0); setSearchLastFour(''); }}
            className={`px-4 py-2 rounded ${filter === 'active' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Active Only
          </button>
          <button
            onClick={() => { setFilter('expiring'); setSearchLastFour(''); }}
            className={`px-4 py-2 rounded ${filter === 'expiring' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Expiring Soon
          </button>
          <div className="ml-auto flex gap-2">
            <input
              type="text"
              placeholder="Last 4 digits..."
              value={searchLastFour}
              onChange={(e) => setSearchLastFour(e.target.value.replace(/\D/g, '').slice(0, 4))}
              className="input-field w-40"
              maxLength={4}
            />
            {searchLastFour && (
              <button onClick={() => setSearchLastFour('')} className="btn-secondary">
                Clear
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="card overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 table-header">Card Number</th>
              <th className="px-6 py-3 table-header">Embossed Name</th>
              <th className="px-6 py-3 table-header">Account ID</th>
              <th className="px-6 py-3 table-header">Customer ID</th>
              <th className="px-6 py-3 table-header">Status</th>
              <th className="px-6 py-3 table-header">Expiration</th>
              <th className="px-6 py-3 table-header">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {displayCards?.map((card) => {
              const isExpiringSoon = new Date(card.expirationDate) <= new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
              
              return (
                <tr key={card.cardNumber} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-mono">
                    {maskCardNumber(card.cardNumber)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {card.embossedName}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {card.accountId}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    {card.customerId}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs rounded-full ${
                      card.activeStatus === 'Y' 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {card.activeStatus === 'Y' ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm ${isExpiringSoon ? 'text-orange-600 font-semibold' : ''}`}>
                    {formatDate(card.expirationDate)}
                    {isExpiringSoon && ' (Soon)'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={() => handleEdit(card)}
                      className="text-blue-600 hover:text-blue-800 mr-3"
                    >
                      Edit
                    </button>
                    {card.activeStatus === 'Y' ? (
                      <button
                        onClick={() => deactivateMutation.mutate(card.cardNumber)}
                        className="text-red-600 hover:text-red-800"
                      >
                        Deactivate
                      </button>
                    ) : (
                      <button
                        onClick={() => activateMutation.mutate(card.cardNumber)}
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

        {data && !searchLastFour && filter !== 'expiring' && (
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
        title={selectedCard ? 'Edit Card' : 'Issue New Card'}
        size="md"
      >
        <CardForm
          card={selectedCard}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            setIsModalOpen(false);
            queryClient.invalidateQueries({ queryKey: ['cards'] });
            setAlert({ type: 'success', message: selectedCard ? 'Card updated' : 'Card issued' });
          }}
        />
      </Modal>
    </div>
  );
}

interface CardFormProps {
  card: Card | null;
  onClose: () => void;
  onSuccess: () => void;
}

function CardForm({ card, onClose, onSuccess }: CardFormProps) {
  const [formData, setFormData] = useState({
    accountId: card?.accountId || 0,
    customerId: card?.customerId || 0,
    embossedName: card?.embossedName || '',
    expirationDate: card?.expirationDate?.split('T')[0] || '',
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
      if (card) {
        await cardService.update(card.cardNumber, {
          embossedName: formData.embossedName,
          expirationDate: formData.expirationDate,
        });
      } else {
        await cardService.create(formData);
      }
      onSuccess();
    } catch {
      setError('Failed to save card');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <Alert type="error" message={error} />}
      
      {!card && (
        <>
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
            <label className="block text-sm font-medium text-gray-700 mb-1">Customer ID</label>
            <input
              type="number"
              name="customerId"
              value={formData.customerId}
              onChange={handleChange}
              className="input-field"
              required
            />
          </div>
        </>
      )}

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Embossed Name</label>
        <input
          type="text"
          name="embossedName"
          value={formData.embossedName}
          onChange={handleChange}
          className="input-field"
          maxLength={26}
          required
        />
        <p className="text-xs text-gray-500 mt-1">Max 26 characters (as printed on card)</p>
      </div>

      <div className="mb-6">
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

      <div className="flex justify-end gap-3">
        <button type="button" onClick={onClose} className="btn-secondary">
          Cancel
        </button>
        <button type="submit" disabled={loading} className="btn-primary">
          {loading ? 'Saving...' : card ? 'Update' : 'Issue Card'}
        </button>
      </div>
    </form>
  );
}
