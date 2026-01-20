'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuthStore, isAdmin } from '@/lib/auth';
import { cn } from '@/lib/utils';
import {
  LayoutDashboard,
  CreditCard,
  Wallet,
  ArrowLeftRight,
  Receipt,
  FileText,
  Users,
  Settings,
  Clock,
  Shield,
  Tag,
  Network,
} from 'lucide-react';

const menuItems = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/accounts', label: 'Accounts', icon: Wallet },
  { href: '/cards', label: 'Cards', icon: CreditCard },
  { href: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
  { href: '/payments', label: 'Payments', icon: Receipt },
  { href: '/reports', label: 'Reports', icon: FileText },
];

const adminItems = [
  { href: '/admin', label: 'User Management', icon: Users },
  { href: '/batch', label: 'Batch Processing', icon: Clock },
  { href: '/authorization', label: 'Authorization', icon: Shield },
  { href: '/transaction-types', label: 'Transaction Types', icon: Tag },
  { href: '/integration', label: 'System Integration', icon: Network },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated) return null;

  return (
    <aside className="w-64 bg-white shadow-md min-h-screen">
      <div className="p-4">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">Menu</h2>
        <nav className="space-y-1">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'flex items-center px-4 py-2 text-sm font-medium rounded-md transition-colors',
                  isActive
                    ? 'bg-primary-100 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                )}
              >
                <Icon className="mr-3 h-5 w-5" />
                {item.label}
              </Link>
            );
          })}
          
          {isAdmin(user) && (
            <>
              <hr className="my-4 border-gray-200" />
              <p className="px-4 text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                Administration
              </p>
              {adminItems.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      'flex items-center px-4 py-2 text-sm font-medium rounded-md transition-colors',
                      isActive
                        ? 'bg-primary-100 text-primary-700'
                        : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                    )}
                  >
                    <Icon className="mr-3 h-5 w-5" />
                    {item.label}
                  </Link>
                );
              })}
            </>
          )}
        </nav>
      </div>
    </aside>
  );
}
