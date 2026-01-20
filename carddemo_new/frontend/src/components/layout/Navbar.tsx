'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuthStore, isAdmin } from '@/lib/auth';
import Button from '@/components/ui/Button';

export default function Navbar() {
  const router = useRouter();
  const { user, isAuthenticated, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  if (!isAuthenticated) return null;

  return (
    <nav className="bg-primary-800 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <Link href="/dashboard" className="text-xl font-bold">
              CardDemo
            </Link>
            <div className="hidden md:block ml-10">
              <div className="flex items-baseline space-x-4">
                <Link href="/dashboard" className="px-3 py-2 rounded-md text-sm font-medium hover:bg-primary-700">
                  Dashboard
                </Link>
                <Link href="/accounts" className="px-3 py-2 rounded-md text-sm font-medium hover:bg-primary-700">
                  Accounts
                </Link>
                <Link href="/cards" className="px-3 py-2 rounded-md text-sm font-medium hover:bg-primary-700">
                  Cards
                </Link>
                <Link href="/transactions" className="px-3 py-2 rounded-md text-sm font-medium hover:bg-primary-700">
                  Transactions
                </Link>
                <Link href="/payments" className="px-3 py-2 rounded-md text-sm font-medium hover:bg-primary-700">
                  Payments
                </Link>
                <Link href="/reports" className="px-3 py-2 rounded-md text-sm font-medium hover:bg-primary-700">
                  Reports
                </Link>
                {isAdmin(user) && (
                  <Link href="/admin" className="px-3 py-2 rounded-md text-sm font-medium hover:bg-primary-700">
                    Admin
                  </Link>
                )}
              </div>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <span className="text-sm">
              Welcome, {user?.firstName} {user?.lastName}
            </span>
            <Button variant="secondary" size="sm" onClick={handleLogout}>
              Logout
            </Button>
          </div>
        </div>
      </div>
    </nav>
  );
}
