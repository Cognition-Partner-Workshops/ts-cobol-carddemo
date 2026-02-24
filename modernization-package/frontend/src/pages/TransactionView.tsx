import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { viewTransaction, TransactionDetail, ErrorResponse } from '../services/api'

/**
 * Transaction View page (CT01) - replaces legacy COTRN01C.
 * 
 * Business Rules implemented:
 * - BR-VT-01: Transaction ID required
 * - BR-VT-02: Transaction must exist
 * - BR-VT-03: Pre-selected auto-load (13 fields displayed)
 * - BR-VT-04: Read-only display
 * - BR-VT-05: PF5 returns to list
 * - BR-CF-03: Invalid key handling
 */
function TransactionView() {
  const navigate = useNavigate()
  const { transactionId: paramTxId } = useParams<{ transactionId: string }>()
  
  const [txIdInput, setTxIdInput] = useState(paramTxId || '')
  const [transaction, setTransaction] = useState<TransactionDetail | null>(null)
  const [message, setMessage] = useState<{ text: string; type: 'info' | 'error' | 'success' }>({ text: '', type: 'info' })
  const [loading, setLoading] = useState(false)

  // BR-VT-03: Auto-load from list selection
  useEffect(() => {
    if (paramTxId) {
      setTxIdInput(paramTxId)
      fetchTransaction(paramTxId)
    }
  }, [paramTxId])

  const fetchTransaction = async (txId: string) => {
    if (!txId.trim()) {
      setMessage({ text: 'Tran ID can NOT be empty...', type: 'error' })
      return
    }
    
    setLoading(true)
    setMessage({ text: '', type: 'info' })
    try {
      const result = await viewTransaction(txId.trim())
      setTransaction(result)
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || 'Error loading transaction', type: 'error' })
      setTransaction(null)
    } finally {
      setLoading(false)
    }
  }

  // ENTER - Manual lookup (BR-VT-01, BR-VT-02)
  const handleLookup = () => {
    fetchTransaction(txIdInput)
  }

  // PF4 - Clear screen (US-VT-03)
  const handleClear = () => {
    setTxIdInput('')
    setTransaction(null)
    setMessage({ text: '', type: 'info' })
  }

  // PF5 - Return to list (BR-VT-05)
  const handleBackToList = () => {
    navigate('/transactions')
  }

  // PF3 - Back to menu
  const handleBackToMenu = () => {
    navigate('/menu')
  }

  // Format timestamp for display
  const formatTimestamp = (ts: string) => {
    if (!ts) return ''
    return ts.replace('T', ' ')
  }

  // Format amount as +99999999.99
  const formatAmount = (amount: number) => {
    const sign = amount >= 0 ? '+' : ''
    return sign + amount.toFixed(2)
  }

  return (
    <div className="terminal-screen">
      <div className="terminal-header">
        View Transaction (CT01) — Read Only
      </div>

      {/* Transaction ID input */}
      <div className="form-row">
        <label>Transaction ID:</label>
        <input
          type="text"
          value={txIdInput}
          onChange={(e) => setTxIdInput(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') handleLookup() }}
          maxLength={16}
          style={{ width: '200px' }}
          placeholder="Enter Transaction ID..."
        />
        <button onClick={handleLookup}>Enter - Lookup</button>
      </div>

      {/* Message area */}
      {message.text && (
        <div className={`message-${message.type}`} style={{ marginTop: '10px' }}>
          {message.text}
        </div>
      )}

      {loading && <div className="message-info" style={{ marginTop: '10px' }}>Loading...</div>}

      {/* Transaction detail - all 13 fields (BR-VT-03, BR-VT-04: read-only) */}
      {transaction && (
        <div style={{ marginTop: '15px', borderTop: '1px solid #003300', paddingTop: '10px' }}>
          <div className="form-row">
            <label>Transaction ID:</label>
            <span className="field-value">{transaction.transactionId}</span>
          </div>
          <div className="form-row">
            <label>Account ID:</label>
            <span className="field-value">{transaction.accountId || 'N/A'}</span>
          </div>
          <div className="form-row">
            <label>Card Number:</label>
            <span className="field-value">{transaction.cardNumber}</span>
          </div>
          <div className="form-row">
            <label>Type Code:</label>
            <span className="field-value">{transaction.typeCode}</span>
          </div>
          <div className="form-row">
            <label>Category Code:</label>
            <span className="field-value">{transaction.categoryCode}</span>
          </div>
          <div className="form-row">
            <label>Source:</label>
            <span className="field-value">{transaction.source}</span>
          </div>
          <div className="form-row">
            <label>Description:</label>
            <span className="field-value">{transaction.description}</span>
          </div>
          <div className="form-row">
            <label>Amount:</label>
            <span className="field-value">{formatAmount(transaction.amount)}</span>
          </div>
          <div className="form-row">
            <label>Origination Timestamp:</label>
            <span className="field-value">{formatTimestamp(transaction.originationTimestamp)}</span>
          </div>
          <div className="form-row">
            <label>Processing Timestamp:</label>
            <span className="field-value">{formatTimestamp(transaction.processingTimestamp)}</span>
          </div>
          <div className="form-row">
            <label>Merchant ID:</label>
            <span className="field-value">{transaction.merchantId}</span>
          </div>
          <div className="form-row">
            <label>Merchant Name:</label>
            <span className="field-value">{transaction.merchantName}</span>
          </div>
          <div className="form-row">
            <label>Merchant City:</label>
            <span className="field-value">{transaction.merchantCity}</span>
          </div>
          <div className="form-row">
            <label>Merchant Zip:</label>
            <span className="field-value">{transaction.merchantZip}</span>
          </div>
        </div>
      )}

      {/* Function key buttons */}
      <div className="terminal-footer">
        <button onClick={handleBackToMenu}>PF3 - Back to Menu</button>
        <button onClick={handleClear}>PF4 - Clear</button>
        <button onClick={handleBackToList}>PF5 - Back to List</button>
      </div>
    </div>
  )
}

export default TransactionView
