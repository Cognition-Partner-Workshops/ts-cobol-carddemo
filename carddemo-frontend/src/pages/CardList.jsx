import React, { useState, useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { cardApi } from '../services/api'

function CardList() {
  const [cards, setCards] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [searchParams] = useSearchParams()
  const acctId = searchParams.get('acctId')

  useEffect(() => { loadCards() }, [page, acctId])

  const loadCards = async () => {
    try {
      const data = await cardApi.list(page, 10, acctId)
      setCards(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('Failed to load cards:', err)
    }
  }

  return (
    <div className="card">
      <h3>Credit Card List {acctId && `(Account: ${acctId})`}</h3>
      <table>
        <thead>
          <tr>
            <th>Card Number</th>
            <th>Account ID</th>
            <th>Embossed Name</th>
            <th>Status</th>
            <th>Expiration</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {cards.map((card) => (
            <tr key={card.cardNum}>
              <td>{card.cardNum}</td>
              <td><Link to={`/accounts/${card.acctId}`}>{card.acctId}</Link></td>
              <td>{card.embossedName}</td>
              <td><span className={card.activeStatus === 'Y' ? 'status-active' : 'status-inactive'}>{card.activeStatus === 'Y' ? 'Active' : 'Inactive'}</span></td>
              <td>{card.expirationDate}</td>
              <td><Link to={`/cards/${card.cardNum}`} className="btn btn-primary btn-sm">View</Link></td>
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

export default CardList
