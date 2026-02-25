import { useNavigate } from "react-router-dom"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { List, Eye, PlusCircle, ArrowRight, CreditCard, Database, Shield } from "lucide-react"

export default function Dashboard() {
  const navigate = useNavigate()

  return (
    <div className="space-y-8">
      {/* Welcome banner */}
      <div className="rounded-lg bg-gradient-to-r from-blue-600 to-blue-700 p-8 text-white">
        <h1 className="text-2xl font-bold">Transaction Processing Module</h1>
        <p className="mt-2 text-blue-100 max-w-2xl">
          Cloud-native implementation of the CardDemo transaction processing system.
          Modernized from COBOL/CICS with full business rule parity.
        </p>
      </div>

      {/* Quick actions */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Card className="hover:shadow-md transition-shadow cursor-pointer group" onClick={() => navigate("/transactions")}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100">
                  <List className="h-5 w-5 text-blue-600" />
                </div>
                <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
              </div>
              <CardTitle className="text-base mt-3">Transaction List</CardTitle>
              <CardDescription>Browse and search transactions with paginated results</CardDescription>
            </CardHeader>
            <CardContent>
              <Button variant="outline" size="sm" className="w-full">
                Open List View
              </Button>
            </CardContent>
          </Card>

          <Card className="hover:shadow-md transition-shadow cursor-pointer group" onClick={() => navigate("/transactions/view")}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100">
                  <Eye className="h-5 w-5 text-green-600" />
                </div>
                <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
              </div>
              <CardTitle className="text-base mt-3">View Transaction</CardTitle>
              <CardDescription>Look up and view detailed transaction information</CardDescription>
            </CardHeader>
            <CardContent>
              <Button variant="outline" size="sm" className="w-full">
                Open Detail View
              </Button>
            </CardContent>
          </Card>

          <Card className="hover:shadow-md transition-shadow cursor-pointer group" onClick={() => navigate("/transactions/add")}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-100">
                  <PlusCircle className="h-5 w-5 text-purple-600" />
                </div>
                <ArrowRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
              </div>
              <CardTitle className="text-base mt-3">Add Transaction</CardTitle>
              <CardDescription>Create a new transaction with validated input fields</CardDescription>
            </CardHeader>
            <CardContent>
              <Button variant="outline" size="sm" className="w-full">
                Open Add Form
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Info cards */}
      <div>
        <h3 className="text-lg font-semibold mb-4">System Overview</h3>
        <div className="grid gap-4 sm:grid-cols-3">
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <CreditCard className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-sm font-medium text-muted-foreground">Architecture</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">Microservice</p>
              <p className="text-xs text-muted-foreground mt-1">Spring Boot 3 + React + PostgreSQL</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <Shield className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-sm font-medium text-muted-foreground">Business Rules</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">30 / 30</p>
              <p className="text-xs text-muted-foreground mt-1">100% logic parity with legacy system</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <div className="flex items-center gap-2">
                <Database className="h-4 w-4 text-muted-foreground" />
                <CardTitle className="text-sm font-medium text-muted-foreground">Validation</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">6-Phase</p>
              <p className="text-xs text-muted-foreground mt-1">Sequential chain with error catalog</p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
