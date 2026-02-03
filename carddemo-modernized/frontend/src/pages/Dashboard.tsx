import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  CreditCard, 
  Users, 
  Wallet, 
  Receipt, 
  DollarSign, 
  Settings,
  LogOut
} from 'lucide-react';

interface MenuOption {
  id: number;
  name: string;
  icon: React.ReactNode;
  path: string;
  adminOnly?: boolean;
}

const menuOptions: MenuOption[] = [
  { id: 1, name: 'Account View', icon: <Wallet className="w-6 h-6" />, path: '/accounts' },
  { id: 2, name: 'Credit Card List', icon: <CreditCard className="w-6 h-6" />, path: '/cards' },
  { id: 3, name: 'Transaction List', icon: <Receipt className="w-6 h-6" />, path: '/transactions' },
  { id: 4, name: 'Add Transaction', icon: <DollarSign className="w-6 h-6" />, path: '/transactions/add' },
  { id: 5, name: 'Bill Payment', icon: <DollarSign className="w-6 h-6" />, path: '/bill-payment' },
  { id: 6, name: 'Customer Management', icon: <Users className="w-6 h-6" />, path: '/customers' },
  { id: 7, name: 'User Management', icon: <Settings className="w-6 h-6" />, path: '/users', adminOnly: true },
];

export default function Dashboard() {
  const { user, logout } = useAuth();

  const filteredOptions = menuOptions.filter(
    (option) => !option.adminOnly || user?.userType === 'ADMIN'
  );

  return (
    <div className="min-h-screen bg-gray-100">
      <header className="bg-blue-600 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <CreditCard className="w-8 h-8" />
              <div>
                <h1 className="text-xl font-bold">CardDemo</h1>
                <p className="text-blue-200 text-sm">Credit Card Management System</p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <div className="text-right">
                <p className="font-medium">{user?.firstName} {user?.lastName}</p>
                <p className="text-blue-200 text-sm">{user?.userType}</p>
              </div>
              <button
                onClick={logout}
                className="p-2 hover:bg-blue-700 rounded-lg transition-colors"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-gray-900">Main Menu</h2>
          <p className="text-gray-600 mt-1">Select an option to continue</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredOptions.map((option) => (
            <Link
              key={option.id}
              to={option.path}
              className="bg-white rounded-xl shadow-md hover:shadow-lg transition-shadow p-6 flex items-center space-x-4 group"
            >
              <div className="flex-shrink-0 w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center text-blue-600 group-hover:bg-blue-600 group-hover:text-white transition-colors">
                {option.icon}
              </div>
              <div>
                <h3 className="text-lg font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
                  {option.name}
                </h3>
                <p className="text-gray-500 text-sm">Option {option.id}</p>
              </div>
            </Link>
          ))}
        </div>
      </main>
    </div>
  );
}
