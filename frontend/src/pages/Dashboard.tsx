import { useQuery } from '@tanstack/react-query';
import { reportService } from '../services/reportService';
import LoadingSpinner from '../components/common/LoadingSpinner';

export default function Dashboard() {
  const { data: summary, isLoading, error } = useQuery({
    queryKey: ['dashboard'],
    queryFn: reportService.getDashboard,
  });

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center text-red-600 p-8">
        Failed to load dashboard data. Please try again.
      </div>
    );
  }

  const stats = [
    { label: 'Total Customers', value: summary?.totalCustomers || 0, color: 'bg-blue-500' },
    { label: 'Total Accounts', value: summary?.totalAccounts || 0, color: 'bg-green-500' },
    { label: 'Active Accounts', value: summary?.activeAccounts || 0, color: 'bg-teal-500' },
    { label: 'Total Cards', value: summary?.totalCards || 0, color: 'bg-purple-500' },
    { label: 'Active Cards', value: summary?.activeCards || 0, color: 'bg-indigo-500' },
    { label: 'Transactions Today', value: summary?.totalTransactionsToday || 0, color: 'bg-orange-500' },
  ];

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        {stats.map((stat) => (
          <div key={stat.label} className="card">
            <div className="flex items-center">
              <div className={`w-12 h-12 ${stat.color} rounded-lg flex items-center justify-center text-white text-xl font-bold`}>
                {stat.value}
              </div>
              <div className="ml-4">
                <p className="text-gray-500 text-sm">{stat.label}</p>
                <p className="text-2xl font-semibold">{stat.value.toLocaleString()}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Financial Summary</h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center border-b pb-2">
              <span className="text-gray-600">Total Balance</span>
              <span className="font-semibold text-lg">
                {formatCurrency(summary?.totalBalance || 0)}
              </span>
            </div>
            <div className="flex justify-between items-center border-b pb-2">
              <span className="text-gray-600">Total Credit Limit</span>
              <span className="font-semibold text-lg">
                {formatCurrency(summary?.totalCreditLimit || 0)}
              </span>
            </div>
            <div className="flex justify-between items-center border-b pb-2">
              <span className="text-gray-600">Utilization Rate</span>
              <span className={`font-semibold text-lg ${
                (summary?.utilizationRate || 0) > 80 ? 'text-red-600' : 'text-green-600'
              }`}>
                {((summary?.utilizationRate || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Transactions This Month</span>
              <span className="font-semibold text-lg">
                {(summary?.totalTransactionsThisMonth || 0).toLocaleString()}
              </span>
            </div>
          </div>
        </div>

        <div className="card">
          <h2 className="text-lg font-semibold mb-4">Alerts</h2>
          <div className="space-y-3">
            {(summary?.overLimitAccounts || 0) > 0 && (
              <div className="flex items-center p-3 bg-red-50 rounded-lg">
                <span className="text-red-500 mr-3">⚠️</span>
                <span className="text-red-700">
                  {summary?.overLimitAccounts} accounts are over their credit limit
                </span>
              </div>
            )}
            {(summary?.expiringCardsThisMonth || 0) > 0 && (
              <div className="flex items-center p-3 bg-yellow-50 rounded-lg">
                <span className="text-yellow-500 mr-3">⏰</span>
                <span className="text-yellow-700">
                  {summary?.expiringCardsThisMonth} cards expiring this month
                </span>
              </div>
            )}
            {(summary?.overLimitAccounts || 0) === 0 && (summary?.expiringCardsThisMonth || 0) === 0 && (
              <div className="flex items-center p-3 bg-green-50 rounded-lg">
                <span className="text-green-500 mr-3">✓</span>
                <span className="text-green-700">No alerts at this time</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
