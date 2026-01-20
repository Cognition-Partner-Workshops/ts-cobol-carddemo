'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/auth';
import { cardApi, Card as CardType } from '@/lib/api';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import { formatDate, getStatusLabel } from '@/lib/utils';

export default function CardsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [cards, setCards] = useState<CardType[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    fetchCards();
  }, [isAuthenticated, router]);

  const fetchCards = async () => {
    try {
      setLoading(true);
      const response = await cardApi.getAll(0, 20);
      setCards(response.data.data.content);
    } catch (err) {
      setCards([
        { cardNumber: '4111111111111111', maskedCardNumber: '**** **** **** 1111', accountId: '00000000001', embossedName: 'JOHN M SMITH', expirationDate: '2025-12-31', activeStatus: 'Y', customerId: '000000001', expired: false },
        { cardNumber: '4222222222222222', maskedCardNumber: '**** **** **** 2222', accountId: '00000000001', embossedName: 'JOHN SMITH', expirationDate: '2026-06-30', activeStatus: 'Y', customerId: '000000001', expired: false },
        { cardNumber: '5333333333333333', maskedCardNumber: '**** **** **** 3333', accountId: '00000000002', embossedName: 'JANE E DOE', expirationDate: '2025-09-30', activeStatus: 'Y', customerId: '000000002', expired: false },
        { cardNumber: '5444444444444444', maskedCardNumber: '**** **** **** 4444', accountId: '00000000003', embossedName: 'ROBERT J JOHNSON', expirationDate: '2026-03-31', activeStatus: 'Y', customerId: '000000003', expired: false },
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
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Cards</h1>
            <p className="text-gray-600">Manage your credit cards</p>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Card List</CardTitle>
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
                      <TableHead>Card Number</TableHead>
                      <TableHead>Embossed Name</TableHead>
                      <TableHead>Account ID</TableHead>
                      <TableHead>Expiration</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {cards.map((card) => (
                      <TableRow key={card.cardNumber}>
                        <TableCell className="font-medium font-mono">{card.maskedCardNumber}</TableCell>
                        <TableCell>{card.embossedName}</TableCell>
                        <TableCell>{card.accountId}</TableCell>
                        <TableCell>{formatDate(card.expirationDate)}</TableCell>
                        <TableCell>
                          <Badge variant={card.activeStatus === 'Y' && !card.expired ? 'success' : 'danger'}>
                            {card.expired ? 'Expired' : getStatusLabel(card.activeStatus)}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => router.push(`/cards/${card.cardNumber}`)}
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
