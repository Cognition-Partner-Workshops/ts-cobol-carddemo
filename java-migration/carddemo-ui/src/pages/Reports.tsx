import { useQuery } from '@tanstack/react-query';
import { FileText, CreditCard, Activity, Users } from 'lucide-react';
import { reportApi } from '../services/api';
import { format, subDays } from 'date-fns';

export function Reports() {
  const startDate = format(subDays(new Date(), 30), "yyyy-MM-dd'T'HH:mm:ss");
  const endDate = format(new Date(), "yyyy-MM-dd'T'HH:mm:ss");

  const { data: accountReport, isLoading: accountLoading } = useQuery({
    queryKey: ['accountReport'],
    queryFn: reportApi.getAccountSummary,
  });

  const { data: cardReport, isLoading: cardLoading } = useQuery({
    queryKey: ['cardReport'],
    queryFn: reportApi.getCardStatus,
  });

  const { data: transactionReport, isLoading: transactionLoading } = useQuery({
    queryKey: ['transactionReport', startDate, endDate],
    queryFn: () => reportApi.getTransactionSummary(startDate, endDate),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Reports</h1>
        <p className="text-gray-500 mt-1">View system reports and analytics</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-blue-100 rounded-lg">
              <CreditCard className="w-5 h-5 text-blue-600" />
            </div>
            <h2 className="text-lg font-semibold text-gray-900">Account Summary Report</h2>
          </div>

          {accountLoading ? (
            <div className="animate-pulse space-y-3">
              {[1, 2, 3, 4].map(i => (
                <div key={i} className="h-6 bg-gray-200 rounded" />
              ))}
            </div>
          ) : accountReport ? (
            <div className="space-y-4">
              <ReportRow label="Report Date" value={accountReport.reportDate} />
              <ReportRow label="Total Accounts" value={accountReport.totalAccounts.toLocaleString()} />
              <ReportRow label="Active Accounts" value={accountReport.activeAccounts.toLocaleString()} />
              <ReportRow label="Inactive Accounts" value={accountReport.inactiveAccounts.toLocaleString()} />
              <ReportRow label="Total Balance" value={`$${accountReport.totalBalance.toLocaleString()}`} />
              <ReportRow label="Total Credit Limit" value={`$${accountReport.totalCreditLimit.toLocaleString()}`} />
              <ReportRow 
                label="Credit Utilization" 
                value={`${accountReport.creditUtilizationRate.toFixed(2)}%`} 
                highlight={accountReport.creditUtilizationRate > 80}
              />
              <ReportRow 
                label="Over Limit Accounts" 
                value={accountReport.overLimitAccountCount.toString()} 
                highlight={accountReport.overLimitAccountCount > 0}
              />
            </div>
          ) : null}
        </div>

        <div className="card">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-green-100 rounded-lg">
              <Activity className="w-5 h-5 text-green-600" />
            </div>
            <h2 className="text-lg font-semibold text-gray-900">Card Status Report</h2>
          </div>

          {cardLoading ? (
            <div className="animate-pulse space-y-3">
              {[1, 2, 3, 4].map(i => (
                <div key={i} className="h-6 bg-gray-200 rounded" />
              ))}
            </div>
          ) : cardReport ? (
            <div className="space-y-4">
              <ReportRow label="Report Date" value={cardReport.reportDate} />
              <ReportRow label="Total Cards" value={cardReport.totalCards.toLocaleString()} />
              <ReportRow label="Active Cards" value={cardReport.activeCards.toLocaleString()} />
              <ReportRow label="Inactive Cards" value={cardReport.inactiveCards.toLocaleString()} />
              <ReportRow 
                label="Expired Active Cards" 
                value={cardReport.expiredActiveCards.toString()} 
                highlight={cardReport.expiredActiveCards > 0}
              />
              <ReportRow 
                label="Expiring Within 30 Days" 
                value={cardReport.expiringWithin30Days.toString()} 
                highlight={cardReport.expiringWithin30Days > 0}
              />
            </div>
          ) : null}
        </div>

        <div className="card lg:col-span-2">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-purple-100 rounded-lg">
              <FileText className="w-5 h-5 text-purple-600" />
            </div>
            <h2 className="text-lg font-semibold text-gray-900">Transaction Summary Report (Last 30 Days)</h2>
          </div>

          {transactionLoading ? (
            <div className="animate-pulse space-y-3">
              {[1, 2, 3, 4].map(i => (
                <div key={i} className="h-6 bg-gray-200 rounded" />
              ))}
            </div>
          ) : transactionReport ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <ReportRow label="Report Date" value={transactionReport.reportDate} />
                <ReportRow label="Total Transactions" value={transactionReport.totalTransactions.toLocaleString()} />
                <ReportRow label="Total Amount" value={`$${transactionReport.totalAmount.toLocaleString()}`} />
                <ReportRow label="Total Credits" value={`$${transactionReport.totalCredits.toLocaleString()}`} />
                <ReportRow label="Total Debits" value={`$${transactionReport.totalDebits.toLocaleString()}`} />
                <ReportRow label="Average Transaction" value={`$${transactionReport.averageTransactionAmount.toLocaleString()}`} />
              </div>

              {transactionReport.transactionsByType && transactionReport.transactionsByType.length > 0 && (
                <div>
                  <h3 className="font-medium text-gray-900 mb-3">By Transaction Type</h3>
                  <div className="space-y-2">
                    {transactionReport.transactionsByType.map((type) => (
                      <div key={type.typeCode} className="flex justify-between items-center py-2 border-b border-gray-100">
                        <span className="text-gray-600">{type.typeCode}</span>
                        <div className="text-right">
                          <span className="font-medium">{type.count} txns</span>
                          <span className="text-gray-500 ml-2">(${type.totalAmount.toLocaleString()})</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

interface ReportRowProps {
  label: string;
  value: string;
  highlight?: boolean;
}

function ReportRow({ label, value, highlight }: ReportRowProps) {
  return (
    <div className="flex justify-between items-center py-2 border-b border-gray-100">
      <span className="text-gray-600">{label}</span>
      <span className={`font-medium ${highlight ? 'text-red-600' : 'text-gray-900'}`}>
        {value}
      </span>
    </div>
  );
}
