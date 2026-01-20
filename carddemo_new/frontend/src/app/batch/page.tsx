'use client';

import { useState } from 'react';
import Card, { CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import Badge from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Play, Clock, FileText, Settings, RefreshCw, CheckCircle, XCircle, AlertCircle } from 'lucide-react';

interface BatchJob {
  id: number;
  jobName: string;
  jobType: string;
  status: string;
  startTime: string;
  endTime: string | null;
  recordsProcessed: number;
  recordsSuccess: number;
  recordsFailed: number;
}

const mockJobs: BatchJob[] = [
  {
    id: 1,
    jobName: 'Daily End of Day Processing',
    jobType: 'END_OF_DAY',
    status: 'COMPLETED',
    startTime: '2026-01-20T23:00:00',
    endTime: '2026-01-20T23:15:32',
    recordsProcessed: 1250,
    recordsSuccess: 1248,
    recordsFailed: 2,
  },
  {
    id: 2,
    jobName: 'Daily Interest Calculation',
    jobType: 'INTEREST',
    status: 'COMPLETED',
    startTime: '2026-01-20T23:16:00',
    endTime: '2026-01-20T23:18:45',
    recordsProcessed: 850,
    recordsSuccess: 850,
    recordsFailed: 0,
  },
  {
    id: 3,
    jobName: 'Monthly Statement Generation',
    jobType: 'STATEMENT',
    status: 'RUNNING',
    startTime: '2026-01-20T02:00:00',
    endTime: null,
    recordsProcessed: 450,
    recordsSuccess: 450,
    recordsFailed: 0,
  },
  {
    id: 4,
    jobName: 'Account File Maintenance',
    jobType: 'FILE_MAINTENANCE',
    status: 'PENDING',
    startTime: '2026-01-21T03:00:00',
    endTime: null,
    recordsProcessed: 0,
    recordsSuccess: 0,
    recordsFailed: 0,
  },
];

export default function BatchPage() {
  const [jobs, setJobs] = useState<BatchJob[]>(mockJobs);
  const [isRunning, setIsRunning] = useState<string | null>(null);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <Badge variant="success">{status}</Badge>;
      case 'RUNNING':
        return <Badge variant="warning">{status}</Badge>;
      case 'FAILED':
        return <Badge variant="destructive">{status}</Badge>;
      case 'PENDING':
        return <Badge variant="secondary">{status}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'RUNNING':
        return <RefreshCw className="h-5 w-5 text-yellow-500 animate-spin" />;
      case 'FAILED':
        return <XCircle className="h-5 w-5 text-red-500" />;
      case 'PENDING':
        return <Clock className="h-5 w-5 text-gray-500" />;
      default:
        return <AlertCircle className="h-5 w-5 text-gray-500" />;
    }
  };

  const runJob = (jobType: string) => {
    setIsRunning(jobType);
    setTimeout(() => {
      setIsRunning(null);
      alert(`${jobType} job started successfully`);
    }, 1000);
  };

  const formatDateTime = (dateStr: string | null) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Batch Processing</h1>
        <p className="text-sm text-gray-500">EPIC-008: Batch Processing Management</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">End of Day</p>
                <p className="text-lg font-semibold">EOD Processing</p>
              </div>
              <Button
                size="sm"
                onClick={() => runJob('END_OF_DAY')}
                disabled={isRunning === 'END_OF_DAY'}
              >
                {isRunning === 'END_OF_DAY' ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <Play className="h-4 w-4" />
                )}
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">Interest</p>
                <p className="text-lg font-semibold">Calculate Interest</p>
              </div>
              <Button
                size="sm"
                onClick={() => runJob('INTEREST')}
                disabled={isRunning === 'INTEREST'}
              >
                {isRunning === 'INTEREST' ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <Play className="h-4 w-4" />
                )}
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">Statements</p>
                <p className="text-lg font-semibold">Generate Statements</p>
              </div>
              <Button
                size="sm"
                onClick={() => runJob('STATEMENT')}
                disabled={isRunning === 'STATEMENT'}
              >
                {isRunning === 'STATEMENT' ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <FileText className="h-4 w-4" />
                )}
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">Maintenance</p>
                <p className="text-lg font-semibold">File Maintenance</p>
              </div>
              <Button
                size="sm"
                onClick={() => runJob('FILE_MAINTENANCE')}
                disabled={isRunning === 'FILE_MAINTENANCE'}
              >
                {isRunning === 'FILE_MAINTENANCE' ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <Settings className="h-4 w-4" />
                )}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Job History</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Status</TableHead>
                <TableHead>Job Name</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Start Time</TableHead>
                <TableHead>End Time</TableHead>
                <TableHead className="text-right">Processed</TableHead>
                <TableHead className="text-right">Success</TableHead>
                <TableHead className="text-right">Failed</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {jobs.map((job) => (
                <TableRow key={job.id}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      {getStatusIcon(job.status)}
                      {getStatusBadge(job.status)}
                    </div>
                  </TableCell>
                  <TableCell className="font-medium">{job.jobName}</TableCell>
                  <TableCell>{job.jobType}</TableCell>
                  <TableCell>{formatDateTime(job.startTime)}</TableCell>
                  <TableCell>{formatDateTime(job.endTime)}</TableCell>
                  <TableCell className="text-right">{job.recordsProcessed.toLocaleString()}</TableCell>
                  <TableCell className="text-right text-green-600">{job.recordsSuccess.toLocaleString()}</TableCell>
                  <TableCell className="text-right text-red-600">{job.recordsFailed.toLocaleString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Scheduled Jobs</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <Clock className="h-5 w-5 text-blue-500" />
                  <div>
                    <p className="font-medium">End of Day Processing</p>
                    <p className="text-sm text-gray-500">Daily at 11:00 PM</p>
                  </div>
                </div>
                <Badge variant="success">Active</Badge>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <Clock className="h-5 w-5 text-blue-500" />
                  <div>
                    <p className="font-medium">Monthly Statements</p>
                    <p className="text-sm text-gray-500">1st of month at 2:00 AM</p>
                  </div>
                </div>
                <Badge variant="success">Active</Badge>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <Clock className="h-5 w-5 text-blue-500" />
                  <div>
                    <p className="font-medium">Account Maintenance</p>
                    <p className="text-sm text-gray-500">Daily at 3:00 AM</p>
                  </div>
                </div>
                <Badge variant="success">Active</Badge>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <Clock className="h-5 w-5 text-blue-500" />
                  <div>
                    <p className="font-medium">Transaction Archival</p>
                    <p className="text-sm text-gray-500">Weekly on Sunday at 4:00 AM</p>
                  </div>
                </div>
                <Badge variant="success">Active</Badge>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Job Statistics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-gray-600">Total Jobs Today</span>
                <span className="font-semibold">12</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600">Successful</span>
                <span className="font-semibold text-green-600">10</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600">Failed</span>
                <span className="font-semibold text-red-600">1</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600">Running</span>
                <span className="font-semibold text-yellow-600">1</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600">Records Processed</span>
                <span className="font-semibold">15,420</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-gray-600">Average Duration</span>
                <span className="font-semibold">8m 32s</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
