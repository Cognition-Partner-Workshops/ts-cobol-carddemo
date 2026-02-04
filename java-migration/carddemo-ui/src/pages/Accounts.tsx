import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';
import { accountApi } from '../services/api';
import type { Account } from '../types';

export function Accounts() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['accounts', page],
    queryFn: () => accountApi.list(page, 10),
  });

  const filteredAccounts = data?.content.filter(account =>
    account.accountId.toString().includes(search) ||
    account.groupId?.toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Accounts</h1>
          <p className="text-gray-500 mt-1">Manage credit card accounts</p>
        </div>
      </div>

      <div className="card">
        <div className="flex flex-col sm:flex-row gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search by account ID or group..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-10"
            />
          </div>
        </div>

        {isLoading ? (
          <div className="text-center py-12">
            <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full mx-auto" />
            <p className="text-gray-500 mt-4">Loading accounts...</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Account ID</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Status</th>
                    <th className="text-right py-3 px-4 font-medium text-gray-600">Balance</th>
                    <th className="text-right py-3 px-4 font-medium text-gray-600">Credit Limit</th>
                    <th className="text-right py-3 px-4 font-medium text-gray-600">Available</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-600">Group</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAccounts.map((account) => (
                    <AccountRow key={account.accountId} account={account} />
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between mt-6 pt-4 border-t border-gray-200">
              <p className="text-sm text-gray-500">
                Showing {filteredAccounts.length} of {data?.totalElements ?? 0} accounts
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

function AccountRow({ account }: { account: Account }) {
  const isActive = account.activeStatus === 'Y';
  
  return (
    <tr className="border-b border-gray-100 hover:bg-gray-50">
      <td className="py-3 px-4">
        <span className="font-medium text-gray-900">{account.accountId}</span>
      </td>
      <td className="py-3 px-4">
        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
          isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
        }`}>
          {isActive ? 'Active' : 'Inactive'}
        </span>
      </td>
      <td className="py-3 px-4 text-right">
        <span className={account.overLimit ? 'text-red-600 font-medium' : ''}>
          ${account.currentBalance.toLocaleString()}
        </span>
      </td>
      <td className="py-3 px-4 text-right">
        ${account.creditLimit.toLocaleString()}
      </td>
      <td className="py-3 px-4 text-right">
        ${account.availableCredit.toLocaleString()}
      </td>
      <td className="py-3 px-4">
        <span className="text-gray-600">{account.groupId || '-'}</span>
      </td>
    </tr>
  );
}
