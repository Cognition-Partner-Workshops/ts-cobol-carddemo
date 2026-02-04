import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';
import { transactionApi } from '../services/api';
import { format } from 'date-fns';
import type { Transaction } from '../types';

export function Transactions() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['transactions', page],
    queryFn: () => transactionApi.list(page, 10),
  });

  const filteredTransactions = data?.content.filter(txn =>
    txn.transactionId.toLowerCase().includes(search.toLowerCase()) ||
    txn.description?.toLowerCase().includes(search.toLowerCase()) ||
    txn.merchantName?.toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Transactions</h1>
          <p className="text-gray-500 mt-1">View transaction history</p>
        </div>
      </div>

      <div className="card">
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search transactions..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-10"
            />
          </div>
        </div>

        {isLoading ? (
          <div className="text-center py-12">
            <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full mx-auto" />
            <p className="text-gray-500 mt-4">Loading transactions...</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Transaction ID</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Date</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Description</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Merchant</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Card</th>
                    <th className="text-right py-3 px-4 font-medium text-gray-600">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredTransactions.map((txn) => (
                    <TransactionRow key={txn.transactionId} transaction={txn} />
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between mt-6 pt-4 border-t border-gray-200">
              <p className="text-sm text-gray-500">
                Showing {filteredTransactions.length} of {data?.totalElements ?? 0} transactions
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

function TransactionRow({ transaction }: { transaction: Transaction }) {
  const isCredit = transaction.amount > 0;
  const formattedDate = transaction.originTimestamp 
    ? format(new Date(transaction.originTimestamp), 'MMM dd, yyyy HH:mm')
    : '-';

  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      <td className="py-3 px-4">
        <span className="font-mono text-sm text-gray-600">{transaction.transactionId}</span>
      </td>
      <td className="py-3 px-4">
        <span className="text-gray-600">{formattedDate}</span>
      </td>
      <td className="py-3 px-4">
        <span className="text-gray-900">{transaction.description || '-'}</span>
      </td>
      <td className="py-3 px-4">
        <div>
          <p className="text-gray-900">{transaction.merchantName || '-'}</p>
          {transaction.merchantCity && (
            <p className="text-sm text-gray-500">{transaction.merchantCity}</p>
          )}
        </div>
      </td>
      <td className="py-3 px-4">
        <span className="font-mono text-sm">{transaction.maskedCardNumber || '-'}</span>
      </td>
      <td className="py-3 px-4 text-right">
        <span className={`font-medium ${isCredit ? 'text-green-600' : 'text-red-600'}`}>
          {isCredit ? '+' : ''}${Math.abs(transaction.amount).toLocaleString()}
        </span>
      </td>
    </tr>
  );
}
