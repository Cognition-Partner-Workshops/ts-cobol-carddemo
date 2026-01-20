'use client';

import { useState } from 'react';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Badge from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Tag, FolderTree, Plus, Edit, Trash2, Search, ToggleLeft, ToggleRight } from 'lucide-react';

interface TransactionType {
  typeCode: string;
  typeDescription: string;
  debitCreditIndicator: string;
  categoryCode: string;
  affectsBalance: boolean;
  maxAmount: number | null;
  feePercentage: number | null;
  active: boolean;
}

interface TransactionCategory {
  categoryCode: string;
  categoryDescription: string;
  parentCategoryCode: string | null;
  categoryType: string;
  reportingGroup: string;
  active: boolean;
}

const mockTypes: TransactionType[] = [
  {
    typeCode: 'PR',
    typeDescription: 'Purchase',
    debitCreditIndicator: 'D',
    categoryCode: '1000',
    affectsBalance: true,
    maxAmount: 50000.00,
    feePercentage: null,
    active: true,
  },
  {
    typeCode: 'PA',
    typeDescription: 'Payment',
    debitCreditIndicator: 'C',
    categoryCode: '2000',
    affectsBalance: true,
    maxAmount: null,
    feePercentage: null,
    active: true,
  },
  {
    typeCode: 'CA',
    typeDescription: 'Cash Advance',
    debitCreditIndicator: 'D',
    categoryCode: '3000',
    affectsBalance: true,
    maxAmount: 5000.00,
    feePercentage: 3.00,
    active: true,
  },
  {
    typeCode: 'RF',
    typeDescription: 'Refund',
    debitCreditIndicator: 'C',
    categoryCode: '4000',
    affectsBalance: true,
    maxAmount: null,
    feePercentage: null,
    active: true,
  },
  {
    typeCode: 'FE',
    typeDescription: 'Fee',
    debitCreditIndicator: 'D',
    categoryCode: '5000',
    affectsBalance: true,
    maxAmount: null,
    feePercentage: null,
    active: true,
  },
  {
    typeCode: 'IN',
    typeDescription: 'Interest',
    debitCreditIndicator: 'D',
    categoryCode: '5000',
    affectsBalance: true,
    maxAmount: null,
    feePercentage: null,
    active: true,
  },
  {
    typeCode: 'BT',
    typeDescription: 'Balance Transfer',
    debitCreditIndicator: 'D',
    categoryCode: '6000',
    affectsBalance: true,
    maxAmount: 25000.00,
    feePercentage: 2.00,
    active: false,
  },
];

const mockCategories: TransactionCategory[] = [
  {
    categoryCode: '1000',
    categoryDescription: 'Purchases',
    parentCategoryCode: null,
    categoryType: 'DEBIT',
    reportingGroup: 'SPENDING',
    active: true,
  },
  {
    categoryCode: '2000',
    categoryDescription: 'Payments',
    parentCategoryCode: null,
    categoryType: 'CREDIT',
    reportingGroup: 'PAYMENTS',
    active: true,
  },
  {
    categoryCode: '3000',
    categoryDescription: 'Cash Advances',
    parentCategoryCode: null,
    categoryType: 'DEBIT',
    reportingGroup: 'CASH',
    active: true,
  },
  {
    categoryCode: '4000',
    categoryDescription: 'Refunds & Credits',
    parentCategoryCode: null,
    categoryType: 'CREDIT',
    reportingGroup: 'CREDITS',
    active: true,
  },
  {
    categoryCode: '5000',
    categoryDescription: 'Fees & Interest',
    parentCategoryCode: null,
    categoryType: 'DEBIT',
    reportingGroup: 'FEES',
    active: true,
  },
  {
    categoryCode: '6000',
    categoryDescription: 'Balance Transfers',
    parentCategoryCode: null,
    categoryType: 'DEBIT',
    reportingGroup: 'TRANSFERS',
    active: true,
  },
];

