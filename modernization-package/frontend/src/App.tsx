import { BrowserRouter, Routes, Route } from "react-router-dom"
import AppLayout from "@/components/layout/AppLayout"
import Dashboard from "@/pages/Dashboard"
import TransactionList from "@/pages/TransactionList"
import TransactionView from "@/pages/TransactionView"
import TransactionAdd from "@/pages/TransactionAdd"

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/transactions" element={<TransactionList />} />
          <Route path="/transactions/view" element={<TransactionView />} />
          <Route path="/transactions/view/:id" element={<TransactionView />} />
          <Route path="/transactions/add" element={<TransactionAdd />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
