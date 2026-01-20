'use client';

import { useState } from 'react';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Badge from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { 
  Network, Server, MessageSquare, Download, Upload, 
  CheckCircle, XCircle, Clock, RefreshCw, Plus, 
  Activity, Send, FileDown, Search 
} from 'lucide-react';

interface ExternalSystem {
  systemCode: string;
  systemName: string;
  systemType: string;
  endpointUrl: string;
  healthStatus: string;
  active: boolean;
  lastHealthCheck: string;
}

interface IntegrationMessage {
  messageId: string;
  sourceSystem: string;
  targetSystem: string;
  messageType: string;
  direction: string;
  status: string;
  createdAt: string;
}

interface DataExport {
  exportId: string;
  exportType: string;
  entityType: string;
  recordCount: number;
  status: string;
  startedAt: string;
  requestedBy: string;
}

const mockSystems: ExternalSystem[] = [
  {
    systemCode: 'CORE_BANKING',
    systemName: 'Core Banking System',
    systemType: 'BANKING',
    endpointUrl: 'https://core.bank.internal/api',
    healthStatus: 'HEALTHY',
    active: true,
    lastHealthCheck: '2026-01-20T15:00:00',
  },
  {
    systemCode: 'FRAUD_DETECT',
    systemName: 'Fraud Detection Service',
    systemType: 'SECURITY',
    endpointUrl: 'https://fraud.security.internal/api',
    healthStatus: 'HEALTHY',
    active: true,
    lastHealthCheck: '2026-01-20T15:00:00',
  },
  {
    systemCode: 'CREDIT_BUREAU',
    systemName: 'Credit Bureau Gateway',
    systemType: 'EXTERNAL',
    endpointUrl: 'https://api.creditbureau.com/v2',
    healthStatus: 'UNHEALTHY',
    active: true,
    lastHealthCheck: '2026-01-20T14:55:00',
  },
  {
    systemCode: 'NOTIFICATION',
    systemName: 'Notification Service',
    systemType: 'MESSAGING',
    endpointUrl: 'https://notify.internal/api',
    healthStatus: 'HEALTHY',
    active: true,
    lastHealthCheck: '2026-01-20T15:00:00',
  },
  {
    systemCode: 'LEGACY_MF',
    systemName: 'Legacy Mainframe',
    systemType: 'LEGACY',
    endpointUrl: 'mq://mainframe.internal:1414',
    healthStatus: 'UNKNOWN',
    active: false,
    lastHealthCheck: '2026-01-15T10:00:00',
  },
];

const mockMessages: IntegrationMessage[] = [
  {
    messageId: 'MSG1705123456ABCD',
    sourceSystem: 'CARDDEMO',
    targetSystem: 'FRAUD_DETECT',
    messageType: 'TRANSACTION_CHECK',
    direction: 'OUTBOUND',
    status: 'COMPLETED',
    createdAt: '2026-01-20T14:58:32',
  },
  {
    messageId: 'MSG1705123457EFGH',
    sourceSystem: 'CORE_BANKING',
    targetSystem: 'CARDDEMO',
    messageType: 'BALANCE_UPDATE',
    direction: 'INBOUND',
    status: 'COMPLETED',
    createdAt: '2026-01-20T14:55:18',
  },
  {
    messageId: 'MSG1705123458IJKL',
    sourceSystem: 'CARDDEMO',
    targetSystem: 'NOTIFICATION',
    messageType: 'ALERT_NOTIFICATION',
    direction: 'OUTBOUND',
    status: 'PENDING',
    createdAt: '2026-01-20T14:52:45',
  },
  {
    messageId: 'MSG1705123459MNOP',
    sourceSystem: 'CARDDEMO',
    targetSystem: 'CREDIT_BUREAU',
    messageType: 'CREDIT_CHECK',
    direction: 'OUTBOUND',
    status: 'FAILED',
    createdAt: '2026-01-20T14:48:21',
  },
];

const mockExports: DataExport[] = [
  {
    exportId: 'EXP1705123456ABCD',
    exportType: 'SCHEDULED',
    entityType: 'TRANSACTIONS',
    recordCount: 15420,
    status: 'COMPLETED',
    startedAt: '2026-01-20T02:00:00',
    requestedBy: 'SYSTEM',
  },
  {
    exportId: 'EXP1705123457EFGH',
    exportType: 'MANUAL',
    entityType: 'ACCOUNTS',
    recordCount: 850,
    status: 'COMPLETED',
    startedAt: '2026-01-20T10:30:00',
    requestedBy: 'ADMIN001',
  },
  {
    exportId: 'EXP1705123458IJKL',
    exportType: 'MANUAL',
    entityType: 'CUSTOMERS',
    recordCount: 0,
    status: 'PROCESSING',
    startedAt: '2026-01-20T14:45:00',
    requestedBy: 'ADMIN001',
  },
];

