import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { cardApi } from '../services/api'

function CardView() {
  const { cardNum } = useParams()
  const [card, setCard] = useState(null)
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState({})

  useEffect(() => { loadCard() }, [cardNum])

  const loadCard = async () => {
    try {
      const data = await cardApi.get(cardNum)
      setCard(data)
      setForm({ embossedName: data.embossedName || '', activeStatus: data.activeStatus || 'Y' })
    } catch (err) {
      console.error('Failed to load card:', err)
    }
  }

  const handleSave = async () => {
    try {
      await cardApi.update(cardNum, form)
      setEditing(false)
      loadCard()
    } catch (err) {
      console.error('Failed to update card:', err)
    }
  }

  if (!card) return <div className="card"><h3>Loading...</h3></div>

  return (
    <div>
      <div className="actions">
        <Link to="/cards" className="btn btn-secondary btn-sm">Back to List</Link>
        {!editing && <button className="btn btn-primary btn-sm" onClick={() => setEditing(true)}>Edit</button>}
      </div>
      <div className="card">
        <h3>Card Details</h3>
        <div className="detail-grid">
          <div className="detail-item"><span className="label">Card Number</span><span className="value">{card.cardNum}</span></div>
          <div className="detail-item"><span className="label">Account ID</span><span className="value"><Link to={`/accounts/${card.acctId}`}>{card.acctId}</Link></span></div>
          <div className="detail-item"><span className="label">CVV</span><span className="value">{card.cvvCd}</span></div>
          <div className="detail-item"><span className="label">Embossed Name</span>{editing ? <input type="text" value={form.embossedName} onChange={(e) => setForm({...form, embossedName: e.target.value})} /> : <span className="value">{card.embossedName}</span>}</div>
          <div className="detail-item"><span className="label">Status</span>{editing ? <select value={form.activeStatus} onChange={(e) => setForm({...form, activeStatus: e.target.value})}><option value="Y">Active</option><option value="N">Inactive</option></select> : <span className={`value ${card.activeStatus === 'Y' ? 'status-active' : 'status-inactive'}`}>{card.activeStatus === 'Y' ? 'Active' : 'Inactive'}</span>}</div>
          <div className="detail-item"><span className="label">Expiration Date</span><span className="value">{card.expirationDate}</span></div>
        </div>
        {editing && <div className="modal-actions"><button className="btn btn-secondary" onClick={() => setEditing(false)}>Cancel</button><button className="btn btn-primary" onClick={handleSave}>Save</button></div>}
      </div>
    </div>
  )
}

export default CardView
