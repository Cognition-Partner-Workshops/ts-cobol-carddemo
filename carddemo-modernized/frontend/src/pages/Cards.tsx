import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Layout from '../components/Layout';
import { api } from '../services/api';
import { Card } from '../types';
import { Eye, Edit, Trash2, Plus, Loader2, CreditCard } from 'lucide-react';

export default function Cards() {
  const [cards, setCards] = useState<Card[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadCards();
  }, []);

  const loadCards = async () => {
    try {
      const response = await api.getCards();
      if (response.success) {
        setCards(response.data);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load cards');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (cardNumber: string) => {
    if (!confirm('Are you sure you want to delete this card?')) return;
    
    try {
      await api.deleteCard(cardNumber);
      setCards(cards.filter(c => c.cardNumber !== cardNumber));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete card');
    }
  };

  const maskCardNumber = (cardNumber: string) => {
    return `****-****-****-${cardNumber.slice(-4)}`;
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

  return (
    <Layout>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Credit Cards</h1>
          <p className="text-gray-600">Manage credit cards</p>
        </div>
        <Link
          to="/cards/new"
          className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-5 h-5 mr-2" />
          Add Card
        </Link>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {cards.map((card) => (
          <div key={card.cardNumber} className="bg-gradient-to-br from-blue-600 to-indigo-700 rounded-xl shadow-lg p-6 text-white">
            <div className="flex items-center justify-between mb-6">
              <CreditCard className="w-10 h-10" />
              <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                card.activeStatus === 'Y' 
                  ? 'bg-green-400 text-green-900' 
                  : 'bg-red-400 text-red-900'
              }`}>
                {card.activeStatus === 'Y' ? 'Active' : 'Inactive'}
              </span>
            </div>
            <div className="mb-4">
              <p className="text-xl font-mono tracking-wider">{maskCardNumber(card.cardNumber)}</p>
            </div>
            <div className="flex justify-between items-end">
              <div>
                <p className="text-xs text-blue-200">Card Holder</p>
                <p className="font-medium">{card.embossedName}</p>
              </div>
              <div className="text-right">
                <p className="text-xs text-blue-200">Expires</p>
                <p className="font-medium">{card.expirationDate}</p>
              </div>
            </div>
            <div className="mt-4 pt-4 border-t border-blue-400 flex justify-end space-x-2">
              <Link
                to={`/cards/${card.cardNumber}`}
                className="p-2 bg-white/20 hover:bg-white/30 rounded-lg transition-colors"
                title="View"
              >
                <Eye className="w-4 h-4" />
              </Link>
              <Link
                to={`/cards/${card.cardNumber}/edit`}
                className="p-2 bg-white/20 hover:bg-white/30 rounded-lg transition-colors"
                title="Edit"
              >
                <Edit className="w-4 h-4" />
              </Link>
              <button
                onClick={() => handleDelete(card.cardNumber)}
                className="p-2 bg-white/20 hover:bg-red-500 rounded-lg transition-colors"
                title="Delete"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
        {cards.length === 0 && (
          <div className="col-span-full text-center py-12 text-gray-500">
            No cards found
          </div>
        )}
      </div>
    </Layout>
  );
}
