import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { reportService } from '../services/reportService';
import Alert from '../components/common/Alert';

export default function Reports() {
  const [activeTab, setActiveTab] = useState<'statement' | 'transactions'>('statement');
  const [accountId, setAccountId] = useState('');
  const [dateRange, setDateRange] = useState({
    start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    end: new Date().toISOString().split('T')[0],
  });

  const { data: statement, isLoading: statementLoading, error: statementError, refetch: refetchStatement } = useQuery({
    queryKey: ['statement', accountId, dateRange],
    queryFn: () => reportService.getAccountStatement(
      Number(accountId),
      dateRange.start,
      dateRange.end
    ),
    enabled: false,
  });

  const { data: transactionReport, isLoading: reportLoading, error: reportError, refetch: refetchReport } = useQuery({
    queryKey: ['transactionReport', dateRange],
    queryFn: () => reportService.getTransactionReport(dateRange.start, dateRange.end),
    enabled: false,
  });

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const handleGenerateStatement = () => {
    if (accountId) {
      refetchStatement();
    }
  };

  const handleGenerateReport = () => {
    refetchReport();
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Reports</h1>

      <div className="card mb-6">
        <div className="flex gap-4 border-b pb-4 mb-4">
          <button
            onClick={() => setActiveTab('statement')}
            className={`px-4 py-2 rounded ${activeTab === 'statement' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Account Statement
          </button>
          <button
            onClick={() => setActiveTab('transactions')}
            className={`px-4 py-2 rounded ${activeTab === 'transactions' ? 'bg-blue-600 text-white' : 'bg-gray-200'}`}
          >
            Transaction Report
          </button>
        </div>

        {activeTab === 'statement' && (
          <div>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Account ID</label>
                <input
                  type="number"
                  value={accountId}
                  onChange={(e) => setAccountId(e.target.value)}
                  className="input-field"
                  placeholder="Enter account ID"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
                <input
                  type="date"
                  value={dateRange.start}
                  onChange={(e) => setDateRange({ ...dateRange, start: e.target.value })}
                  className="input-field"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
                <input
                  type="date"
                  value={dateRange.end}
                  onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
                  className="input-field"
                />
              </div>
              <div className="flex items-end">
                <button
                  onClick={handleGenerateStatement}
                  disabled={!accountId || statementLoading}
                  className="btn-primary w-full"
                >
                  {statementLoading ? 'Loading...' : 'Generate Statement'}
                </button>
              </div>
            </div>

            {statementError && <Alert type="error" message="Failed to generate statement" />}

            {statement && (
              <div className="border rounded-lg p-6 mt-4">
                <div className="flex justify-between items-start mb-6">
                  <div>
                    <h2 className="text-xl font-bold">Account Statement</h2>
                    <p className="text-gray-600">Account ID: {statement.accountId}</p>
                    <p className="text-gray-600">Statement Date: {new Date(statement.statementDate).toLocaleDateString()}</p>
                  </div>
                  <button
                    onClick={() => window.print()}
                    className="btn-secondary"
                  >
                    Print
                  </button>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                  <div className="bg-gray-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Opening Balance</p>
                    <p className="text-xl font-semibold">{formatCurrency(statement.openingBalance)}</p>
                  </div>
                  <div className="bg-gray-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Total Debits</p>
                    <p className="text-xl font-semibold text-red-600">{formatCurrency(statement.totalDebits)}</p>
                  </div>
                  <div className="bg-gray-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Total Credits</p>
                    <p className="text-xl font-semibold text-green-600">{formatCurrency(statement.totalCredits)}</p>
                  </div>
                  <div className="bg-gray-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Closing Balance</p>
                    <p className="text-xl font-semibold">{formatCurrency(statement.closingBalance)}</p>
                  </div>
                </div>

                <div className="bg-yellow-50 p-4 rounded mb-6">
                  <div className="flex justify-between items-center">
                    <div>
                      <p className="font-semibold">Minimum Payment Due</p>
                      <p className="text-2xl font-bold text-yellow-700">{formatCurrency(statement.minimumPaymentDue)}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm text-gray-600">Payment Due Date</p>
                      <p className="font-semibold">{new Date(statement.paymentDueDate).toLocaleDateString()}</p>
                    </div>
                  </div>
                </div>

                <h3 className="font-semibold mb-3">Transaction History</h3>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-2 table-header">Date</th>
                        <th className="px-4 py-2 table-header">Description</th>
                        <th className="px-4 py-2 table-header">Type</th>
                        <th className="px-4 py-2 table-header text-right">Amount</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                      {statement.transactions.map((txn) => (
                        <tr key={txn.id || txn.transactionId}>
                          <td className="px-4 py-2 text-sm">
                            {new Date(txn.originTimestamp || txn.originationTimestamp || '').toLocaleDateString()}
                          </td>
                          <td className="px-4 py-2 text-sm">{txn.transactionDescription || txn.description || '-'}</td>
                          <td className="px-4 py-2 text-sm">{txn.transactionTypeCode}</td>
                          <td className={`px-4 py-2 text-sm text-right font-semibold ${
                            txn.transactionTypeCode === 'CR' ? 'text-green-600' : 'text-red-600'
                          }`}>
                            {txn.transactionTypeCode === 'CR' ? '+' : '-'}
                            {formatCurrency(txn.transactionAmount ?? txn.amount ?? 0)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'transactions' && (
          <div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
                <input
                  type="date"
                  value={dateRange.start}
                  onChange={(e) => setDateRange({ ...dateRange, start: e.target.value })}
                  className="input-field"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
                <input
                  type="date"
                  value={dateRange.end}
                  onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
                  className="input-field"
                />
              </div>
              <div className="flex items-end">
                <button
                  onClick={handleGenerateReport}
                  disabled={reportLoading}
                  className="btn-primary w-full"
                >
                  {reportLoading ? 'Loading...' : 'Generate Report'}
                </button>
              </div>
            </div>

            {reportError && <Alert type="error" message="Failed to generate report" />}

            {transactionReport && (
              <div className="border rounded-lg p-6 mt-4">
                <h2 className="text-xl font-bold mb-6">Transaction Report</h2>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                  <div className="bg-blue-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Total Transactions</p>
                    <p className="text-2xl font-bold text-blue-700">{transactionReport.totalTransactions}</p>
                  </div>
                  <div className="bg-green-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Purchases</p>
                    <p className="text-2xl font-bold text-green-700">{transactionReport.purchaseCount}</p>
                  </div>
                  <div className="bg-purple-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Payments</p>
                    <p className="text-2xl font-bold text-purple-700">{transactionReport.paymentCount}</p>
                  </div>
                  <div className="bg-orange-50 p-4 rounded">
                    <p className="text-sm text-gray-500">Refunds</p>
                    <p className="text-2xl font-bold text-orange-700">{transactionReport.refundCount}</p>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <h3 className="font-semibold mb-3">Transactions by Type</h3>
                    <div className="space-y-2">
                      {Object.entries(transactionReport.transactionsByType || {}).map(([type, count]) => (
                        <div key={type} className="flex justify-between items-center bg-gray-50 p-2 rounded">
                          <span>{type}</span>
                          <span className="font-semibold">{count as number}</span>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div>
                    <h3 className="font-semibold mb-3">Amounts by Type</h3>
                    <div className="space-y-2">
                      {Object.entries(transactionReport.amountsByType || {}).map(([type, amount]) => (
                        <div key={type} className="flex justify-between items-center bg-gray-50 p-2 rounded">
                          <span>{type}</span>
                          <span className="font-semibold">{formatCurrency(amount as number)}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>

                {transactionReport.transactionsByDay && Object.keys(transactionReport.transactionsByDay).length > 0 && (
                  <div className="mt-6">
                    <h3 className="font-semibold mb-3">Daily Transaction Volume</h3>
                    <div className="overflow-x-auto">
                      <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                          <tr>
                            <th className="px-4 py-2 table-header">Date</th>
                            <th className="px-4 py-2 table-header text-right">Count</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                          {Object.entries(transactionReport.transactionsByDay).map(([date, count]) => (
                            <tr key={date}>
                              <td className="px-4 py-2 text-sm">{date}</td>
                              <td className="px-4 py-2 text-sm text-right font-semibold">{count as number}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
