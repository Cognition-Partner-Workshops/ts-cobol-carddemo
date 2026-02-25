import { useState, useEffect, useCallback } from "react"
import { useNavigate } from "react-router-dom"
import {
  listTransactions,
  viewTransaction,
  TransactionListResponse,
  TransactionDetail,
  ErrorResponse,
} from "@/services/api"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet"
import { Separator } from "@/components/ui/separator"
import {
  Search,
  ChevronLeft,
  ChevronRight,
  MoreHorizontal,
  Eye,
  ArrowUpDown,
  AlertCircle,
  Loader2,
  CreditCard,
} from "lucide-react"

type SortField = "transactionId" | "amount" | "originationTimestamp"
type SortDir = "asc" | "desc"

export default function TransactionList() {
  const navigate = useNavigate()
  const [data, setData] = useState<TransactionListResponse | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [filterValue, setFilterValue] = useState("")
  const [activeFilter, setActiveFilter] = useState<string | undefined>(undefined)
  const [message, setMessage] = useState<{ text: string; type: "error" | "warning" | "info" } | null>(null)
  const [loading, setLoading] = useState(false)
  const [sortField, setSortField] = useState<SortField>("transactionId")
  const [sortDir, setSortDir] = useState<SortDir>("asc")

  // Side drawer state for CT01
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionDetail | null>(null)
  const [drawerLoading, setDrawerLoading] = useState(false)

  const fetchData = useCallback(async (page: number, startId?: string) => {
    setLoading(true)
    setMessage(null)
    try {
      const result = await listTransactions(page, 10, startId)
      setData(result)
      setCurrentPage(result.page)
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || "Error loading transactions", type: "error" })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData(0)
  }, [fetchData])

  const handleSearch = () => {
    if (filterValue.trim() === "") {
      setActiveFilter(undefined)
      fetchData(0)
      return
    }
    if (!/^\d+$/.test(filterValue.trim())) {
      setMessage({ text: "Transaction ID must be numeric", type: "error" })
      return
    }
    setActiveFilter(filterValue.trim())
    fetchData(0, filterValue.trim())
  }

  const handleNextPage = () => {
    if (!data || data.last) {
      setMessage({ text: "You are on the last page", type: "warning" })
      return
    }
    fetchData(currentPage + 1, activeFilter)
  }

  const handlePrevPage = () => {
    if (!data || data.first) {
      setMessage({ text: "You are on the first page", type: "warning" })
      return
    }
    fetchData(currentPage - 1, activeFilter)
  }

  const handleViewTransaction = async (txId: string) => {
    setDrawerOpen(true)
    setDrawerLoading(true)
    setSelectedTransaction(null)
    try {
      const detail = await viewTransaction(txId)
      setSelectedTransaction(detail)
    } catch (err) {
      const error = err as ErrorResponse
      setMessage({ text: error.message || "Error loading transaction", type: "error" })
      setDrawerOpen(false)
    } finally {
      setDrawerLoading(false)
    }
  }

  const handleViewFullPage = (txId: string) => {
    navigate(`/transactions/view/${txId}`)
  }

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir(sortDir === "asc" ? "desc" : "asc")
    } else {
      setSortField(field)
      setSortDir("asc")
    }
  }

  const sortedContent = data?.content ? [...data.content].sort((a, b) => {
    const modifier = sortDir === "asc" ? 1 : -1
    if (sortField === "transactionId") return a.transactionId.localeCompare(b.transactionId) * modifier
    if (sortField === "amount") return (a.amount - b.amount) * modifier
    if (sortField === "originationTimestamp") return a.originationTimestamp.localeCompare(b.originationTimestamp) * modifier
    return 0
  }) : []

  const formatDate = (ts: string) => {
    if (!ts) return "—"
    try {
      return new Date(ts).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" })
    } catch {
      return ts
    }
  }

  const formatAmount = (amount: number) => {
    return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", signDisplay: "always" }).format(amount)
  }

  return (
    <div className="space-y-6">
      {/* Search and filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Filter by Transaction ID..."
                value={filterValue}
                onChange={(e) => setFilterValue(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter") handleSearch() }}
                className="pl-10"
              />
            </div>
            <Button onClick={handleSearch}>Search</Button>
            {activeFilter && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => { setFilterValue(""); setActiveFilter(undefined); fetchData(0) }}
              >
                Clear filter
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Alert message */}
      {message && (
        <div className={`flex items-center gap-2 rounded-lg border px-4 py-3 text-sm ${
          message.type === "error" ? "border-red-200 bg-red-50 text-red-700" :
          message.type === "warning" ? "border-yellow-200 bg-yellow-50 text-yellow-700" :
          "border-blue-200 bg-blue-50 text-blue-700"
        }`}>
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          {message.text}
        </div>
      )}

      {/* Data table */}
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Transactions</CardTitle>
            {data && (
              <span className="text-sm text-muted-foreground">
                {data.totalElements} total records
              </span>
            )}
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              <span className="ml-2 text-sm text-muted-foreground">Loading transactions...</span>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="bg-muted/50">
                  <TableHead className="cursor-pointer select-none" onClick={() => handleSort("transactionId")}>
                    <div className="flex items-center gap-1">
                      Transaction ID
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </TableHead>
                  <TableHead>Card Number</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Source</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="cursor-pointer select-none" onClick={() => handleSort("amount")}>
                    <div className="flex items-center gap-1">
                      Amount
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </TableHead>
                  <TableHead className="cursor-pointer select-none" onClick={() => handleSort("originationTimestamp")}>
                    <div className="flex items-center gap-1">
                      Date
                      <ArrowUpDown className="h-3 w-3" />
                    </div>
                  </TableHead>
                  <TableHead className="w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {sortedContent.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} className="h-24 text-center text-muted-foreground">
                      No transactions found
                    </TableCell>
                  </TableRow>
                ) : (
                  sortedContent.map((tx) => (
                    <TableRow
                      key={tx.transactionId}
                      className="cursor-pointer"
                      onClick={() => handleViewTransaction(tx.transactionId)}
                    >
                      <TableCell className="font-mono text-xs font-medium">{tx.transactionId}</TableCell>
                      <TableCell className="font-mono text-xs">{tx.cardNumber}</TableCell>
                      <TableCell>
                        <Badge variant="outline" className="text-xs">{tx.typeCode}</Badge>
                      </TableCell>
                      <TableCell className="text-xs">{tx.categoryCode}</TableCell>
                      <TableCell className="text-xs">{tx.source}</TableCell>
                      <TableCell className="text-xs max-w-[200px] truncate">{tx.description}</TableCell>
                      <TableCell className={`text-right font-mono text-xs font-medium ${tx.amount >= 0 ? "text-green-700" : "text-red-700"}`}>
                        {formatAmount(tx.amount)}
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">{formatDate(tx.originationTimestamp)}</TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger
                            className="h-8 w-8 p-0 flex items-center justify-center rounded-md hover:bg-muted"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <MoreHorizontal className="h-4 w-4" />
                          </DropdownMenuTrigger>
                          <DropdownMenuContent>
                            <DropdownMenuItem onClick={() => handleViewTransaction(tx.transactionId)}>
                              <Eye className="mr-2 h-4 w-4" /> View Details
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => handleViewFullPage(tx.transactionId)}>
                              <CreditCard className="mr-2 h-4 w-4" /> Full Page View
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}

          {/* Pagination */}
          {data && (
            <div className="flex items-center justify-between border-t px-6 py-4">
              <span className="text-sm text-muted-foreground">
                Page {data.page + 1} of {Math.max(data.totalPages, 1)}
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handlePrevPage}
                  disabled={data.first}
                >
                  <ChevronLeft className="h-4 w-4 mr-1" />
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleNextPage}
                  disabled={data.last}
                >
                  Next
                  <ChevronRight className="h-4 w-4 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Side Drawer for Transaction Detail (CT01) */}
      <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
        <SheetContent side="right" onClose={() => setDrawerOpen(false)}>
          <SheetHeader>
            <SheetTitle>Transaction Details</SheetTitle>
            <SheetDescription>
              {selectedTransaction ? `ID: ${selectedTransaction.transactionId}` : "Loading..."}
            </SheetDescription>
          </SheetHeader>

          {drawerLoading ? (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : selectedTransaction ? (
            <div className="mt-6 space-y-6 overflow-y-auto max-h-[calc(100vh-200px)]">
              {/* Identity */}
              <div>
                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Identity</h4>
                <div className="space-y-3">
                  <DetailRow label="Transaction ID" value={selectedTransaction.transactionId} mono />
                  <DetailRow label="Account ID" value={selectedTransaction.accountId || "N/A"} mono />
                  <DetailRow label="Card Number" value={selectedTransaction.cardNumber} mono />
                </div>
              </div>

              <Separator />

              {/* Classification */}
              <div>
                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Classification</h4>
                <div className="space-y-3">
                  <DetailRow label="Type Code" value={selectedTransaction.typeCode} />
                  <DetailRow label="Category Code" value={String(selectedTransaction.categoryCode)} />
                  <DetailRow label="Source" value={selectedTransaction.source} />
                  <DetailRow label="Description" value={selectedTransaction.description} />
                </div>
              </div>

              <Separator />

              {/* Financial */}
              <div>
                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Financial</h4>
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">Amount</span>
                    <span className={`text-lg font-bold font-mono ${selectedTransaction.amount >= 0 ? "text-green-700" : "text-red-700"}`}>
                      {formatAmount(selectedTransaction.amount)}
                    </span>
                  </div>
                  <DetailRow label="Origination Date" value={formatDate(selectedTransaction.originationTimestamp)} />
                  <DetailRow label="Processing Date" value={formatDate(selectedTransaction.processingTimestamp)} />
                </div>
              </div>

              <Separator />

              {/* Merchant */}
              <div>
                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Merchant</h4>
                <div className="space-y-3">
                  <DetailRow label="Merchant ID" value={String(selectedTransaction.merchantId)} mono />
                  <DetailRow label="Merchant Name" value={selectedTransaction.merchantName} />
                  <DetailRow label="Merchant City" value={selectedTransaction.merchantCity} />
                  <DetailRow label="Merchant ZIP" value={selectedTransaction.merchantZip} />
                </div>
              </div>

              {/* Actions */}
              <div className="pt-4">
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => { setDrawerOpen(false); handleViewFullPage(selectedTransaction.transactionId) }}
                >
                  Open Full Page View
                </Button>
              </div>
            </div>
          ) : null}
        </SheetContent>
      </Sheet>
    </div>
  )
}

function DetailRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex justify-between items-center">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className={`text-sm font-medium ${mono ? "font-mono" : ""}`}>{value}</span>
    </div>
  )
}
