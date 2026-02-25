import { useState } from "react"
import { useNavigate } from "react-router-dom"
import {
  addTransaction,
  getLatestTransaction,
  AddTransactionRequest,
  ConfirmationRequiredResponse,
  AddTransactionResponse,
  ErrorResponse,
} from "@/services/api"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import {
  Eraser,
  Copy,
  Send,
  AlertCircle,
  CheckCircle2,
  Loader2,
  CreditCard,
  Building,
  Calendar,
  DollarSign,
  FileText,
} from "lucide-react"

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

const INITIAL_FORM: FormData = {
  accountId: "",
  cardNumber: "",
  typeCode: "",
  categoryCode: "",
  source: "",
  description: "",
  amount: "",
  originationDate: "",
  processingDate: "",
  merchantId: "",
  merchantName: "",
  merchantCity: "",
  merchantZip: "",
}

export default function TransactionAdd() {
  const navigate = useNavigate()
  const [form, setForm] = useState<FormData>({ ...INITIAL_FORM })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [message, setMessage] = useState<{ text: string; type: "error" | "success" | "info" } | null>(null)
  const [loading, setLoading] = useState(false)
  const [copyLoading, setCopyLoading] = useState(false)

  // Confirmation dialog state
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmData, setConfirmData] = useState<ConfirmationRequiredResponse | null>(null)

  // Success state
  const [successData, setSuccessData] = useState<AddTransactionResponse | null>(null)

  const updateField = (field: keyof FormData, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
    // Clear field error on change
    if (fieldErrors[field]) {
      setFieldErrors((prev) => {
        const next = { ...prev }
        delete next[field]
        return next
      })
    }
  }

  // Real-time validation
  const validateField = (field: keyof FormData, value: string): string | null => {
    switch (field) {
      case "accountId":
        if (value && !/^\d{1,11}$/.test(value)) return "Account ID must be numeric (up to 11 digits)"
        break
      case "cardNumber":
        if (value && !/^\d{16}$/.test(value)) return "Card Number must be exactly 16 digits"
        break
      case "typeCode":
        if (!value.trim()) return "Type Code is required"
        if (value.length !== 2) return "Type Code must be exactly 2 characters"
        break
      case "categoryCode":
        if (!value.trim()) return "Category Code is required"
        if (!/^\d{4}$/.test(value)) return "Category Code must be exactly 4 digits"
        break
      case "source":
        if (value.length > 10) return "Source must be at most 10 characters"
        break
      case "description":
        if (value.length > 100) return "Description must be at most 100 characters"
        break
      case "amount":
        if (!value.trim()) return "Amount is required"
        if (isNaN(Number(value))) return "Amount must be a valid number"
        break
      case "originationDate":
        if (value && !/^\d{4}-\d{2}-\d{2}$/.test(value)) return "Date format: YYYY-MM-DD"
        break
      case "processingDate":
        if (value && !/^\d{4}-\d{2}-\d{2}$/.test(value)) return "Date format: YYYY-MM-DD"
        break
      case "merchantId":
        if (value && !/^\d+$/.test(value)) return "Merchant ID must be numeric"
        break
      case "merchantName":
        if (value.length > 50) return "Merchant Name must be at most 50 characters"
        break
      case "merchantCity":
        if (value.length > 50) return "Merchant City must be at most 50 characters"
        break
      case "merchantZip":
        if (value && !/^\d{5}(-\d{4})?$/.test(value)) return "ZIP must be 5 or 9 digits (e.g., 12345 or 12345-6789)"
        break
    }
    return null
  }

  const handleBlur = (field: keyof FormData) => {
    const error = validateField(field, form[field])
    if (error) {
      setFieldErrors((prev) => ({ ...prev, [field]: error }))
    } else {
      setFieldErrors((prev) => {
        const next = { ...prev }
        delete next[field]
        return next
      })
    }
  }

  const handleClear = () => {
    setForm({ ...INITIAL_FORM })
    setFieldErrors({})
    setMessage(null)
    setSuccessData(null)
  }

  const handleCopyLast = async () => {
    setCopyLoading(true)
    setMessage(null)
    try {
      const latest = await getLatestTransaction()
      setForm({
        accountId: "",
        cardNumber: "",
        typeCode: latest.typeCode || "",
        categoryCode: String(latest.categoryCode) || "",
        source: latest.source || "",
        description: latest.description || "",
        amount: String(latest.amount) || "",
        originationDate: latest.originationDate || "",
        processingDate: latest.processingDate || "",
        merchantId: String(latest.merchantId) || "",
        merchantName: latest.merchantName || "",
        merchantCity: latest.merchantCity || "",
        merchantZip: latest.merchantZip || "",
      })
      setMessage({ text: "Last transaction data copied to form", type: "info" })
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || "No previous transaction to copy", type: "error" })
    } finally {
      setCopyLoading(false)
    }
  }

  const handleSubmit = async () => {
    // Validate all fields first
    const errors: Record<string, string> = {}
    for (const [key, value] of Object.entries(form)) {
      const error = validateField(key as keyof FormData, value)
      if (error) errors[key] = error
    }

    // Account ID or Card Number required
    if (!form.accountId && !form.cardNumber) {
      errors.accountId = "Either Account ID or Card Number is required"
      errors.cardNumber = "Either Account ID or Card Number is required"
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      setMessage({ text: "Please fix the validation errors above", type: "error" })
      return
    }

    setLoading(true)
    setMessage(null)

    try {
      const request: AddTransactionRequest = {
        accountId: form.accountId || null,
        cardNumber: form.cardNumber || null,
        typeCode: form.typeCode,
        categoryCode: form.categoryCode,
        source: form.source,
        description: form.description,
        amount: form.amount,
        originationDate: form.originationDate,
        processingDate: form.processingDate,
        merchantId: form.merchantId,
        merchantName: form.merchantName,
        merchantCity: form.merchantCity,
        merchantZip: form.merchantZip,
        confirmation: null,
      }

      const response = await addTransaction(request)

      if ("confirmationRequired" in response) {
        setConfirmData(response as ConfirmationRequiredResponse)
        setConfirmOpen(true)
      } else {
        setSuccessData(response as AddTransactionResponse)
        setMessage({ text: `Transaction created successfully! ID: ${(response as AddTransactionResponse).transactionId}`, type: "success" })
      }
    } catch (err) {
      const error = err as ErrorResponse
      if (error.field) {
        setFieldErrors({ [error.field]: error.message })
      }
      setMessage({ text: error.message || "Error adding transaction", type: "error" })
    } finally {
      setLoading(false)
    }
  }

  const handleConfirm = async () => {
    setConfirmOpen(false)
    setLoading(true)

    try {
      const request: AddTransactionRequest = {
        accountId: form.accountId || null,
        cardNumber: form.cardNumber || null,
        typeCode: form.typeCode,
        categoryCode: form.categoryCode,
        source: form.source,
        description: form.description,
        amount: form.amount,
        originationDate: form.originationDate,
        processingDate: form.processingDate,
        merchantId: form.merchantId,
        merchantName: form.merchantName,
        merchantCity: form.merchantCity,
        merchantZip: form.merchantZip,
        confirmation: "Y",
      }

      const response = await addTransaction(request)

      if ("transactionId" in response && "transaction" in response) {
        setSuccessData(response as AddTransactionResponse)
        setMessage({ text: `Transaction created successfully! ID: ${(response as AddTransactionResponse).transactionId}`, type: "success" })
      }
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || "Error confirming transaction", type: "error" })
    } finally {
      setLoading(false)
    }
  }

  const renderField = (
    field: keyof FormData,
    label: string,
    placeholder: string,
    options?: { type?: string; required?: boolean }
  ) => {
    const error = fieldErrors[field]
    return (
      <div className="space-y-2">
        <Label htmlFor={field} className="text-sm">
          {label}
          {options?.required && <span className="text-red-500 ml-1">*</span>}
        </Label>
        <Input
          id={field}
          type={options?.type || "text"}
          placeholder={placeholder}
          value={form[field]}
          onChange={(e) => updateField(field, e.target.value)}
          onBlur={() => handleBlur(field)}
          className={error ? "border-red-300 focus-visible:ring-red-400" : ""}
        />
        {error && (
          <p className="text-xs text-red-600 flex items-center gap-1">
            <AlertCircle className="h-3 w-3" />
            {error}
          </p>
        )}
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header actions */}
      <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">New Transaction</h2>
          <p className="text-sm text-muted-foreground">Fill in the details below to add a new transaction</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={handleClear}>
            <Eraser className="h-4 w-4 mr-2" />
            Clear
          </Button>
          <Button variant="outline" size="sm" onClick={handleCopyLast} disabled={copyLoading}>
            {copyLoading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Copy className="h-4 w-4 mr-2" />}
            Copy Last
          </Button>
        </div>
      </div>

      {/* Alert messages */}
      {message && (
        <div className={`flex items-center gap-2 rounded-lg border px-4 py-3 text-sm ${
          message.type === "error" ? "border-red-200 bg-red-50 text-red-700" :
          message.type === "success" ? "border-green-200 bg-green-50 text-green-700" :
          "border-blue-200 bg-blue-50 text-blue-700"
        }`}>
          {message.type === "success" ? <CheckCircle2 className="h-4 w-4 flex-shrink-0" /> : <AlertCircle className="h-4 w-4 flex-shrink-0" />}
          {message.text}
          {successData && (
            <Button
              variant="link"
              size="sm"
              className="ml-2 h-auto p-0"
              onClick={() => navigate(`/transactions/view/${successData.transactionId}`)}
            >
              View Transaction
            </Button>
          )}
        </div>
      )}

      {/* Form in grouped cards */}
      <div className="grid gap-6 md:grid-cols-2">
        {/* Identity Card */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <CreditCard className="h-4 w-4 text-blue-600" />
              <CardTitle className="text-sm font-medium">Identity</CardTitle>
            </div>
            <CardDescription>Provide either Account ID or Card Number</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {renderField("accountId", "Account ID", "e.g., 12345678901")}
            <div className="flex items-center gap-4">
              <div className="flex-1 h-px bg-border" />
              <Badge variant="secondary" className="text-xs">OR</Badge>
              <div className="flex-1 h-px bg-border" />
            </div>
            {renderField("cardNumber", "Card Number", "e.g., 4111111111111111")}
          </CardContent>
        </Card>

        {/* Classification Card */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <FileText className="h-4 w-4 text-purple-600" />
              <CardTitle className="text-sm font-medium">Classification</CardTitle>
            </div>
            <CardDescription>Transaction type and category details</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              {renderField("typeCode", "Type Code", "e.g., SA", { required: true })}
              {renderField("categoryCode", "Category Code", "e.g., 5001", { required: true })}
            </div>
            {renderField("source", "Source", "e.g., ONLINE")}
            {renderField("description", "Description", "Transaction description")}
          </CardContent>
        </Card>

        {/* Financial Card */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <DollarSign className="h-4 w-4 text-green-600" />
              <CardTitle className="text-sm font-medium">Financial</CardTitle>
            </div>
            <CardDescription>Amount and date information</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {renderField("amount", "Amount", "e.g., 100.50", { required: true })}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="originationDate" className="text-sm flex items-center gap-1">
                  <Calendar className="h-3 w-3" />
                  Origination Date
                </Label>
                <Input
                  id="originationDate"
                  type="date"
                  value={form.originationDate}
                  onChange={(e) => updateField("originationDate", e.target.value)}
                  onBlur={() => handleBlur("originationDate")}
                  className={fieldErrors.originationDate ? "border-red-300 focus-visible:ring-red-400" : ""}
                />
                {fieldErrors.originationDate && (
                  <p className="text-xs text-red-600 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.originationDate}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="processingDate" className="text-sm flex items-center gap-1">
                  <Calendar className="h-3 w-3" />
                  Processing Date
                </Label>
                <Input
                  id="processingDate"
                  type="date"
                  value={form.processingDate}
                  onChange={(e) => updateField("processingDate", e.target.value)}
                  onBlur={() => handleBlur("processingDate")}
                  className={fieldErrors.processingDate ? "border-red-300 focus-visible:ring-red-400" : ""}
                />
                {fieldErrors.processingDate && (
                  <p className="text-xs text-red-600 flex items-center gap-1">
                    <AlertCircle className="h-3 w-3" />
                    {fieldErrors.processingDate}
                  </p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Merchant Card */}
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <Building className="h-4 w-4 text-orange-600" />
              <CardTitle className="text-sm font-medium">Merchant</CardTitle>
            </div>
            <CardDescription>Merchant location and identification</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {renderField("merchantId", "Merchant ID", "e.g., 12345")}
            {renderField("merchantName", "Merchant Name", "e.g., Amazon")}
            <div className="grid grid-cols-2 gap-4">
              {renderField("merchantCity", "City", "e.g., Seattle")}
              {renderField("merchantZip", "ZIP Code", "e.g., 98101")}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Submit button */}
      <Card>
        <CardContent className="pt-6">
          <Button
            className="w-full sm:w-auto"
            size="lg"
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? (
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
            ) : (
              <Send className="h-4 w-4 mr-2" />
            )}
            Submit Transaction
          </Button>
        </CardContent>
      </Card>

      {/* Confirmation Dialog (replaces Y/N text input) */}
      <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <DialogContent onClose={() => setConfirmOpen(false)}>
          <DialogHeader>
            <DialogTitle>Confirm Transaction</DialogTitle>
            <DialogDescription>
              {confirmData?.message || "Please confirm the transaction details."}
            </DialogDescription>
          </DialogHeader>

          {confirmData && (
            <div className="my-4 space-y-3 rounded-lg border bg-muted/50 p-4">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Resolved Account ID</span>
                <span className="font-mono font-medium">{confirmData.resolvedAccountId}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Resolved Card Number</span>
                <span className="font-mono font-medium">{confirmData.resolvedCardNumber}</span>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleConfirm}>
              <CheckCircle2 className="h-4 w-4 mr-2" />
              Confirm & Submit
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
