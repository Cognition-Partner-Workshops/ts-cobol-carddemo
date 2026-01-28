import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { customerService } from '../services/customerService';
import { Customer } from '../types';
import LoadingSpinner from '../components/common/LoadingSpinner';
import Pagination from '../components/common/Pagination';
import Modal from '../components/common/Modal';
import Alert from '../components/common/Alert';

export default function Customers() {
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [alert, setAlert] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['customers', page, searchQuery],
    queryFn: () => searchQuery 
      ? customerService.search(searchQuery, page, 10)
      : customerService.getAll(page, 10),
  });

  const deleteMutation = useMutation({
    mutationFn: customerService.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      setAlert({ type: 'success', message: 'Customer deleted successfully' });
    },
    onError: () => {
      setAlert({ type: 'error', message: 'Failed to delete customer' });
    },
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
  };

  const handleEdit = (customer: Customer) => {
    setSelectedCustomer(customer);
    setIsModalOpen(true);
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this customer?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleAddNew = () => {
    setSelectedCustomer(null);
    setIsModalOpen(true);
  };

  const maskSSN = (ssn: string) => {
    if (!ssn || ssn.length < 4) return '***-**-****';
    return `***-**-${ssn.slice(-4)}`;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return <Alert type="error" message="Failed to load customers" />;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Customers</h1>
        <button onClick={handleAddNew} className="btn-primary">
          Add Customer
        </button>
      </div>

      {alert && (
        <Alert type={alert.type} message={alert.message} onClose={() => setAlert(null)} />
      )}

      <div className="card mb-6">
        <form onSubmit={handleSearch} className="flex gap-4">
          <input
            type="text"
            placeholder="Search by name, SSN, or phone..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input-field flex-1"
          />
          <button type="submit" className="btn-primary">
            Search
          </button>
          {searchQuery && (
            <button
              type="button"
              onClick={() => { setSearchQuery(''); setPage(0); }}
              className="btn-secondary"
            >
              Clear
            </button>
          )}
        </form>
      </div>

      <div className="card overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 table-header">ID</th>
              <th className="px-6 py-3 table-header">Name</th>
              <th className="px-6 py-3 table-header">SSN</th>
              <th className="px-6 py-3 table-header">City</th>
              <th className="px-6 py-3 table-header">State</th>
              <th className="px-6 py-3 table-header">FICO Score</th>
              <th className="px-6 py-3 table-header">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {data?.content.map((customer) => (
              <tr key={customer.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap text-sm">{customer.id}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">
                    {customer.firstName} {customer.middleName} {customer.lastName}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {maskSSN(customer.ssn)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">{customer.city}</td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">{customer.state}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs rounded-full ${
                    (customer.ficoScore || 0) >= 700 ? 'bg-green-100 text-green-800' :
                    (customer.ficoScore || 0) >= 600 ? 'bg-yellow-100 text-yellow-800' :
                    'bg-red-100 text-red-800'
                  }`}>
                    {customer.ficoScore || 'N/A'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <button
                    onClick={() => handleEdit(customer)}
                    className="text-blue-600 hover:text-blue-800 mr-3"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(customer.id)}
                    className="text-red-600 hover:text-red-800"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {data && (
          <Pagination
            currentPage={page}
            totalPages={data.totalPages}
            onPageChange={setPage}
          />
        )}
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={selectedCustomer ? 'Edit Customer' : 'Add Customer'}
        size="lg"
      >
        <CustomerForm
          customer={selectedCustomer}
          onClose={() => setIsModalOpen(false)}
          onSuccess={() => {
            setIsModalOpen(false);
            queryClient.invalidateQueries({ queryKey: ['customers'] });
            setAlert({ type: 'success', message: selectedCustomer ? 'Customer updated' : 'Customer created' });
          }}
        />
      </Modal>
    </div>
  );
}

interface CustomerFormProps {
  customer: Customer | null;
  onClose: () => void;
  onSuccess: () => void;
}

function CustomerForm({ customer, onClose, onSuccess }: CustomerFormProps) {
  const [formData, setFormData] = useState({
    firstName: customer?.firstName || '',
    middleName: customer?.middleName || '',
    lastName: customer?.lastName || '',
    addressLine1: customer?.addressLine1 || '',
    addressLine2: customer?.addressLine2 || '',
    city: customer?.city || '',
    state: customer?.state || '',
    zipCode: customer?.zipCode || '',
    countryCode: customer?.countryCode || 'USA',
    phoneNumber1: customer?.phoneNumber1 || '',
    phoneNumber2: customer?.phoneNumber2 || '',
    ssn: customer?.ssn || '',
    ficoScore: customer?.ficoScore || 0,
    dateOfBirth: customer?.dateOfBirth || '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const value = e.target.type === 'number' ? Number(e.target.value) : e.target.value;
    setFormData({ ...formData, [e.target.name]: value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      if (customer) {
        await customerService.update(customer.id, formData);
      } else {
        await customerService.create(formData as Omit<Customer, 'id'>);
      }
      onSuccess();
    } catch {
      setError('Failed to save customer');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <Alert type="error" message={error} />}
      
      <div className="grid grid-cols-3 gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
          <input
            type="text"
            name="firstName"
            value={formData.firstName}
            onChange={handleChange}
            className="input-field"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Middle Name</label>
          <input
            type="text"
            name="middleName"
            value={formData.middleName}
            onChange={handleChange}
            className="input-field"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
          <input
            type="text"
            name="lastName"
            value={formData.lastName}
            onChange={handleChange}
            className="input-field"
            required
          />
        </div>
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Address Line 1</label>
        <input
          type="text"
          name="addressLine1"
          value={formData.addressLine1}
          onChange={handleChange}
          className="input-field"
          required
        />
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1">Address Line 2</label>
        <input
          type="text"
          name="addressLine2"
          value={formData.addressLine2}
          onChange={handleChange}
          className="input-field"
        />
      </div>

      <div className="grid grid-cols-3 gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
          <input
            type="text"
            name="city"
            value={formData.city}
            onChange={handleChange}
            className="input-field"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
          <input
            type="text"
            name="state"
            value={formData.state}
            onChange={handleChange}
            className="input-field"
            maxLength={2}
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">ZIP Code</label>
          <input
            type="text"
            name="zipCode"
            value={formData.zipCode}
            onChange={handleChange}
            className="input-field"
            required
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Phone 1</label>
          <input
            type="tel"
            name="phoneNumber1"
            value={formData.phoneNumber1}
            onChange={handleChange}
            className="input-field"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Phone 2</label>
          <input
            type="tel"
            name="phoneNumber2"
            value={formData.phoneNumber2}
            onChange={handleChange}
            className="input-field"
          />
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">SSN</label>
          <input
            type="text"
            name="ssn"
            value={formData.ssn}
            onChange={handleChange}
            className="input-field"
            placeholder="XXX-XX-XXXX"
            required={!customer}
            disabled={!!customer}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">FICO Score</label>
          <input
            type="number"
            name="ficoScore"
            value={formData.ficoScore}
            onChange={handleChange}
            className="input-field"
            min={300}
            max={850}
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Date of Birth</label>
          <input
            type="date"
            name="dateOfBirth"
            value={formData.dateOfBirth}
            onChange={handleChange}
            className="input-field"
          />
        </div>
      </div>

      <div className="flex justify-end gap-3">
        <button type="button" onClick={onClose} className="btn-secondary">
          Cancel
        </button>
        <button type="submit" disabled={loading} className="btn-primary">
          {loading ? 'Saving...' : 'Save'}
        </button>
      </div>
    </form>
  );
}
