'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import { formatCurrency, formatDate } from '@/lib/utils';
import { FileText, TrendingUp, Calendar } from 'lucide-react';

export default function ReportsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [selectedReport, setSelectedReport] = useState<string | null>(null);
  const [accountId, setAccountId] = useState('00000000001');
  const [startDate, setStartDate] = useState('2024-01-01');
  const [endDate, setEndDate] = useState('2024-01-31');
  const [month, setMonth] = useState('1');
  const [year, setYear] = useState('2024');
  const [reportData, setReportData] = useState<Record<string, unknown> | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  const generateTransactionSummary = () => {
    setLoading(true);
    setTimeout(() => {
      setReportData({
        type: 'transaction-summary',
        accountId,
        startDate,
        endDate,
        totalTransactions: 35,
        totalPurchases: 2750.00,
        totalPayments: 2000.00,
        totalCashAdvances: 200.00,
        totalFees: 25.00,
        totalInterest: 45.50,
        netChange: 1020.50,
        categories: [
          { name: 'Retail Purchase', count: 15, amount: 1250.00, percentage: 45.5 },
          { name: 'Dining', count: 8, amount: 450.00, percentage: 16.4 },
          { name: 'Fuel', count: 6, amount: 320.00, percentage: 11.6 },
          { name: 'Online Purchase', count: 4, amount: 480.00, percentage: 17.5 },
          { name: 'Other', count: 2, amount: 250.00, percentage: 9.0 },
        ],
      });
      setLoading(false);
    }, 1000);
  };

  const generateAccountStatement = () => {
    setLoading(true);
    setTimeout(() => {
      setReportData({
        type: 'account-statement',
        accountId,
        customerName: 'John M Smith',
        statementDate: `${year}-${month.padStart(2, '0')}-28`,
        periodStart: `${year}-${month.padStart(2, '0')}-01`,
        periodEnd: `${year}-${month.padStart(2, '0')}-28`,
        previousBalance: 1500.00,
        totalPurchases: 750.00,
        totalPayments: 500.00,
        totalFees: 0.00,
        totalInterest: 22.50,
        newBalance: 1772.50,
        minimumPaymentDue: 35.45,
        paymentDueDate: `${year}-${(parseInt(month) + 1).toString().padStart(2, '0')}-22`,
        creditLimit: 10000.00,
        availableCredit: 8227.50,
      });
      setLoading(false);
    }, 1000);
  };

  if (!isAuthenticated) return null;

  return (
    <div className="min-h-screen bg-gray-100">
      <Navbar />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-6">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Reports</h1>
            <p className="text-gray-600">Generate account reports and statements</p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <Card className={`cursor-pointer transition-all ${selectedReport === 'transaction-summary' ? 'ring-2 ring-primary-500' : ''}`}
                  onClick={() => setSelectedReport('transaction-summary')}>
              <CardContent>
                <div className="flex items-center">
                  <div className="p-3 bg-purple-100 rounded-full mr-4">
                    <TrendingUp className="h-6 w-6 text-purple-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Transaction Summary</h3>
                    <p className="text-sm text-gray-500">View spending breakdown by category</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className={`cursor-pointer transition-all ${selectedReport === 'account-statement' ? 'ring-2 ring-primary-500' : ''}`}
                  onClick={() => setSelectedReport('account-statement')}>
              <CardContent>
                <div className="flex items-center">
                  <div className="p-3 bg-blue-100 rounded-full mr-4">
                    <FileText className="h-6 w-6 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">Account Statement</h3>
                    <p className="text-sm text-gray-500">Generate monthly account statement</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {selectedReport === 'transaction-summary' && (
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>Transaction Summary Report</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
                  <Input label="Account ID" value={accountId} onChange={(e) => setAccountId(e.target.value)} />
                  <Input label="Start Date" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                  <Input label="End Date" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                  <div className="flex items-end">
                    <Button onClick={generateTransactionSummary} isLoading={loading} className="w-full">
                      Generate Report
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {selectedReport === 'account-statement' && (
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>Account Statement</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
                  <Input label="Account ID" value={accountId} onChange={(e) => setAccountId(e.target.value)} />
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Month</label>
                    <select className="w-full px-3 py-2 border border-gray-300 rounded-md" value={month} onChange={(e) => setMonth(e.target.value)}>
                      {[...Array(12)].map((_, i) => (
                        <option key={i + 1} value={i + 1}>{new Date(2024, i).toLocaleString('default', { month: 'long' })}</option>
                      ))}
                    </select>
                  </div>
                  <Input label="Year" type="number" value={year} onChange={(e) => setYear(e.target.value)} />
                  <div className="flex items-end">
                    <Button onClick={generateAccountStatement} isLoading={loading} className="w-full">
                      Generate Statement
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {reportData && reportData.type === 'transaction-summary' && (
            <Card>
              <CardHeader>
                <CardTitle>Transaction Summary - {reportData.accountId as string}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-500">Total Transactions</p>
                    <p className="text-2xl font-bold">{reportData.totalTransactions as number}</p>
                  </div>
                  <div className="p-4 bg-red-50 rounded-lg">
                    <p className="text-sm text-gray-500">Total Purchases</p>
                    <p className="text-2xl font-bold text-red-600">{formatCurrency(reportData.totalPurchases as number)}</p>
                  </div>
                  <div className="p-4 bg-green-50 rounded-lg">
                    <p className="text-sm text-gray-500">Total Payments</p>
                    <p className="text-2xl font-bold text-green-600">{formatCurrency(reportData.totalPayments as number)}</p>
                  </div>
                  <div className="p-4 bg-blue-50 rounded-lg">
                    <p className="text-sm text-gray-500">Net Change</p>
                    <p className="text-2xl font-bold text-blue-600">{formatCurrency(reportData.netChange as number)}</p>
                  </div>
                </div>
                <h4 className="font-semibold mb-3">Spending by Category</h4>
                <div className="space-y-3">
                  {(reportData.categories as Array<{ name: string; count: number; amount: number; percentage: number }>).map((cat) => (
                    <div key={cat.name} className="flex items-center">
                      <div className="w-32 text-sm text-gray-600">{cat.name}</div>
                      <div className="flex-1 mx-4">
                        <div className="h-4 bg-gray-200 rounded-full overflow-hidden">
                          <div className="h-full bg-primary-500 rounded-full" style={{ width: `${cat.percentage}%` }}></div>
                        </div>
                      </div>
                      <div className="w-24 text-right text-sm font-medium">{formatCurrency(cat.amount)}</div>
                      <div className="w-16 text-right text-sm text-gray-500">{cat.percentage}%</div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {reportData && reportData.type === 'account-statement' && (
            <Card>
              <CardHeader>
                <CardTitle>Account Statement</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="border-b pb-4 mb-4">
                  <p className="text-lg font-semibold">{reportData.customerName as string}</p>
                  <p className="text-sm text-gray-500">Account: {reportData.accountId as string}</p>
                  <p className="text-sm text-gray-500">Statement Period: {formatDate(reportData.periodStart as string)} - {formatDate(reportData.periodEnd as string)}</p>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                  <div className="p-4 bg-gray-50 rounded-lg">
                    <p className="text-sm text-gray-500">Previous Balance</p>
                    <p className="text-xl font-bold">{formatCurrency(reportData.previousBalance as number)}</p>
                  </div>
                  <div className="p-4 bg-red-50 rounded-lg">
                    <p className="text-sm text-gray-500">Purchases</p>
                    <p className="text-xl font-bold text-red-600">+{formatCurrency(reportData.totalPurchases as number)}</p>
                  </div>
                  <div className="p-4 bg-green-50 rounded-lg">
                    <p className="text-sm text-gray-500">Payments</p>
                    <p className="text-xl font-bold text-green-600">-{formatCurrency(reportData.totalPayments as number)}</p>
                  </div>
                  <div className="p-4 bg-blue-50 rounded-lg">
                    <p className="text-sm text-gray-500">New Balance</p>
                    <p className="text-xl font-bold text-blue-600">{formatCurrency(reportData.newBalance as number)}</p>
                  </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-yellow-50 rounded-lg">
                  <div>
                    <p className="text-sm text-gray-500">Minimum Payment Due</p>
                    <p className="text-lg font-bold text-yellow-700">{formatCurrency(reportData.minimumPaymentDue as number)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">Payment Due Date</p>
                    <p className="text-lg font-bold">{formatDate(reportData.paymentDueDate as string)}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500">Available Credit</p>
                    <p className="text-lg font-bold text-green-600">{formatCurrency(reportData.availableCredit as number)}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </main>
      </div>
    </div>
  );
}
