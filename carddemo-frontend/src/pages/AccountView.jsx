import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { accountApi } from '../services/api'

function AccountView() {
  const { id } = useParams()
  const [data, setData] = useState(null)
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState({})

  useEffect(() => { loadAccount() }, [id])

  const loadAccount = async () => {
    try {
      const result = await accountApi.get(id)
      setData(result)
      if (result.account) setForm({ creditLimit: result.account.creditLimit, cashCreditLimit: result.account.cashCreditLimit, groupId: result.account.groupId || '' })
    } catch (err) {
      console.error('Failed to load account:', err)
    }
  }

  const handleSave = async () => {
    try {
      await accountApi.update(id, form)
      setEditing(false)
      loadAccount()
    } catch (err) {
      console.error('Failed to update account:', err)
    }
  }

  const formatCurrency = (val) => val == null ? '$0.00' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val)

  if (!data) return <div className="card"><h3>Loading...</h3></div>

  const { account, customer, cardReferences } = data

  return (
    <div>
      <div className="actions">
        <Link to="/accounts" className="btn btn-secondary btn-sm">Back to List</Link>
        {!editing && <button className="btn btn-primary btn-sm" onClick={() => setEditing(true)}>Edit</button>}
      </div>
      <div className="card">
        <h3>Account #{account.acctId}</h3>
        <div className="detail-grid">
          <div className="detail-item"><span className="label">Status</span><span className={`value ${account.activeStatus === 'Y' ? 'status-active' : 'status-inactive'}`}>{account.activeStatus === 'Y' ? 'Active' : 'Inactive'}</span></div>
          <div className="detail-item"><span className="label">Current Balance</span><span className="value">{formatCurrency(account.currBal)}</span></div>
          <div className="detail-item"><span className="label">Credit Limit</span>{editing ? <input type="number" value={form.creditLimit} onChange={(e) => setForm({...form, creditLimit: e.target.value})} /> : <span className="value">{formatCurrency(account.creditLimit)}</span>}</div>
          <div className="detail-item"><span className="label">Cash Credit Limit</span>{editing ? <input type="number" value={form.cashCreditLimit} onChange={(e) => setForm({...form, cashCreditLimit: e.target.value})} /> : <span className="value">{formatCurrency(account.cashCreditLimit)}</span>}</div>
          <div className="detail-item"><span className="label">Open Date</span><span className="value">{account.openDate}</span></div>
          <div className="detail-item"><span className="label">Expiration Date</span><span className="value">{account.expirationDate}</span></div>
          <div className="detail-item"><span className="label">Current Cycle Credit</span><span className="value">{formatCurrency(account.currCycCredit)}</span></div>
          <div className="detail-item"><span className="label">Current Cycle Debit</span><span className="value">{formatCurrency(account.currCycDebit)}</span></div>
          <div className="detail-item"><span className="label">Group ID</span>{editing ? <input type="text" value={form.groupId} onChange={(e) => setForm({...form, groupId: e.target.value})} /> : <span className="value">{account.groupId}</span>}</div>
        </div>
        {editing && <div className="modal-actions"><button className="btn btn-secondary" onClick={() => setEditing(false)}>Cancel</button><button className="btn btn-primary" onClick={handleSave}>Save</button></div>}
      </div>
      {customer && (
        <div className="card">
          <h3>Customer Information</h3>
          <div className="detail-grid">
            <div className="detail-item"><span className="label">Name</span><span className="value">{customer.firstName} {customer.middleName} {customer.lastName}</span></div>
            <div className="detail-item"><span className="label">Address</span><span className="value">{customer.addrLine1}</span></div>
            <div className="detail-item"><span className="label">City/State</span><span className="value">{customer.addrLine3}, {customer.addrStateCd} {customer.addrZip}</span></div>
            <div className="detail-item"><span className="label">Phone</span><span className="value">{customer.phoneNum1}</span></div>
            <div className="detail-item"><span className="label">FICO Score</span><span className="value">{customer.ficoCreditScore}</span></div>
          </div>
        </div>
      )}
      {cardReferences && cardReferences.length > 0 && (
        <div className="card">
          <h3>Associated Cards</h3>
          <table>
            <thead><tr><th>Card Number</th><th>Customer ID</th><th>Actions</th></tr></thead>
            <tbody>
              {cardReferences.map((xref) => (
                <tr key={xref.cardNum}><td>{xref.cardNum}</td><td>{xref.custId}</td><td><Link to={`/cards/${xref.cardNum}`} className="btn btn-primary btn-sm">View</Link></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default AccountView
