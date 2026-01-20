'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuthStore } from '@/lib/auth';
import { paymentApi, Payment } from '@/lib/api';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import { formatCurrency, formatDateTime } from '@/lib/utils';

const paymentSchema = z.object({
  accountId: z.string().min(11, 'Account ID is required'),
  amount: z.string().min(1, 'Amount is required'),
  paymentSource: z.string().min(1, 'Payment source is required'),
  sourceAccount: z.string().optional(),
});

type PaymentFormData = z.infer<typeof paymentSchema>;

export default function PaymentsPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const { register, handleSubmit, reset, formState: { errors } } = useForm<PaymentFormData>({
    resolver: zodResolver(paymentSchema),
    defaultValues: {
      accountId: '00000000001',
      paymentSource: 'BANK_TRANSFER',
    },
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    fetchPayments();
  }, [isAuthenticated, router]);

  const fetchPayments = async () => {
    try {
      setLoading(true);
      const response = await paymentApi.getByAccount('00000000001', 0, 20);
      setPayments(response.data.data.content);
    } catch (err) {
      setPayments([
        { paymentId: '2024010100000001', accountId: '00000000001', amount: 500.00, paymentSource: 'BANK_TRANSFER', sourceAccount: '****1234', confirmationNumber: 'CONF123456789012', status: 'PROCESSED', scheduledDate: '2024-01-01T10:00:00', processedDate: '2024-01-01T10:05:00' },
        { paymentId: '2024010500000002', accountId: '00000000001', amount: 250.00, paymentSource: 'DEBIT_CARD', sourceAccount: '****5678', confirmationNumber: 'CONF987654321098', status: 'PENDING', scheduledDate: '2024-01-10T00:00:00', processedDate: '' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = async (data: PaymentFormData) => {
    setSubmitting(true);
    setMessage(null);
    try {
      const response = await paymentApi.create({
        accountId: data.accountId,
        amount: parseFloat(data.amount),
        paymentSource: data.paymentSource,
        sourceAccount: data.sourceAccount,
      });
      setMessage({ type: 'success', text: `Payment submitted successfully. Confirmation: ${response.data.data.confirmationNumber}` });
      reset();
      setShowForm(false);
      fetchPayments();
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to submit payment. Please try again.' });
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'PROCESSED': return 'success';
      case 'PENDING': return 'warning';
      case 'CANCELLED': return 'danger';
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
              <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
              <p className="text-gray-600">Make and view bill payments</p>
            </div>
            <Button onClick={() => setShowForm(!showForm)}>
              {showForm ? 'Cancel' : 'Make Payment'}
            </Button>
          </div>

          {message && (
            <div className={`mb-4 p-4 rounded-md ${message.type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
              {message.text}
            </div>
          )}

          {showForm && (
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>Make a Payment</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Input
                      id="accountId"
                      label="Account ID"
                      error={errors.accountId?.message}
                      {...register('accountId')}
                    />
                    <Input
                      id="amount"
                      label="Amount"
                      type="number"
                      step="0.01"
                      placeholder="0.00"
                      error={errors.amount?.message}
                      {...register('amount')}
                    />
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Payment Source</label>
                      <select
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                        {...register('paymentSource')}
                      >
                        <option value="BANK_TRANSFER">Bank Transfer</option>
                        <option value="DEBIT_CARD">Debit Card</option>
                        <option value="CHECK">Check</option>
                      </select>
                    </div>
                    <Input
                      id="sourceAccount"
                      label="Source Account (optional)"
                      placeholder="Account number"
                      {...register('sourceAccount')}
                    />
                  </div>
                  <div className="flex justify-end">
                    <Button type="submit" isLoading={submitting}>
                      Submit Payment
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Payment History</CardTitle>
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
                      <TableHead>Account</TableHead>
                      <TableHead>Amount</TableHead>
                      <TableHead>Source</TableHead>
                      <TableHead>Confirmation</TableHead>
                      <TableHead>Status</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {payments.map((payment) => (
                      <TableRow key={payment.paymentId}>
                        <TableCell>{formatDateTime(payment.scheduledDate)}</TableCell>
                        <TableCell>{payment.accountId}</TableCell>
                        <TableCell className="text-green-600 font-medium">
                          {formatCurrency(payment.amount)}
                        </TableCell>
                        <TableCell>{payment.paymentSource}</TableCell>
                        <TableCell className="font-mono text-sm">{payment.confirmationNumber}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusVariant(payment.status)}>{payment.status}</Badge>
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
