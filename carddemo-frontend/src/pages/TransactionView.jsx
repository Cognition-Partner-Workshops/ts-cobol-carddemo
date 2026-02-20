import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { transactionApi } from '../services/api'

function TransactionView() {
  const { id } = useParams()
  const [tran, setTran] = useState(null)

  useEffect(() => { loadTransaction() }, [id])

  const loadTransaction = async () => {
    try {
      const data = await transactionApi.get(id)
      setTran(data)
    } catch (err) {
      console.error('Failed to load transaction:', err)
    }
  }

  const formatCurrency = (val) => val == null ? '$0.00' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)

  if (!tran) return <div className="card"><h3>Loading...</h3></div>

  return (
    <div>
      <div className="actions">
        <Link to="/transactions" className="btn btn-secondary btn-sm">Back to List</Link>
      </div>
      <div className="card">
        <h3>Transaction Details</h3>
        <div className="detail-grid">
          <div className="detail-item"><span className="label">Transaction ID</span><span className="value">{tran.tranId}</span></div>
          <div className="detail-item"><span className="label">Type Code</span><span className="value">{tran.typeCd}</span></div>
          <div className="detail-item"><span className="label">Category Code</span><span className="value">{tran.catCd}</span></div>
          <div className="detail-item"><span className="label">Source</span><span className="value">{tran.source}</span></div>
          <div className="detail-item"><span className="label">Description</span><span className="value">{tran.description}</span></div>
          <div className="detail-item"><span className="label">Amount</span><span className={`value ${tran.amount < 0 ? 'amount-negative' : 'amount-positive'}`}>{formatCurrency(tran.amount)}</span></div>
          <div className="detail-item"><span className="label">Card Number</span><span className="value">{tran.cardNum}</span></div>
          <div className="detail-item"><span className="label">Merchant</span><span className="value">{tran.merchantName}</span></div>
          <div className="detail-item"><span className="label">Merchant City</span><span className="value">{tran.merchantCity}</span></div>
          <div className="detail-item"><span className="label">Merchant ZIP</span><span className="value">{tran.merchantZip}</span></div>
          <div className="detail-item"><span className="label">Original Timestamp</span><span className="value">{tran.origTs ? new Date(tran.origTs).toLocaleString() : ''}</span></div>
          <div className="detail-item"><span className="label">Processed Timestamp</span><span className="value">{tran.procTs ? new Date(tran.procTs).toLocaleString() : ''}</span></div>
        </div>
      </div>
    </div>
  )
}

export default TransactionView
