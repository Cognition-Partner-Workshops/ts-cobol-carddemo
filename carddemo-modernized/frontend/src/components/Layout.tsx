import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  CreditCard, 
  Users, 
  Wallet, 
  Receipt, 
  Home,
  LogOut,
  Settings
} from 'lucide-react';

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { path: '/dashboard', icon: <Home className="w-5 h-5" />, label: 'Dashboard' },
    { path: '/accounts', icon: <Wallet className="w-5 h-5" />, label: 'Accounts' },
    { path: '/cards', icon: <CreditCard className="w-5 h-5" />, label: 'Cards' },
    { path: '/transactions', icon: <Receipt className="w-5 h-5" />, label: 'Transactions' },
    { path: '/customers', icon: <Users className="w-5 h-5" />, label: 'Customers' },
    ...(user?.userType === 'ADMIN' ? [{ path: '/users', icon: <Settings className="w-5 h-5" />, label: 'Users' }] : []),
  ];

  return (
    <div className="min-h-screen bg-gray-100">
      <nav className="bg-blue-600 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-8">
              <Link to="/dashboard" className="flex items-center space-x-2">
                <CreditCard className="w-8 h-8" />
                <span className="font-bold text-lg">CardDemo</span>
              </Link>
              <div className="hidden md:flex space-x-1">
                {navItems.map((item) => (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`px-3 py-2 rounded-md text-sm font-medium flex items-center space-x-1 transition-colors ${
                      location.pathname.startsWith(item.path)
                        ? 'bg-blue-700 text-white'
                        : 'text-blue-100 hover:bg-blue-500'
                    }`}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </Link>
                ))}
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-medium">{user?.firstName} {user?.lastName}</p>
                <p className="text-xs text-blue-200">{user?.userType}</p>
              </div>
              <button
                onClick={handleLogout}
                className="p-2 hover:bg-blue-700 rounded-lg transition-colors"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
  );
}
