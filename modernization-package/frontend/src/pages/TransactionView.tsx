import { useState, useEffect } from "react"
import { useParams, useNavigate } from "react-router-dom"
import { viewTransaction, TransactionDetail, ErrorResponse } from "@/services/api"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import {
  ArrowLeft,
  Search,
  Eraser,
  AlertCircle,
  Loader2,
  CreditCard,
  Building,
  Calendar,
  DollarSign,
  Hash,
  FileText,
} from "lucide-react"

export default function TransactionView() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [transactionId, setTransactionId] = useState(id || "")
  const [transaction, setTransaction] = useState<TransactionDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ text: string; type: "error" | "info" } | null>(null)

  useEffect(() => {
    if (id) {
      setTransactionId(id)
      fetchTransaction(id)
    }
  }, [id])

  const fetchTransaction = async (txId: string) => {
    if (!txId.trim()) {
      setMessage({ text: "Please enter a Transaction ID", type: "error" })
      return
    }
    if (!/^\d+$/.test(txId.trim())) {
      setMessage({ text: "Transaction ID must be numeric", type: "error" })
      return
    }
    setLoading(true)
    setMessage(null)
    setTransaction(null)
    try {
      const detail = await viewTransaction(txId.trim())
      setTransaction(detail)
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || "Transaction not found", type: "error" })
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = () => {
    fetchTransaction(transactionId)
  }

  const handleClear = () => {
    setTransactionId("")
    setTransaction(null)
    setMessage(null)
  }

  const formatDate = (ts: string) => {
    if (!ts) return "—"
    try {
      return new Date(ts).toLocaleDateString("en-US", {
        year: "numeric",
        month: "long",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      })
    } catch {
      return ts
    }
  }

  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      signDisplay: "always",
    }).format(amount)
  }

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Lookup card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Look Up Transaction</CardTitle>
          <CardDescription>Enter a Transaction ID to view its details</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="flex-1 max-w-xs">
              <Label htmlFor="txId" className="sr-only">Transaction ID</Label>
              <div className="relative">
                <Hash className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="txId"
                  placeholder="Enter Transaction ID"
                  value={transactionId}
                  onChange={(e) => setTransactionId(e.target.value)}
                  onKeyDown={(e) => { if (e.key === "Enter") handleSearch() }}
                  className="pl-10 font-mono"
                />
              </div>
            </div>
            <Button onClick={handleSearch} disabled={loading}>
              {loading ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : <Search className="h-4 w-4 mr-2" />}
              Search
            </Button>
            <Button variant="outline" onClick={handleClear}>
              <Eraser className="h-4 w-4 mr-2" />
              Clear
            </Button>
            <Button variant="ghost" onClick={() => navigate("/transactions")}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to List
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Alert message */}
      {message && (
        <div className={`flex items-center gap-2 rounded-lg border px-4 py-3 text-sm ${
          message.type === "error"
            ? "border-red-200 bg-red-50 text-red-700"
            : "border-blue-200 bg-blue-50 text-blue-700"
        }`}>
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          {message.text}
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-sm text-muted-foreground">Loading transaction details...</span>
        </div>
      )}

      {/* Transaction details */}
      {transaction && !loading && (
        <div className="grid gap-6 md:grid-cols-2">
          {/* Identity Card */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <CreditCard className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-sm font-medium">Identity</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label className="text-xs text-muted-foreground">Transaction ID</Label>
                <p className="font-mono font-medium mt-1">{transaction.transactionId}</p>
              </div>
              <Separator />
              <div>
                <Label className="text-xs text-muted-foreground">Account ID</Label>
                <p className="font-mono font-medium mt-1">{transaction.accountId || "N/A"}</p>
              </div>
              <Separator />
              <div>
                <Label className="text-xs text-muted-foreground">Card Number</Label>
                <p className="font-mono font-medium mt-1">{transaction.cardNumber}</p>
              </div>
            </CardContent>
          </Card>

          {/* Classification Card */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <FileText className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-sm font-medium">Classification</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <Label className="text-xs text-muted-foreground">Type Code</Label>
                  <p className="font-medium mt-1">{transaction.typeCode}</p>
                </div>
                <Badge variant="outline">{transaction.typeCode}</Badge>
              </div>
              <Separator />
              <div>
                <Label className="text-xs text-muted-foreground">Category Code</Label>
                <p className="font-medium mt-1">{String(transaction.categoryCode)}</p>
              </div>
              <Separator />
              <div>
                <Label className="text-xs text-muted-foreground">Source</Label>
                <p className="font-medium mt-1">{transaction.source}</p>
              </div>
              <Separator />
              <div>
                <Label className="text-xs text-muted-foreground">Description</Label>
                <p className="font-medium mt-1">{transaction.description}</p>
              </div>
            </CardContent>
          </Card>

          {/* Financial Card */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <DollarSign className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-sm font-medium">Financial</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label className="text-xs text-muted-foreground">Amount</Label>
                <p className={`text-2xl font-bold font-mono mt-1 ${transaction.amount >= 0 ? "text-green-700" : "text-red-700"}`}>
                  {formatAmount(transaction.amount)}
                </p>
              </div>
              <Separator />
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-xs text-muted-foreground flex items-center gap-1">
                    <Calendar className="h-3 w-3" /> Origination Date
                  </Label>
                  <p className="text-sm font-medium mt-1">{formatDate(transaction.originationTimestamp)}</p>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground flex items-center gap-1">
                    <Calendar className="h-3 w-3" /> Processing Date
                  </Label>
                  <p className="text-sm font-medium mt-1">{formatDate(transaction.processingTimestamp)}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Merchant Card */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center gap-2">
                <Building className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-sm font-medium">Merchant</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label className="text-xs text-muted-foreground">Merchant ID</Label>
                <p className="font-mono font-medium mt-1">{String(transaction.merchantId)}</p>
              </div>
              <Separator />
              <div>
                <Label className="text-xs text-muted-foreground">Merchant Name</Label>
                <p className="font-medium mt-1">{transaction.merchantName}</p>
              </div>
              <Separator />
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-xs text-muted-foreground">City</Label>
                  <p className="font-medium mt-1">{transaction.merchantCity}</p>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">ZIP Code</Label>
                  <p className="font-mono font-medium mt-1">{transaction.merchantZip}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Empty state */}
      {!transaction && !loading && !message && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16 text-center">
            <Search className="h-12 w-12 text-muted-foreground/30 mb-4" />
            <h3 className="text-lg font-medium text-muted-foreground">No Transaction Selected</h3>
            <p className="text-sm text-muted-foreground mt-1">Enter a Transaction ID above to view its details</p>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
