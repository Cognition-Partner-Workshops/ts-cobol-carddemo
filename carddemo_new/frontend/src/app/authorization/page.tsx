'use client';

import { useState } from 'react';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Badge from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Shield, CheckCircle, XCircle, AlertTriangle, Search, Plus, Edit, Trash2 } from 'lucide-react';

interface AuthorizationRequest {
  authId: string;
  cardNumber: string;
  merchantName: string;
  amount: number;
  status: string;
  responseCode: string;
  declineReason: string | null;
  requestTimestamp: string;
}

interface AuthorizationRule {
  id: number;
  ruleCode: string;
  ruleName: string;
  ruleType: string;
  maxAmount: number | null;
  dailyLimit: number | null;
  velocityCount: number | null;
  action: string;
  active: boolean;
}

const mockAuthorizations: AuthorizationRequest[] = [
  {
    authId: 'AUTH1705123456ABCD',
    cardNumber: '************1234',
    merchantName: 'Amazon.com',
    amount: 125.99,
    status: 'APPROVED',
    responseCode: '00',
    declineReason: null,
    requestTimestamp: '2026-01-20T14:32:15',
  },
  {
    authId: 'AUTH1705123457EFGH',
    cardNumber: '************5678',
    merchantName: 'Best Buy',
    amount: 899.00,
    status: 'APPROVED',
    responseCode: '00',
    declineReason: null,
    requestTimestamp: '2026-01-20T14:28:42',
  },
  {
    authId: 'AUTH1705123458IJKL',
    cardNumber: '************9012',
    merchantName: 'Luxury Goods Inc',
    amount: 5500.00,
    status: 'DECLINED',
    responseCode: '65',
    declineReason: 'Transaction would exceed credit limit',
    requestTimestamp: '2026-01-20T14:15:33',
  },
  {
    authId: 'AUTH1705123459MNOP',
    cardNumber: '************3456',
    merchantName: 'Gas Station',
    amount: 45.50,
    status: 'APPROVED',
    responseCode: '00',
    declineReason: null,
    requestTimestamp: '2026-01-20T13:55:21',
  },
  {
    authId: 'AUTH1705123460QRST',
    cardNumber: '************7890',
    merchantName: 'Online Casino',
    amount: 200.00,
    status: 'DECLINED',
    responseCode: '57',
    declineReason: 'Declined by rule: Gambling Restriction',
    requestTimestamp: '2026-01-20T13:42:18',
  },
];

const mockRules: AuthorizationRule[] = [
  {
    id: 1,
    ruleCode: 'RULE001',
    ruleName: 'High Value Transaction',
    ruleType: 'AMOUNT_LIMIT',
    maxAmount: 5000.00,
    dailyLimit: null,
    velocityCount: null,
    action: 'DECLINE',
    active: true,
  },
  {
    id: 2,
    ruleCode: 'RULE002',
    ruleName: 'Daily Spending Limit',
    ruleType: 'DAILY_LIMIT',
    maxAmount: null,
    dailyLimit: 10000.00,
    velocityCount: null,
    action: 'DECLINE',
    active: true,
  },
  {
    id: 3,
    ruleCode: 'RULE003',
    ruleName: 'Velocity Check',
    ruleType: 'VELOCITY',
    maxAmount: null,
    dailyLimit: null,
    velocityCount: 10,
    action: 'DECLINE',
    active: true,
  },
  {
    id: 4,
    ruleCode: 'RULE004',
    ruleName: 'Gambling Restriction',
    ruleType: 'MCC_RESTRICTION',
    maxAmount: null,
    dailyLimit: null,
    velocityCount: null,
    action: 'DECLINE',
    active: true,
  },
  {
    id: 5,
    ruleCode: 'RULE005',
    ruleName: 'International Block',
    ruleType: 'COUNTRY_RESTRICTION',
    maxAmount: null,
    dailyLimit: null,
    velocityCount: null,
    action: 'DECLINE',
    active: false,
  },
];