export default function TransactionTypesPage() {
  const [types, setTypes] = useState<TransactionType[]>(mockTypes);
  const [categories, setCategories] = useState<TransactionCategory[]>(mockCategories);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<'types' | 'categories'>('types');

  const toggleTypeStatus = (typeCode: string) => {
    setTypes(types.map(type => 
      type.typeCode === typeCode ? { ...type, active: !type.active } : type
    ));
  };

  const toggleCategoryStatus = (categoryCode: string) => {
    setCategories(categories.map(cat => 
      cat.categoryCode === categoryCode ? { ...cat, active: !cat.active } : cat
    ));
  };

  const formatCurrency = (amount: number | null) => {
    if (amount === null) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const filteredTypes = types.filter(type =>
    type.typeDescription.toLowerCase().includes(searchTerm.toLowerCase()) ||
    type.typeCode.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredCategories = categories.filter(cat =>
    cat.categoryDescription.toLowerCase().includes(searchTerm.toLowerCase()) ||
    cat.categoryCode.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Transaction Type Management</h1>
        <p className="text-sm text-gray-500">EPIC-010: Transaction Type Configuration</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-100 rounded-lg">
                <Tag className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Transaction Types</p>
                <p className="text-2xl font-bold">{types.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-green-100 rounded-lg">
                <ToggleRight className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Active Types</p>
                <p className="text-2xl font-bold">{types.filter(t => t.active).length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-purple-100 rounded-lg">
                <FolderTree className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Categories</p>
                <p className="text-2xl font-bold">{categories.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-orange-100 rounded-lg">
                <ToggleLeft className="h-6 w-6 text-orange-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Inactive Types</p>
                <p className="text-2xl font-bold">{types.filter(t => !t.active).length}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="flex gap-2 border-b">
        <button
          className={`px-4 py-2 font-medium ${activeTab === 'types' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
          onClick={() => setActiveTab('types')}
        >
          Transaction Types
        </button>
        <button
          className={`px-4 py-2 font-medium ${activeTab === 'categories' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
          onClick={() => setActiveTab('categories')}
        >
          Categories
        </button>
      </div>

      {activeTab === 'types' && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>Transaction Types</CardTitle>
              <div className="flex gap-2">
                <div className="relative w-64">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <Input
                    placeholder="Search types..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Button size="sm">
                  <Plus className="h-4 w-4 mr-2" />
                  Add Type
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Code</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>D/C</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Max Amount</TableHead>
                  <TableHead>Fee %</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTypes.map((type) => (
                  <TableRow key={type.typeCode}>
                    <TableCell className="font-mono font-bold">{type.typeCode}</TableCell>
                    <TableCell>{type.typeDescription}</TableCell>
                    <TableCell>
                      <Badge variant={type.debitCreditIndicator === 'D' ? 'destructive' : 'success'}>
                        {type.debitCreditIndicator === 'D' ? 'Debit' : 'Credit'}
                      </Badge>
                    </TableCell>
                    <TableCell>{type.categoryCode}</TableCell>
                    <TableCell>{formatCurrency(type.maxAmount)}</TableCell>
                    <TableCell>{type.feePercentage ? `${type.feePercentage}%` : '-'}</TableCell>
                    <TableCell>
                      <Badge variant={type.active ? 'success' : 'secondary'}>
                        {type.active ? 'Active' : 'Inactive'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" onClick={() => toggleTypeStatus(type.typeCode)}>
                          {type.active ? 'Disable' : 'Enable'}
                        </Button>
                        <Button size="sm" variant="outline">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button size="sm" variant="outline">
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {activeTab === 'categories' && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>Transaction Categories</CardTitle>
              <div className="flex gap-2">
                <div className="relative w-64">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <Input
                    placeholder="Search categories..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Button size="sm">
                  <Plus className="h-4 w-4 mr-2" />
                  Add Category
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Code</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Reporting Group</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCategories.map((cat) => (
                  <TableRow key={cat.categoryCode}>
                    <TableCell className="font-mono font-bold">{cat.categoryCode}</TableCell>
                    <TableCell>{cat.categoryDescription}</TableCell>
                    <TableCell>
                      <Badge variant={cat.categoryType === 'DEBIT' ? 'destructive' : 'success'}>
                        {cat.categoryType}
                      </Badge>
                    </TableCell>
                    <TableCell>{cat.reportingGroup}</TableCell>
                    <TableCell>
                      <Badge variant={cat.active ? 'success' : 'secondary'}>
                        {cat.active ? 'Active' : 'Inactive'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" onClick={() => toggleCategoryStatus(cat.categoryCode)}>
                          {cat.active ? 'Disable' : 'Enable'}
                        </Button>
                        <Button size="sm" variant="outline">
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button size="sm" variant="outline">
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
