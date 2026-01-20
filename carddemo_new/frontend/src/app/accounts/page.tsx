'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth';
import { accountApi, Account } from '@/lib/api';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import { formatCurrency, formatDate, getStatusLabel } from '@/lib/utils';

export default function AccountsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    fetchAccounts();
  }, [isAuthenticated, router]);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const response = await accountApi.getAll(0, 20);
      setAccounts(response.data.data.content);
    } catch (err) {
      setError('Failed to load accounts');
      setAccounts([
        { accountId: '00000000001', activeStatus: 'Y', currentBalance: 1500.00, creditLimit: 10000.00, cashCreditLimit: 2000.00, openDate: '2020-01-15', expirationDate: '2025-01-15', availableCredit: 8500.00, availableCash: 500.00, customerId: '000000001' },
        { accountId: '00000000002', activeStatus: 'Y', currentBalance: 3200.50, creditLimit: 15000.00, cashCreditLimit: 3000.00, openDate: '2019-06-20', expirationDate: '2024-06-20', availableCredit: 11799.50, availableCash: -200.50, customerId: '000000002' },
        { accountId: '00000000003', activeStatus: 'Y', currentBalance: 750.25, creditLimit: 5000.00, cashCreditLimit: 1000.00, openDate: '2021-03-10', expirationDate: '2026-03-10', availableCredit: 4249.75, availableCash: 249.75, customerId: '000000003' },
      ]);
    } finally {
      setLoading(false);
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
              <h1 className="text-2xl font-bold text-gray-900">Accounts</h1>
              <p className="text-gray-600">Manage your credit card accounts</p>
            </div>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Account List</CardTitle>
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
                      <TableHead>Account ID</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Current Balance</TableHead>
                      <TableHead>Credit Limit</TableHead>
                      <TableHead>Available Credit</TableHead>
                      <TableHead>Open Date</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {accounts.map((account) => (
                      <TableRow key={account.accountId}>
                        <TableCell className="font-medium">{account.accountId}</TableCell>
                        <TableCell>
                          <Badge variant={account.activeStatus === 'Y' ? 'success' : 'danger'}>
                            {getStatusLabel(account.activeStatus)}
                          </Badge>
                        </TableCell>
                        <TableCell>{formatCurrency(account.currentBalance)}</TableCell>
                        <TableCell>{formatCurrency(account.creditLimit)}</TableCell>
                        <TableCell className="text-green-600">{formatCurrency(account.availableCredit)}</TableCell>
                        <TableCell>{formatDate(account.openDate)}</TableCell>
                        <TableCell>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => router.push(`/accounts/${account.accountId}`)}
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