export default function AuthorizationPage() {
  const [authorizations] = useState<AuthorizationRequest[]>(mockAuthorizations);
  const [rules, setRules] = useState<AuthorizationRule[]>(mockRules);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<'history' | 'rules'>('history');

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return <Badge variant="success">{status}</Badge>;
      case 'DECLINED':
        return <Badge variant="destructive">{status}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'DECLINED':
        return <XCircle className="h-5 w-5 text-red-500" />;
      default:
        return <AlertTriangle className="h-5 w-5 text-yellow-500" />;
    }
  };

  const toggleRule = (ruleId: number) => {
    setRules(rules.map(rule => 
      rule.id === ruleId ? { ...rule, active: !rule.active } : rule
    ));
  };

  const formatDateTime = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const filteredAuthorizations = authorizations.filter(auth =>
    auth.merchantName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    auth.authId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    auth.cardNumber.includes(searchTerm)
  );

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Authorization Processing</h1>
        <p className="text-sm text-gray-500">EPIC-009: Real-time Authorization</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-green-100 rounded-lg">
                <CheckCircle className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Approved Today</p>
                <p className="text-2xl font-bold">1,247</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-red-100 rounded-lg">
                <XCircle className="h-6 w-6 text-red-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Declined Today</p>
                <p className="text-2xl font-bold">43</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-100 rounded-lg">
                <Shield className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Approval Rate</p>
                <p className="text-2xl font-bold">96.7%</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-purple-100 rounded-lg">
                <AlertTriangle className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Active Rules</p>
                <p className="text-2xl font-bold">{rules.filter(r => r.active).length}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="flex gap-2 border-b">
        <button
          className={`px-4 py-2 font-medium ${activeTab === 'history' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
          onClick={() => setActiveTab('history')}
        >
          Authorization History
        </button>
        <button
          className={`px-4 py-2 font-medium ${activeTab === 'rules' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
          onClick={() => setActiveTab('rules')}
        >
          Authorization Rules
        </button>
      </div>

      {activeTab === 'history' && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>Recent Authorizations</CardTitle>
              <div className="relative w-64">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search authorizations..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Status</TableHead>
                  <TableHead>Auth ID</TableHead>
                  <TableHead>Card</TableHead>
                  <TableHead>Merchant</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead>Response</TableHead>
                  <TableHead>Timestamp</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredAuthorizations.map((auth) => (
                  <TableRow key={auth.authId}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {getStatusIcon(auth.status)}
                        {getStatusBadge(auth.status)}
                      </div>
                    </TableCell>
                    <TableCell className="font-mono text-sm">{auth.authId}</TableCell>
                    <TableCell>{auth.cardNumber}</TableCell>
                    <TableCell>{auth.merchantName}</TableCell>
                    <TableCell className="text-right font-medium">{formatCurrency(auth.amount)}</TableCell>
                    <TableCell>
                      <span className="font-mono">{auth.responseCode}</span>
                      {auth.declineReason && (
                        <p className="text-xs text-red-500 mt-1">{auth.declineReason}</p>
                      )}
                    </TableCell>
                    <TableCell>{formatDateTime(auth.requestTimestamp)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {activeTab === 'rules' && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>Authorization Rules</CardTitle>
              <Button size="sm">
                <Plus className="h-4 w-4 mr-2" />
                Add Rule
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Rule Code</TableHead>
                  <TableHead>Rule Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Limit</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rules.map((rule) => (
                  <TableRow key={rule.id}>
                    <TableCell className="font-mono">{rule.ruleCode}</TableCell>
                    <TableCell className="font-medium">{rule.ruleName}</TableCell>
                    <TableCell>{rule.ruleType}</TableCell>
                    <TableCell>
                      {rule.maxAmount && formatCurrency(rule.maxAmount)}
                      {rule.dailyLimit && formatCurrency(rule.dailyLimit)}
                      {rule.velocityCount && `${rule.velocityCount} txns/hr`}
                      {!rule.maxAmount && !rule.dailyLimit && !rule.velocityCount && '-'}
                    </TableCell>
                    <TableCell>
                      <Badge variant={rule.action === 'DECLINE' ? 'destructive' : 'warning'}>
                        {rule.action}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant={rule.active ? 'success' : 'secondary'}>
                        {rule.active ? 'Active' : 'Inactive'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" onClick={() => toggleRule(rule.id)}>
                          {rule.active ? 'Disable' : 'Enable'}
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
