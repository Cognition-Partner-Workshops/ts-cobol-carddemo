import { useQuery } from '@tanstack/react-query';
import { 
  CreditCard, 
  Users, 
  DollarSign, 
  AlertTriangle,
  TrendingUp,
  Activity
} from 'lucide-react';
import { accountApi, cardApi, userApi } from '../services/api';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'];

export function Dashboard() {
  const { data: accountStats } = useQuery({
    queryKey: ['accountStats'],
    queryFn: accountApi.getStatistics,
  });

  const { data: cardStats } = useQuery({
    queryKey: ['cardStats'],
    queryFn: cardApi.getStatistics,
  });

  const { data: userStats } = useQuery({
    queryKey: ['userStats'],
    queryFn: userApi.getStatistics,
  });

  const accountChartData = accountStats ? [
    { name: 'Active', value: accountStats.activeAccounts },
    { name: 'Total', value: accountStats.totalAccounts - accountStats.activeAccounts },
  ] : [];

  const cardChartData = cardStats ? [
    { name: 'Active Cards', value: cardStats.activeCards },
    { name: 'Inactive', value: cardStats.totalCards - cardStats.activeCards },
    { name: 'Expiring', value: cardStats.expiringCards },
  ] : [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-500 mt-1">Welcome to CardDemo Management System</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Accounts"
          value={accountStats?.totalAccounts ?? 0}
          icon={CreditCard}
          color="blue"
        />
        <StatCard
          title="Total Balance"
          value={`$${(accountStats?.totalBalance ?? 0).toLocaleString()}`}
          icon={DollarSign}
          color="green"
        />
        <StatCard
          title="Active Cards"
          value={cardStats?.activeCards ?? 0}
          icon={Activity}
          color="purple"
        />
        <StatCard
          title="Over Limit"
          value={accountStats?.overLimitCount ?? 0}
          icon={AlertTriangle}
          color="red"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Account Status</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={accountChartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {accountChartData.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="flex justify-center gap-6 mt-4">
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-blue-500" />
              <span className="text-sm text-gray-600">Active</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-green-500" />
              <span className="text-sm text-gray-600">Inactive</span>
            </div>
          </div>
        </div>

        <div className="card">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Card Overview</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={cardChartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="value" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="card">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 bg-blue-100 rounded-lg">
              <CreditCard className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="font-semibold text-gray-900">Account Summary</h3>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Total Accounts</span>
              <span className="font-medium">{accountStats?.totalAccounts ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Active Accounts</span>
              <span className="font-medium">{accountStats?.activeAccounts ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Credit Limit</span>
              <span className="font-medium">${(accountStats?.totalCreditLimit ?? 0).toLocaleString()}</span>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 bg-green-100 rounded-lg">
              <TrendingUp className="w-5 h-5 text-green-600" />
            </div>
            <h3 className="font-semibold text-gray-900">Card Summary</h3>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Total Cards</span>
              <span className="font-medium">{cardStats?.totalCards ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Active Cards</span>
              <span className="font-medium">{cardStats?.activeCards ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Expiring Soon</span>
              <span className="font-medium text-amber-600">{cardStats?.expiringCards ?? 0}</span>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 bg-purple-100 rounded-lg">
              <Users className="w-5 h-5 text-purple-600" />
            </div>
            <h3 className="font-semibold text-gray-900">User Summary</h3>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-600">Total Users</span>
              <span className="font-medium">{userStats?.totalUsers ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Administrators</span>
              <span className="font-medium">{userStats?.totalAdmins ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Enabled Users</span>
              <span className="font-medium">{userStats?.totalEnabledUsers ?? 0}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ComponentType<{ className?: string }>;
  color: 'blue' | 'green' | 'purple' | 'red';
}

function StatCard({ title, value, icon: Icon, color }: StatCardProps) {
  const colorClasses = {
    blue: 'bg-blue-100 text-blue-600',
    green: 'bg-green-100 text-green-600',
    purple: 'bg-purple-100 text-purple-600',
    red: 'bg-red-100 text-red-600',
  };

  return (
    <div className="card">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
        </div>
        <div className={`p-3 rounded-lg ${colorClasses[color]}`}>
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </div>
  );
}
