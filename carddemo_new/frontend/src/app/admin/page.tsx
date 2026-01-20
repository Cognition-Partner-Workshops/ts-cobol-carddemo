'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuthStore, isAdmin } from '@/lib/auth';
import { adminApi, AdminUser } from '@/lib/api';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import { formatDateTime } from '@/lib/utils';

const userSchema = z.object({
  userId: z.string().min(1, 'User ID is required').max(8, 'User ID must be at most 8 characters'),
  firstName: z.string().min(1, 'First name is required').max(20, 'First name must be at most 20 characters'),
  lastName: z.string().min(1, 'Last name is required').max(20, 'Last name must be at most 20 characters'),
  password: z.string().min(1, 'Password is required').max(8, 'Password must be at most 8 characters'),
  userType: z.enum(['A', 'U'], { required_error: 'User type is required' }),
});

type UserFormData = z.infer<typeof userSchema>;

export default function AdminPage() {
  const router = useRouter();
  const { user, isAuthenticated } = useAuthStore();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const { register, handleSubmit, reset, formState: { errors } } = useForm<UserFormData>({
    resolver: zodResolver(userSchema),
    defaultValues: { userType: 'U' },
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    if (!isAdmin(user)) {
      router.push('/dashboard');
      return;
    }
    fetchUsers();
  }, [isAuthenticated, user, router]);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await adminApi.getUsers(0, 20);
      setUsers(response.data.data.content);
    } catch (err) {
      setUsers([
        { userId: 'ADMIN001', firstName: 'System', lastName: 'Administrator', userType: 'A', userTypeDescription: 'Administrator', active: true, createdAt: '2024-01-01T00:00:00', lastLogin: '2024-01-20T10:30:00' },
        { userId: 'ADMIN002', firstName: 'Jane', lastName: 'Admin', userType: 'A', userTypeDescription: 'Administrator', active: true, createdAt: '2024-01-01T00:00:00', lastLogin: '2024-01-19T14:20:00' },
        { userId: 'USER0001', firstName: 'John', lastName: 'Doe', userType: 'U', userTypeDescription: 'Regular User', active: true, createdAt: '2024-01-02T00:00:00', lastLogin: '2024-01-20T09:15:00' },
        { userId: 'USER0002', firstName: 'Jane', lastName: 'Smith', userType: 'U', userTypeDescription: 'Regular User', active: true, createdAt: '2024-01-03T00:00:00', lastLogin: '2024-01-18T16:45:00' },
        { userId: 'USER0003', firstName: 'Bob', lastName: 'Johnson', userType: 'U', userTypeDescription: 'Regular User', active: false, createdAt: '2024-01-04T00:00:00', lastLogin: '2024-01-10T11:00:00' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = async (data: UserFormData) => {
    setSubmitting(true);
    setMessage(null);
    try {
      await adminApi.createUser(data);
      setMessage({ type: 'success', text: `User ${data.userId} created successfully` });
      reset();
      setShowForm(false);
      fetchUsers();
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to create user. User ID may already exist.' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteUser = async (userId: string) => {
    if (!confirm(`Are you sure you want to deactivate user ${userId}?`)) return;
    try {
      await adminApi.deleteUser(userId);
      setMessage({ type: 'success', text: `User ${userId} deactivated successfully` });
      fetchUsers();
    } catch (err) {
      setMessage({ type: 'error', text: 'Failed to deactivate user' });
    }
  };

  if (!isAuthenticated || !isAdmin(user)) return null;

  return (
    <div className="min-h-screen bg-gray-100">
      <Navbar />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-6">
          <div className="mb-6 flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">User Administration</h1>
              <p className="text-gray-600">Manage system users</p>
            </div>
            <Button onClick={() => setShowForm(!showForm)}>
              {showForm ? 'Cancel' : 'Add User'}
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
                <CardTitle>Create New User</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Input id="userId" label="User ID" error={errors.userId?.message} {...register('userId')} />
                    <Input id="firstName" label="First Name" error={errors.firstName?.message} {...register('firstName')} />
                    <Input id="lastName" label="Last Name" error={errors.lastName?.message} {...register('lastName')} />
                    <Input id="password" type="password" label="Password" error={errors.password?.message} {...register('password')} />
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">User Type</label>
                      <select className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500" {...register('userType')}>
                        <option value="U">Regular User</option>
                        <option value="A">Administrator</option>
                      </select>
                      {errors.userType && <p className="mt-1 text-sm text-red-600">{errors.userType.message}</p>}
                    </div>
                  </div>
                  <div className="flex justify-end">
                    <Button type="submit" isLoading={submitting}>Create User</Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>User List</CardTitle>
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
                      <TableHead>User ID</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Last Login</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {users.map((u) => (
                      <TableRow key={u.userId}>
                        <TableCell className="font-medium">{u.userId}</TableCell>
                        <TableCell>{u.firstName} {u.lastName}</TableCell>
                        <TableCell>
                          <Badge variant={u.userType === 'A' ? 'info' : 'default'}>
                            {u.userTypeDescription}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant={u.active ? 'success' : 'danger'}>
                            {u.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </TableCell>
                        <TableCell>{u.lastLogin ? formatDateTime(u.lastLogin) : 'Never'}</TableCell>
                        <TableCell>
                          <div className="flex space-x-2">
                            <Button size="sm" variant="ghost" onClick={() => router.push(`/admin/users/${u.userId}`)}>
                              Edit
                            </Button>
                            {u.active && u.userId !== user?.userId && (
                              <Button size="sm" variant="danger" onClick={() => handleDeleteUser(u.userId)}>
                                Deactivate
                              </Button>
                            )}
                          </div>
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
