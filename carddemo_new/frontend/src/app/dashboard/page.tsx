'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore, isAdmin } from '@/lib/auth';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { formatCurrency } from '@/lib/utils';
import { CreditCard, Wallet, ArrowLeftRight, Receipt, Users, TrendingUp } from 'lucide-react';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';

interface DashboardStats {
  totalAccounts: number;
  totalCards: number;
  totalTransactions: number;
  pendingPayments: number;
  totalBalance: number;
  availableCredit: number;
}

export default function DashboardPage() {
  const router = useRouter();
  const { user, isAuthenticated } = useAuthStore();
  const [stats, setStats] = useState<DashboardStats>({
    totalAccounts: 4,
    totalCards: 5,
    totalTransactions: 156,
    pendingPayments: 2,
    totalBalance: 5450.75,
    availableCredit: 32549.25,
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  if (!isAuthenticated) {
    return null;
  }

  const statCards = [
    { title: 'Total Accounts', value: stats.totalAccounts, icon: Wallet, color: 'bg-blue-500' },
    { title: 'Active Cards', value: stats.totalCards, icon: CreditCard, color: 'bg-green-500' },
    { title: 'Transactions', value: stats.totalTransactions, icon: ArrowLeftRight, color: 'bg-purple-500' },
    { title: 'Pending Payments', value: stats.pendingPayments, icon: Receipt, color: 'bg-yellow-500' },
  ];

  return (
    <div className="min-h-screen bg-gray-100">
      <Navbar />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-6">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-gray-600">Welcome back, {user?.firstName}!</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
            {statCards.map((stat) => {
              const Icon = stat.icon;
              return (
                <Card key={stat.title}>
                  <CardContent>
                    <div className="flex items-center">
                      <div className={`p-3 rounded-full ${stat.color} text-white mr-4`}>
                        <Icon className="h-6 w-6" />
                      </div>
                      <div>
                        <p className="text-sm text-gray-500">{stat.title}</p>
                        <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Account Summary</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex justify-between items-center p-4 bg-gray-50 rounded-lg">
                    <div>
                      <p className="text-sm text-gray-500">Total Balance</p>
                      <p className="text-xl font-bold text-gray-900">{formatCurrency(stats.totalBalance)}</p>
                    </div>
                    <TrendingUp className="h-8 w-8 text-red-500" />
                  </div>
                  <div className="flex justify-between items-center p-4 bg-gray-50 rounded-lg">
                    <div>
                      <p className="text-sm text-gray-500">Available Credit</p>
                      <p className="text-xl font-bold text-green-600">{formatCurrency(stats.availableCredit)}</p>
                    </div>
                    <CreditCard className="h-8 w-8 text-green-500" />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Quick Actions</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 gap-4">
                  <button
                    onClick={() => router.push('/accounts')}
                    className="p-4 bg-primary-50 rounded-lg hover:bg-primary-100 transition-colors text-left"
                  >
                    <Wallet className="h-6 w-6 text-primary-600 mb-2" />
                    <p className="font-medium text-gray-900">View Accounts</p>
                    <p className="text-sm text-gray-500">Manage your accounts</p>
                  </button>
                  <button
                    onClick={() => router.push('/cards')}
                    className="p-4 bg-green-50 rounded-lg hover:bg-green-100 transition-colors text-left"
                  >
                    <CreditCard className="h-6 w-6 text-green-600 mb-2" />
                    <p className="font-medium text-gray-900">View Cards</p>
                    <p className="text-sm text-gray-500">Manage your cards</p>
                  </button>
                  <button
                    onClick={() => router.push('/transactions')}
                    className="p-4 bg-purple-50 rounded-lg hover:bg-purple-100 transition-colors text-left"
                  >
                    <ArrowLeftRight className="h-6 w-6 text-purple-600 mb-2" />
                    <p className="font-medium text-gray-900">Transactions</p>
                    <p className="text-sm text-gray-500">View transaction history</p>
                  </button>
                  <button
                    onClick={() => router.push('/payments')}
                    className="p-4 bg-yellow-50 rounded-lg hover:bg-yellow-100 transition-colors text-left"
                  >
                    <Receipt className="h-6 w-6 text-yellow-600 mb-2" />
                    <p className="font-medium text-gray-900">Make Payment</p>
                    <p className="text-sm text-gray-500">Pay your bill</p>
                  </button>
                </div>
              </CardContent>
            </Card>
          </div>

          {isAdmin(user) && (
            <Card className="mt-6">
              <CardHeader>
                <CardTitle>Administration</CardTitle>
              </CardHeader>
              <CardContent>
                <button
                  onClick={() => router.push('/admin')}
                  className="flex items-center p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                >
                  <Users className="h-6 w-6 text-gray-600 mr-4" />
                  <div>
                    <p className="font-medium text-gray-900">User Management</p>
                    <p className="text-sm text-gray-500">Manage system users and permissions</p>
                  </div>
                </button>
              </CardContent>
            </Card>
          )}
        </main>
      </div>
    </div>
  );
}
