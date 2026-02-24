import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  addTransaction,
  getLatestTransaction,
  AddTransactionRequest,
  AddTransactionResponse,
  ConfirmationRequiredResponse,
  ErrorResponse,
} from '../services/api'

/**
 * Transaction Add page (CT02) - replaces legacy COTRN02C.
 *
 * Business Rules implemented:
 * - BR-AT-01: Account or Card Required
 * - BR-AT-02: Account ID Numeric
 * - BR-AT-03: Card Number Numeric
 * - BR-AT-04: Account/Card Must Exist
 * - BR-AT-05: Cross-Reference Auto-Resolution
 * - BR-AT-06: All Fields Mandatory (11 remaining fields)
 * - BR-AT-07: Type/Category Numeric
 * - BR-AT-08: Amount Format (-99999999.99)
 * - BR-AT-09: Date Format (YYYY-MM-DD)
 * - BR-AT-10: Date Calendar Validity
 * - BR-AT-11: Merchant ID Numeric
 * - BR-AT-12: Y/N Confirmation Gate
 * - BR-AT-13: Auto-Generated Transaction ID
 * - BR-AT-14: Duplicate ID Rejection
 * - BR-CF-01: PF3 Back to Menu
 * - BR-CF-02: PF4 Clear Form
 */

interface FormData {
  accountId: string
  cardNumber: string
  typeCode: string
  categoryCode: string
  source: string
  description: string
  amount: string
  originationDate: string
  processingDate: string
  merchantId: string
  merchantName: string
  merchantCity: string
  merchantZip: string
}

const EMPTY_FORM: FormData = {
  accountId: '',
  cardNumber: '',
  typeCode: '',
  categoryCode: '',
  source: '',
  description: '',
  amount: '',
  originationDate: '',
  processingDate: '',
  merchantId: '',
  merchantName: '',
  merchantCity: '',
  merchantZip: '',
}

