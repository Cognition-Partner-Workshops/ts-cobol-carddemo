import React, { useState } from 'react'
import { transactionApi } from '../services/api'

function TransactionReport() {
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')

  const handleGenerate = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const data = await transactionApi.report(startDate, endDate)
      setReport(data)
    } catch (err) {
      setError(err.message || 'Failed to generate report')
    }
  }

  const formatCurrency = (val) => val == null ? '$0.00' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)

  return (
    <div>
      <div className="card">
        <h3>Transaction Report</h3>
        <form onSubmit={handleGenerate} style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', marginBottom: '1rem' }}>
          <div className="form-group"><label>Start Date</label><input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required /></div>
          <div className="form-group"><label>End Date</label><input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} required /></div>
          <button type="submit" className="btn btn-primary" style={{ marginBottom: '1rem' }}>Generate</button>
        </form>
        {error && <p className="error-message">{error}</p>}
      </div>
      {report && (
        <div className="card">
          <h3>Report Results</h3>
          <div className="detail-grid" style={{ marginBottom: '1rem' }}>
            <div className="detail-item"><span className="label">Period</span><span className="value">{report.startDate} to {report.endDate}</span></div>
            <div className="detail-item"><span className="label">Total Transactions</span><span className="value">{report.totalTransactions}</span></div>
            <div className="detail-item"><span className="label">Total Amount</span><span className="value">{formatCurrency(report.totalAmount)}</span></div>
          </div>
          {report.accountSummaries && (
            <table>
              <thead><tr><th>Account ID</th><th>Transactions</th><th>Total Amount</th></tr></thead>
              <tbody>
                {Object.entries(report.accountSummaries).map(([acctId, summary]) => (
                  <tr key={acctId}><td>{acctId}</td><td>{summary.count}</td><td>{formatCurrency(summary.totalAmount)}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  )
}

export default TransactionReport