export default function IntegrationPage() {
  const [systems] = useState<ExternalSystem[]>(mockSystems);
  const [messages] = useState<IntegrationMessage[]>(mockMessages);
  const [exports] = useState<DataExport[]>(mockExports);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<'systems' | 'messages' | 'exports'>('systems');

  const getHealthBadge = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return <Badge variant="success">{status}</Badge>;
      case 'UNHEALTHY':
        return <Badge variant="destructive">{status}</Badge>;
      case 'UNKNOWN':
        return <Badge variant="secondary">{status}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <Badge variant="success">{status}</Badge>;
      case 'PENDING':
        return <Badge variant="warning">{status}</Badge>;
      case 'PROCESSING':
        return <Badge variant="warning">{status}</Badge>;
      case 'FAILED':
        return <Badge variant="destructive">{status}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  const getHealthIcon = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'UNHEALTHY':
        return <XCircle className="h-5 w-5 text-red-500" />;
      case 'UNKNOWN':
        return <Clock className="h-5 w-5 text-gray-500" />;
      default:
        return <Activity className="h-5 w-5 text-gray-500" />;
    }
  };

  const formatDateTime = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">System Integration</h1>
        <p className="text-sm text-gray-500">EPIC-011: External System Integration</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-100 rounded-lg">
                <Server className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">External Systems</p>
                <p className="text-2xl font-bold">{systems.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-green-100 rounded-lg">
                <CheckCircle className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Healthy Systems</p>
                <p className="text-2xl font-bold">{systems.filter(s => s.healthStatus === 'HEALTHY').length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-purple-100 rounded-lg">
                <MessageSquare className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Messages Today</p>
                <p className="text-2xl font-bold">1,247</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-orange-100 rounded-lg">
                <Download className="h-6 w-6 text-orange-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Exports Today</p>
                <p className="text-2xl font-bold">{exports.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="flex gap-2 border-b">
        <button
          className={`px-4 py-2 font-medium ${activeTab === 'systems' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
          onClick={() => setActiveTab('systems')}
        >
          External Systems
        </button>
        <button
          className={`px-4 py-2 font-medium ${activeTab === 'messages' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
          onClick={() => setActiveTab('messages')}
        >
          Message Queue
        </button>
        <button
          className={`px-4 py-2 font-medium ${activeTab === 'exports' ? 'border-b-2 border-blue-500 text-blue-600' : 'text-gray-500'}`}
          onClick={() => setActiveTab('exports')}
        >
          Data Exports
        </button>
      </div>

      {activeTab === 'systems' && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>External Systems</CardTitle>
              <div className="flex gap-2">
                <Button size="sm" variant="outline">
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Health Check All
                </Button>
                <Button size="sm">
                  <Plus className="h-4 w-4 mr-2" />
                  Add System
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Health</TableHead>
                  <TableHead>System Code</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Endpoint</TableHead>
                  <TableHead>Last Check</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {systems.map((system) => (
                  <TableRow key={system.systemCode}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {getHealthIcon(system.healthStatus)}
                        {getHealthBadge(system.healthStatus)}
                      </div>
                    </TableCell>
                    <TableCell className="font-mono font-bold">{system.systemCode}</TableCell>
                    <TableCell>{system.systemName}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{system.systemType}</Badge>
                    </TableCell>
                    <TableCell className="font-mono text-sm max-w-xs truncate">{system.endpointUrl}</TableCell>
                    <TableCell>{formatDateTime(system.lastHealthCheck)}</TableCell>
                    <TableCell>
                      <Badge variant={system.active ? 'success' : 'secondary'}>
                        {system.active ? 'Active' : 'Inactive'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline">
                          <RefreshCw className="h-4 w-4" />
                        </Button>
                        <Button size="sm" variant="outline">
                          {system.active ? 'Disable' : 'Enable'}
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

      {activeTab === 'messages' && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>Integration Messages</CardTitle>
              <div className="flex gap-2">
                <div className="relative w-64">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <Input
                    placeholder="Search messages..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Button size="sm">
                  <Send className="h-4 w-4 mr-2" />
                  Send Message
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Status</TableHead>
                  <TableHead>Message ID</TableHead>
                  <TableHead>Direction</TableHead>
                  <TableHead>Source</TableHead>
                  <TableHead>Target</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {messages.map((msg) => (
                  <TableRow key={msg.messageId}>
                    <TableCell>{getStatusBadge(msg.status)}</TableCell>
                    <TableCell className="font-mono text-sm">{msg.messageId}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        {msg.direction === 'OUTBOUND' ? (
                          <Upload className="h-4 w-4 text-blue-500" />
                        ) : (
                          <Download className="h-4 w-4 text-green-500" />
                        )}
                        {msg.direction}
                      </div>
                    </TableCell>
                    <TableCell>{msg.sourceSystem}</TableCell>
                    <TableCell>{msg.targetSystem}</TableCell>
                    <TableCell>{msg.messageType}</TableCell>
                    <TableCell>{formatDateTime(msg.createdAt)}</TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline">View</Button>
                        {msg.status === 'FAILED' && (
                          <Button size="sm" variant="outline">Retry</Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {activeTab === 'exports' && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle>Data Exports</CardTitle>
              <Button size="sm">
                <FileDown className="h-4 w-4 mr-2" />
                New Export
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Status</TableHead>
                  <TableHead>Export ID</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Entity</TableHead>
                  <TableHead className="text-right">Records</TableHead>
                  <TableHead>Started</TableHead>
                  <TableHead>Requested By</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {exports.map((exp) => (
                  <TableRow key={exp.exportId}>
                    <TableCell>{getStatusBadge(exp.status)}</TableCell>
                    <TableCell className="font-mono text-sm">{exp.exportId}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{exp.exportType}</Badge>
                    </TableCell>
                    <TableCell>{exp.entityType}</TableCell>
                    <TableCell className="text-right">{exp.recordCount.toLocaleString()}</TableCell>
                    <TableCell>{formatDateTime(exp.startedAt)}</TableCell>
                    <TableCell>{exp.requestedBy}</TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        {exp.status === 'COMPLETED' && (
                          <Button size="sm" variant="outline">
                            <Download className="h-4 w-4" />
                          </Button>
                        )}
                        {exp.status === 'PROCESSING' && (
                          <Button size="sm" variant="outline">Cancel</Button>
                        )}
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
