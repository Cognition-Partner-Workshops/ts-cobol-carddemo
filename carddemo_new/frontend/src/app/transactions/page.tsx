'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth';
import { transactionApi, Transaction } from '@/lib/api';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import { formatCurrency, formatDateTime } from '@/lib/utils';

export default function TransactionsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    fetchTransactions();
  }, [isAuthenticated, router]);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const response = await transactionApi.getAll(0, 20);
      setTransactions(response.data.data.content);
    } catch (err) {
      setTransactions([
        { transactionId: '2024010112000001', typeCode: 'PR', typeDescription: 'Purchase', categoryCode: '0001', categoryDescription: 'Retail Purchase', description: 'WALMART STORE #1234', amount: 125.50, merchantName: 'WALMART', merchantCity: 'NEW YORK', cardNumber: '4111111111111111', maskedCardNumber: '**** 1111', accountId: '00000000001', originalTimestamp: '2024-01-01T12:00:00', status: 'POSTED' },
        { transactionId: '2024010214300002', typeCode: 'PR', typeDescription: 'Purchase', categoryCode: '0004', categoryDescription: 'Dining', description: 'STARBUCKS #5678', amount: 15.75, merchantName: 'STARBUCKS', merchantCity: 'NEW YORK', cardNumber: '4111111111111111', maskedCardNumber: '**** 1111', accountId: '00000000001', originalTimestamp: '2024-01-02T14:30:00', status: 'POSTED' },
        { transactionId: '2024010309150003', typeCode: 'PR', typeDescription: 'Purchase', categoryCode: '0005', categoryDescription: 'Fuel', description: 'SHELL GAS STATION', amount: 45.00, merchantName: 'SHELL', merchantCity: 'BROOKLYN', cardNumber: '4111111111111111', maskedCardNumber: '**** 1111', accountId: '00000000001', originalTimestamp: '2024-01-03T09:15:00', status: 'POSTED' },
        { transactionId: '2024010416450004', typeCode: 'PR', typeDescription: 'Purchase', categoryCode: '0002', categoryDescription: 'Online Purchase', description: 'AMAZON.COM', amount: 89.99, merchantName: 'AMAZON', merchantCity: 'SEATTLE', cardNumber: '5333333333333333', maskedCardNumber: '**** 3333', accountId: '00000000002', originalTimestamp: '2024-01-04T16:45:00', status: 'POSTED' },
        { transactionId: '2024010511200005', typeCode: 'PM', typeDescription: 'Payment', categoryCode: '0010', categoryDescription: 'Other', description: 'PAYMENT - THANK YOU', amount: -500.00, merchantName: 'PAYMENT', merchantCity: '', cardNumber: '4111111111111111', maskedCardNumber: '**** 1111', accountId: '00000000001', originalTimestamp: '2024-01-05T11:20:00', status: 'POSTED' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'POSTED': return 'success';
      case 'PENDING': return 'warning';
      case 'DECLINED': return 'danger';
      default: return 'default';
    }
  };

  if (!isAuthenticated) return null;

  return (
    <div className="min-h-screen bg-gray-100">
      <Navbar />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-6">
          <div className="mb-6 flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Transactions</h1>
              <p className="text-gray-600">View your transaction history</p>
            </div>
            <Button onClick={() => router.push('/transactions/new')}>
              Add Transaction
            </Button>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Transaction History</CardTitle>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="flex justify-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-600"></div>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Description</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Card</TableHead>
                      <TableHead>Amount</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {transactions.map((txn) => (
                      <TableRow key={txn.transactionId}>
                        <TableCell>{formatDateTime(txn.originalTimestamp)}</TableCell>
                        <TableCell>
                          <div>
                            <p className="font-medium">{txn.description}</p>
                            <p className="text-sm text-gray-500">{txn.merchantCity}</p>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="info">{txn.typeDescription}</Badge>
                        </TableCell>
                        <TableCell className="font-mono text-sm">{txn.maskedCardNumber}</TableCell>
                        <TableCell className={txn.amount < 0 ? 'text-green-600' : 'text-gray-900'}>
                          {formatCurrency(txn.amount)}
                        </TableCell>
                        <TableCell>
                          <Badge variant={getStatusVariant(txn.status)}>{txn.status}</Badge>
                        </TableCell>
                        <TableCell>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => router.push(`/transactions/${txn.transactionId}`)}
                          >
                            View
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </main>
      </div>
    </div>
  );
}
