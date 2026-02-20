import React, { useState, useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { transactionApi } from '../services/api'

function TransactionList() {
  const [transactions, setTransactions] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [searchParams] = useSearchParams()
  const cardNum = searchParams.get('cardNum')
  const acctId = searchParams.get('acctId')

  useEffect(() => { loadTransactions() }, [page, cardNum, acctId])

  const loadTransactions = async () => {
    try {
      const data = await transactionApi.list(page, 10, { cardNum, acctId })
      setTransactions(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('Failed to load transactions:', err)
    }
  }

  const formatCurrency = (val) => {
    if (val == null) return '$0.00'
    const formatted = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Math.abs(val))
    return val < 0 ? `-${formatted}` : formatted
  }

  return (
    <div className="card">
      <h3>Transaction List</h3>
      <div className="actions">
        <Link to="/transactions/add" className="btn btn-primary btn-sm">Add Transaction</Link>
      </div>
      <table>
        <thead>
          <tr>
            <th>Transaction ID</th>
            <th>Type</th>
            <th>Card Number</th>
            <th>Amount</th>
            <th>Merchant</th>
            <th>Date</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tran) => (
            <tr key={tran.tranId}>
              <td>{tran.tranId}</td>
              <td>{tran.typeCd}</td>
              <td>{tran.cardNum}</td>
              <td className={tran.amount < 0 ? 'amount-negative' : 'amount-positive'}>{formatCurrency(tran.amount)}</td>
              <td>{tran.merchantName}</td>
              <td>{tran.origTs ? new Date(tran.origTs).toLocaleDateString() : ''}</td>
              <td><Link to={`/transactions/${tran.tranId}`} className="btn btn-primary btn-sm">View</Link></td>
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

export default TransactionList