function TransactionAdd() {
  const navigate = useNavigate()
  const [form, setForm] = useState<FormData>({ ...EMPTY_FORM })
  const [message, setMessage] = useState<{ text: string; type: 'info' | 'error' | 'success' | 'warning' }>({
    text: '',
    type: 'info',
  })
  const [errorField, setErrorField] = useState<string | null>(null)
  const [showConfirm, setShowConfirm] = useState(false)
  const [resolvedAccountId, setResolvedAccountId] = useState<string | null>(null)
  const [resolvedCardNumber, setResolvedCardNumber] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [successTxId, setSuccessTxId] = useState<string | null>(null)

  const handleChange = (field: keyof FormData, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }))
    if (errorField === field) {
      setErrorField(null)
      setMessage({ text: '', type: 'info' })
    }
    // Clear success state on any edit
    if (successTxId) {
      setSuccessTxId(null)
    }
  }

  // Submit form (first without confirmation, then with Y)
  const handleSubmit = async (confirmation: string | null) => {
    setLoading(true)
    setMessage({ text: '', type: 'info' })
    setErrorField(null)

    const request: AddTransactionRequest = {
      accountId: form.accountId.trim() || null,
      cardNumber: form.cardNumber.trim() || null,
      typeCode: form.typeCode.trim(),
      categoryCode: form.categoryCode.trim(),
      source: form.source.trim(),
      description: form.description.trim(),
      amount: form.amount.trim(),
      originationDate: form.originationDate.trim(),
      processingDate: form.processingDate.trim(),
      merchantId: form.merchantId.trim(),
      merchantName: form.merchantName.trim(),
      merchantCity: form.merchantCity.trim(),
      merchantZip: form.merchantZip.trim(),
      confirmation: confirmation,
    }

    try {
      const result = await addTransaction(request)

      if ('confirmationRequired' in result) {
        // BR-AT-12: Confirmation required
        const confirmResult = result as ConfirmationRequiredResponse
        setResolvedAccountId(confirmResult.resolvedAccountId)
        setResolvedCardNumber(confirmResult.resolvedCardNumber)
        setMessage({ text: confirmResult.message, type: 'warning' })
        setShowConfirm(true)
      } else {
        // BR-AT-13: Success - show auto-generated ID
        const addResult = result as AddTransactionResponse
        setSuccessTxId(addResult.transactionId)
        setMessage({ text: addResult.message, type: 'success' })
        setShowConfirm(false)
        setResolvedAccountId(null)
        setResolvedCardNumber(null)
      }
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || 'Validation error', type: 'error' })
      setErrorField(error.field || null)
      setShowConfirm(false)
    } finally {
      setLoading(false)
    }
  }

  // Initial submit (no confirmation)
  const handleEnter = () => {
    if (showConfirm) {
      // Already showing confirmation - confirm with Y
      handleSubmit('Y')
      return
    }
    handleSubmit(null)
  }

  // Confirm Yes
  const handleConfirmYes = () => {
    setShowConfirm(false)
    handleSubmit('Y')
  }

  // Confirm No
  const handleConfirmNo = () => {
    setShowConfirm(false)
    setMessage({ text: 'Transaction cancelled.', type: 'info' })
  }

  // PF4 - Clear form (BR-CF-02)
  const handleClear = () => {
    setForm({ ...EMPTY_FORM })
    setMessage({ text: '', type: 'info' })
    setErrorField(null)
    setShowConfirm(false)
    setResolvedAccountId(null)
    setResolvedCardNumber(null)
    setSuccessTxId(null)
  }

  // PF5 - Copy Last Transaction (US-AT-06)
  const handleCopyLast = async () => {
    setLoading(true)
    setMessage({ text: '', type: 'info' })
    try {
      const latest = await getLatestTransaction()
      setForm(prev => ({
        ...prev,
        // Key fields (accountId, cardNumber) are NOT overwritten per legacy behavior
        typeCode: latest.typeCode || '',
        categoryCode: String(latest.categoryCode),
        source: latest.source || '',
        description: latest.description || '',
        amount: latest.amount.toFixed(2),
        originationDate: latest.originationDate || '',
        processingDate: latest.processingDate || '',
        merchantId: String(latest.merchantId),
        merchantName: latest.merchantName || '',
        merchantCity: latest.merchantCity || '',
        merchantZip: latest.merchantZip || '',
      }))
      setMessage({ text: 'Copied data from last transaction (ID: ' + latest.transactionId + ')', type: 'info' })
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || 'No transactions found to copy', type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  // PF3 - Back to menu (BR-CF-01)
  const handleBackToMenu = () => {
    navigate('/menu')
  }

  const inputClass = (field: string) => (errorField === field ? 'error-field' : '')

  return (
    <div className="terminal-screen">
      <div className="terminal-header">
        Add Transaction (CT02)
      </div>

      {/* Message area */}
      {message.text && (
        <div className={`message-${message.type}`} style={{ marginBottom: '10px' }}>
          {message.text}
        </div>
      )}

      {/* Success display */}
      {successTxId && (
        <div className="message-success" style={{ marginBottom: '10px' }}>
          New Transaction ID: {successTxId}
        </div>
      )}

      {/* Resolved cross-reference display */}
      {resolvedAccountId && (
        <div style={{ marginBottom: '5px', color: '#00ccff', fontSize: '13px' }}>
          Resolved Account ID: {resolvedAccountId} | Card Number: {resolvedCardNumber}
        </div>
      )}

      {/* Form fields - all 13 input fields */}
      <div style={{ marginTop: '10px' }}>
        {/* Key fields (Phase 1) */}
        <div className="form-row">
          <label>Account ID:</label>
          <input
            type="text"
            value={form.accountId}
            onChange={(e) => handleChange('accountId', e.target.value)}
            maxLength={11}
            className={inputClass('accountId')}
            placeholder="Enter Account ID or Card Number"
          />
        </div>
        <div className="form-row">
          <label>Card Number:</label>
          <input
            type="text"
            value={form.cardNumber}
            onChange={(e) => handleChange('cardNumber', e.target.value)}
            maxLength={16}
            className={inputClass('cardNumber')}
            placeholder="Enter Card Number or Account ID"
          />
        </div>

        {/* Data fields (Phase 2-6) */}
        <div className="form-row">
          <label>Type Code:</label>
          <input
            type="text"
            value={form.typeCode}
            onChange={(e) => handleChange('typeCode', e.target.value)}
            maxLength={2}
            className={inputClass('typeCode')}
          />
        </div>
        <div className="form-row">
          <label>Category Code:</label>
          <input
            type="text"
            value={form.categoryCode}
            onChange={(e) => handleChange('categoryCode', e.target.value)}
            maxLength={4}
            className={inputClass('categoryCode')}
          />
        </div>
        <div className="form-row">
          <label>Source:</label>
          <input
            type="text"
            value={form.source}
            onChange={(e) => handleChange('source', e.target.value)}
            maxLength={10}
            className={inputClass('source')}
          />
        </div>
        <div className="form-row">
          <label>Description:</label>
          <input
            type="text"
            value={form.description}
            onChange={(e) => handleChange('description', e.target.value)}
            maxLength={100}
            className={inputClass('description')}
            style={{ maxWidth: '400px' }}
          />
        </div>
        <div className="form-row">
          <label>Amount:</label>
          <input
            type="text"
            value={form.amount}
            onChange={(e) => handleChange('amount', e.target.value)}
            maxLength={12}
            className={inputClass('amount')}
            placeholder="-99999999.99"
          />
        </div>
        <div className="form-row">
          <label>Origination Date:</label>
          <input
            type="text"
            value={form.originationDate}
            onChange={(e) => handleChange('originationDate', e.target.value)}
            maxLength={10}
            className={inputClass('originationDate')}
            placeholder="YYYY-MM-DD"
          />
        </div>
        <div className="form-row">
          <label>Processing Date:</label>
          <input
            type="text"
            value={form.processingDate}
            onChange={(e) => handleChange('processingDate', e.target.value)}
            maxLength={10}
            className={inputClass('processingDate')}
            placeholder="YYYY-MM-DD"
          />
        </div>
        <div className="form-row">
          <label>Merchant ID:</label>
          <input
            type="text"
            value={form.merchantId}
            onChange={(e) => handleChange('merchantId', e.target.value)}
            maxLength={9}
            className={inputClass('merchantId')}
          />
        </div>
        <div className="form-row">
          <label>Merchant Name:</label>
          <input
            type="text"
            value={form.merchantName}
            onChange={(e) => handleChange('merchantName', e.target.value)}
            maxLength={50}
            className={inputClass('merchantName')}
          />
        </div>
        <div className="form-row">
          <label>Merchant City:</label>
          <input
            type="text"
            value={form.merchantCity}
            onChange={(e) => handleChange('merchantCity', e.target.value)}
            maxLength={30}
            className={inputClass('merchantCity')}
          />
        </div>
        <div className="form-row">
          <label>Merchant Zip:</label>
          <input
            type="text"
            value={form.merchantZip}
            onChange={(e) => handleChange('merchantZip', e.target.value)}
            maxLength={10}
            className={inputClass('merchantZip')}
          />
        </div>
      </div>

      {/* Confirmation dialog (BR-AT-12) */}
      {showConfirm && (
        <div className="confirm-overlay">
          <div className="confirm-dialog">
            <div className="title">Confirm Transaction Addition</div>
            <p style={{ marginBottom: '10px' }}>
              Account ID: {resolvedAccountId}
            </p>
            <p style={{ marginBottom: '10px' }}>
              Card Number: {resolvedCardNumber}
            </p>
            <p>Are you sure you want to add this transaction? (Y/N)</p>
            <div className="actions">
              <button onClick={handleConfirmYes}>Y - Yes, Add</button>
              <button onClick={handleConfirmNo}>N - No, Cancel</button>
            </div>
          </div>
        </div>
      )}

      {/* Function key buttons */}
      <div className="terminal-footer">
        <button onClick={handleBackToMenu}>PF3 - Back to Menu</button>
        <button onClick={handleClear}>PF4 - Clear</button>
        <button onClick={handleCopyLast}>PF5 - Copy Last</button>
        <button onClick={handleEnter} disabled={loading}>
          {loading ? 'Processing...' : 'Enter - Submit'}
        </button>
      </div>
    </div>
  )
}

export default TransactionAdd
