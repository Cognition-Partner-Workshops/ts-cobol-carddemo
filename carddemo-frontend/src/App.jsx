import React, { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import MainMenu from './pages/MainMenu'
import AccountList from './pages/AccountList'
import AccountView from './pages/AccountView'
import CardList from './pages/CardList'
import CardView from './pages/CardView'
import TransactionList from './pages/TransactionList'
import TransactionView from './pages/TransactionView'
import TransactionAdd from './pages/TransactionAdd'
import TransactionReport from './pages/TransactionReport'
import BillPayment from './pages/BillPayment'
import UserList from './pages/UserList'
import Navbar from './components/Navbar'

function App() {
  const [user, setUser] = useState(null)

  useEffect(() => {
    const stored = localStorage.getItem('user')
    if (stored) {
      setUser(JSON.parse(stored))
    }
  }, [])

  const handleLogin = (userData) => {
    setUser(userData)
    localStorage.setItem('user', JSON.stringify(userData))
    localStorage.setItem('token', userData.token)
  }

  const handleLogout = () => {
    setUser(null)
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  if (!user) {
    return <Login onLogin={handleLogin} />
  }

  return (
    <BrowserRouter>
      <div className="app-container">
        <Navbar user={user} onLogout={handleLogout} />
        <div className="main-content">
          <Routes>
            <Route path="/" element={<MainMenu user={user} />} />
            <Route path="/accounts" element={<AccountList />} />
            <Route path="/accounts/:id" element={<AccountView />} />
            <Route path="/cards" element={<CardList />} />
            <Route path="/cards/:cardNum" element={<CardView />} />
            <Route path="/transactions" element={<TransactionList />} />
            <Route path="/transactions/add" element={<TransactionAdd />} />
            <Route path="/transactions/:id" element={<TransactionView />} />
            <Route path="/reports" element={<TransactionReport />} />
            <Route path="/bill-payment" element={<BillPayment />} />
            {user.userType === 'A' && (
              <Route path="/admin/users" element={<UserList />} />
            )}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  )
}

export default App
