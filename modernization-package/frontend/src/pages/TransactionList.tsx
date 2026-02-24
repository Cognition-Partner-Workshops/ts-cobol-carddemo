import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { listTransactions, TransactionListResponse, ErrorResponse } from '../services/api'

/**
 * Transaction List page (CT00) - replaces legacy COTRN00C.
 * 
 * Business Rules implemented:
 * - BR-LT-01: Page size fixed at 10
 * - BR-LT-02: Numeric filter validation
 * - BR-LT-03: Valid selection value ('S' only)
 * - BR-LT-04: Empty filter browses from start
 * - BR-LT-05: Forward pagination boundary
 * - BR-LT-06: Backward pagination boundary
 * - BR-LT-07: Page state preservation
 * - BR-LT-08: Selection triggers detail view
 * - BR-CF-03: Invalid key handling
 */
function TransactionList() {
  const navigate = useNavigate()
  const [data, setData] = useState<TransactionListResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [filterValue, setFilterValue] = useState('')
  const [activeFilter, setActiveFilter] = useState<string | undefined>(undefined)
  const [selections, setSelections] = useState<Record<number, string>>({})
  const [message, setMessage] = useState<{ text: string; type: 'info' | 'error' | 'warning' }>({ text: '', type: 'info' })
  const [loading, setLoading] = useState(false)

  const fetchData = useCallback(async (page: number, startId?: string) => {
    setLoading(true)
    setMessage({ text: '', type: 'info' })
    try {
      const result = await listTransactions(page, 10, startId)
      setData(result)
      setCurrentPage(result.page)
      setSelections({})
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || 'Error loading transactions', type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(0)
  }, [fetchData])

  // PF8 - Next Page (BR-LT-05)
  const handleNextPage = () => {
    if (!data) return
    if (data.last) {
      setMessage({ text: 'You are already at the bottom of the page...', type: 'warning' })
      return
    }
    fetchData(currentPage + 1, activeFilter)
  }

  // PF7 - Previous Page (BR-LT-06)
  const handlePrevPage = () => {
    if (!data) return
    if (data.first) {
      setMessage({ text: 'You are already at the top of the page...', type: 'warning' })
      return
    }
    fetchData(currentPage - 1, activeFilter)
  }

  // ENTER - Apply filter (BR-LT-02, BR-LT-04)
  const handleApplyFilter = () => {
    if (filterValue.trim() === '') {
      // BR-LT-04: Empty filter browses from start
      setActiveFilter(undefined)
      fetchData(0)
      return
    }
    // BR-LT-02: Numeric validation (client-side check; server also validates)
    if (!/^\d+$/.test(filterValue.trim())) {
      setMessage({ text: 'Tran ID must be Numeric ...', type: 'error' })
      return
    }
    setActiveFilter(filterValue.trim())
    fetchData(0, filterValue.trim())
  }

  // Handle selection input change
  const handleSelectionChange = (index: number, value: string) => {
    setSelections(prev => ({ ...prev, [index]: value.toUpperCase() }))
  }

  // Process selection (BR-LT-03, BR-LT-08)
  const handleProcessSelection = () => {
    if (!data) return
    setMessage({ text: '', type: 'info' })

    for (let i = 0; i < data.content.length; i++) {
      const sel = selections[i]
      if (sel && sel.trim() !== '') {
        if (sel.trim() === 'S') {
          // BR-LT-08: Selection triggers detail view
          const txId = data.content[i].transactionId
          navigate(`/transactions/view/${txId}`)
          return
        } else {
          // BR-LT-03: Invalid selection value
          setMessage({ text: 'Invalid selection. Valid value is S', type: 'error' })
          return
        }
      }
    }

    // No selection - refresh list (BR-LT-08 - ENTER refreshes)
    fetchData(currentPage, activeFilter)
  }

  // PF3 - Back to Menu (BR-CF-01)
  const handleBackToMenu = () => {
    navigate('/menu')
  }

  // Format date from timestamp
  const formatDate = (ts: string) => {
    if (!ts) return ''
    const d = new Date(ts)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const yy = String(d.getFullYear()).slice(-2)
    return `${mm}/${dd}/${yy}`
  }

  // Format amount
  const formatAmount = (amount: number) => {
    const sign = amount >= 0 ? '+' : ''
    return sign + amount.toFixed(2)
  }

  return (
    <div className="terminal-screen">
      <div className="terminal-header">
        Transaction List (CT00)
      </div>

      {/* Filter row */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
        <label style={{ color: '#00ccff' }}>Transaction ID Filter:</label>
        <input
          type="text"
          value={filterValue}
          onChange={(e) => setFilterValue(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') handleApplyFilter() }}
          style={{ width: '200px' }}
          placeholder="Enter Tran ID..."
        />
        <button onClick={handleApplyFilter}>Enter</button>
      </div>

      {/* Message area */}
      {message.text && (
        <div className={`message-${message.type}`}>
          {message.text}
        </div>
      )}

      {/* Transaction table */}
      {loading ? (
        <div className="message-info">Loading...</div>
      ) : data ? (
        <>
          <table>
            <thead>
              <tr>
                <th>Sel</th>
                <th>Transaction ID</th>
                <th>Date</th>
                <th>Card Number</th>
                <th>Type</th>
                <th>Cat</th>
                <th>Source</th>
                <th>Description</th>
                <th style={{ textAlign: 'right' }}>Amount</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((tx, idx) => (
                <tr key={tx.transactionId} className={selections[idx] === 'S' ? 'selected' : ''}>
                  <td>
                    <input
                      className="sel-input"
                      type="text"
                      maxLength={1}
                      value={selections[idx] || ''}
                      onChange={(e) => handleSelectionChange(idx, e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter') handleProcessSelection() }}
                    />
                  </td>
                  <td>{tx.transactionId}</td>
                  <td>{formatDate(tx.originationTimestamp)}</td>
                  <td>{tx.cardNumber}</td>
                  <td>{tx.typeCode}</td>
                  <td>{tx.categoryCode}</td>
                  <td>{tx.source}</td>
                  <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {tx.description}
                  </td>
                  <td style={{ textAlign: 'right' }}>{formatAmount(tx.amount)}</td>
                </tr>
              ))}
              {data.content.length === 0 && (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', color: '#666' }}>
                    No transactions found
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Page info (BR-LT-07) */}
          <div className="page-info">
            Page {data.page + 1} of {data.totalPages} | Total: {data.totalElements} transactions
          </div>
        </>
      ) : null}

      {/* Function key buttons */}
      <div className="terminal-footer">
        <button onClick={handleBackToMenu}>PF3 - Back to Menu</button>
        <button onClick={handlePrevPage} disabled={!data || data.first}>
          PF7 - Previous Page
        </button>
        <button onClick={handleNextPage} disabled={!data || data.last}>
          PF8 - Next Page
        </button>
        <button onClick={handleProcessSelection}>Enter - Process</button>
      </div>
    </div>
  )
}

export default TransactionList
