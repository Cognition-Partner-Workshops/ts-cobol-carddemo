import React, { useState } from 'react'
import { billPaymentApi } from '../services/api'

function BillPayment() {
  const [acctId, setAcctId] = useState('')
  const [amount, setAmount] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setResult(null)
    try {
      const data = await billPaymentApi.pay(parseInt(acctId), parseFloat(amount))
      setResult(data)
      setAcctId('')
      setAmount('')
    } catch (err) {
      setError(err.message || 'Payment failed')
    }
  }

  const formatCurrency = (val) => val == null ? '$0.00' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)

  return (
    <div>
      <div className="card">
        <h3>Bill Payment</h3>
        <form onSubmit={handleSubmit}>
          <div className="detail-grid">
            <div className="form-group"><label>Account ID</label><input type="number" value={acctId} onChange={(e) => setAcctId(e.target.value)} required /></div>
            <div className="form-group"><label>Payment Amount</label><input type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required min="0.01" /></div>
          </div>
          {error && <p className="error-message">{error}</p>}
          <div className="modal-actions"><button type="submit" className="btn btn-primary">Submit Payment</button></div>
        </form>
      </div>
      {result && (
        <div className="card">
          <h3>Payment Confirmation</h3>
          <div className="detail-grid">
            <div className="detail-item"><span className="label">Transaction ID</span><span className="value">{result.tranId}</span></div>
            <div className="detail-item"><span className="label">Amount</span><span className="value amount-negative">{formatCurrency(result.amount)}</span></div>
            <div className="detail-item"><span className="label">Card Number</span><span className="value">{result.cardNum}</span></div>
            <div className="detail-item"><span className="label">Date</span><span className="value">{result.origTs ? new Date(result.origTs).toLocaleString() : ''}</span></div>
          </div>
        </div>
      )}
    </div>
  )
}

export default BillPayment
