import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { accountApi } from '../services/api'

function AccountList() {
  const [accounts, setAccounts] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    loadAccounts()
  }, [page])

  const loadAccounts = async () => {
    try {
      const data = await accountApi.list(page, 10)
      setAccounts(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('Failed to load accounts:', err)
    }
  }

  const formatCurrency = (val) => {
    if (val == null) return '$0.00'
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)
  }

  return (
    <div className="card">
      <h3>Account List</h3>
      <table>
        <thead>
          <tr>
            <th>Account ID</th>
            <th>Status</th>
            <th>Current Balance</th>
            <th>Credit Limit</th>
            <th>Open Date</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map((acct) => (
            <tr key={acct.acctId}>
              <td>{acct.acctId}</td>
              <td><span className={acct.activeStatus === 'Y' ? 'status-active' : 'status-inactive'}>{acct.activeStatus === 'Y' ? 'Active' : 'Inactive'}</span></td>
              <td>{formatCurrency(acct.currBal)}</td>
              <td>{formatCurrency(acct.creditLimit)}</td>
              <td>{acct.openDate}</td>
              <td><Link to={`/accounts/${acct.acctId}`} className="btn btn-primary btn-sm">View</Link></td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="pagination">
        <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Previous</button>
        <span>Page {page + 1} of {totalPages}</span>
        <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next</button>
      </div>
    </div>
  )
}

export default AccountList
