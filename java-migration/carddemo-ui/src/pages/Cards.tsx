import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, ChevronLeft, ChevronRight, CreditCard } from 'lucide-react';
import { cardApi } from '../services/api';
import type { Card } from '../types';

export function Cards() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['cards', page],
    queryFn: () => cardApi.list(page, 10),
  });

  const filteredCards = data?.content.filter(card =>
    card.maskedCardNumber.includes(search) ||
    card.embossedName.toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Cards</h1>
          <p className="text-gray-500 mt-1">Manage credit cards</p>
        </div>
      </div>

      <div className="card">
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search by card number or name..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-10"
            />
          </div>
        </div>

        {isLoading ? (
          <div className="text-center py-12">
            <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full mx-auto" />
            <p className="text-gray-500 mt-4">Loading cards...</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredCards.map((card) => (
                <CardItem key={card.cardNumber} card={card} />
              ))}
            </div>

            <div className="flex items-center justify-between mt-6 pt-4 border-t border-gray-200">
              <p className="text-sm text-gray-500">
                Showing {filteredCards.length} of {data?.totalElements ?? 0} cards
              </p>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <span className="text-sm text-gray-600">
                  Page {page + 1} of {data?.totalPages ?? 1}
                </span>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= (data?.totalPages ?? 1) - 1}
                  className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function CardItem({ card }: { card: Card }) {
  const isActive = card.activeStatus === 'Y';
  const isExpired = card.expired;

  return (
    <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-xl p-6 text-white relative overflow-hidden">
      <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/2" />
      <div className="absolute bottom-0 left-0 w-24 h-24 bg-white/5 rounded-full translate-y-1/2 -translate-x-1/2" />
      
      <div className="relative">
        <div className="flex items-center justify-between mb-6">
          <CreditCard className="w-8 h-8" />
          <div className="flex gap-2">
            {!isActive && (
              <span className="px-2 py-1 bg-gray-600 rounded text-xs">Inactive</span>
            )}
            {isExpired && (
              <span className="px-2 py-1 bg-red-600 rounded text-xs">Expired</span>
            )}
          </div>
        </div>

        <div className="mb-6">
          <p className="text-xl tracking-widest font-mono">{card.maskedCardNumber}</p>
        </div>

        <div className="flex justify-between items-end">
          <div>
            <p className="text-xs text-gray-400 uppercase">Card Holder</p>
            <p className="font-medium">{card.embossedName}</p>
          </div>
          <div className="text-right">
            <p className="text-xs text-gray-400 uppercase">Expires</p>
            <p className="font-medium">{card.expirationDate}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
