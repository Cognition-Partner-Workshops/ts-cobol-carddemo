import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { transactionApi } from '../services/api'

function TransactionAdd() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ cardNum: '', typeCd: '01', catCd: '1', amount: '', merchantName: '', merchantCity: '', merchantZip: '', description: '' })
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await transactionApi.add({ ...form, catCd: parseInt(form.catCd), amount: parseFloat(form.amount) })
      navigate('/transactions')
    } catch (err) {
      setError(err.message || 'Failed to add transaction')
    }
  }

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div>
      <div className="actions">
        <Link to="/transactions" className="btn btn-secondary btn-sm">Back to List</Link>
      </div>
      <div className="card">
        <h3>Add Transaction</h3>
        <form onSubmit={handleSubmit}>
          <div className="detail-grid">
            <div className="form-group"><label>Card Number</label><input type="text" value={form.cardNum} onChange={handleChange('cardNum')} required maxLength={16} /></div>
            <div className="form-group"><label>Type Code</label><select value={form.typeCd} onChange={handleChange('typeCd')}><option value="01">Purchase</option><option value="02">Payment</option><option value="03">Cash Advance</option><option value="04">Balance Transfer</option><option value="05">Fee</option></select></div>
            <div className="form-group"><label>Category Code</label><input type="number" value={form.catCd} onChange={handleChange('catCd')} required min={1} /></div>
            <div className="form-group"><label>Amount</label><input type="number" step="0.01" value={form.amount} onChange={handleChange('amount')} required /></div>
            <div className="form-group"><label>Merchant Name</label><input type="text" value={form.merchantName} onChange={handleChange('merchantName')} /></div>
            <div className="form-group"><label>Merchant City</label><input type="text" value={form.merchantCity} onChange={handleChange('merchantCity')} /></div>
            <div className="form-group"><label>Merchant ZIP</label><input type="text" value={form.merchantZip} onChange={handleChange('merchantZip')} /></div>
            <div className="form-group"><label>Description</label><input type="text" value={form.description} onChange={handleChange('description')} /></div>
          </div>
          {error && <p className="error-message">{error}</p>}
          <div className="modal-actions"><button type="submit" className="btn btn-primary">Add Transaction</button></div>
        </form>
      </div>
    </div>
  )
}

export default TransactionAdd
