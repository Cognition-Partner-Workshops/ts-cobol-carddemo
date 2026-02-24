import { Routes, Route, Navigate, NavLink } from 'react-router-dom'
import TransactionList from './pages/TransactionList'
import TransactionView from './pages/TransactionView'
import TransactionAdd from './pages/TransactionAdd'
import MainMenu from './pages/MainMenu'

function App() {
  return (
    <>
      <nav className="nav-bar">
        <span className="brand">CardDemo</span>
        <NavLink to="/menu" className={({ isActive }) => isActive ? 'active' : ''}>
          Menu
        </NavLink>
        <NavLink to="/transactions" className={({ isActive }) => isActive ? 'active' : ''}>
          List (CT00)
        </NavLink>
        <NavLink to="/transactions/add" className={({ isActive }) => isActive ? 'active' : ''}>
          Add (CT02)
        </NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<Navigate to="/menu" replace />} />
        <Route path="/menu" element={<MainMenu />} />
        <Route path="/transactions" element={<TransactionList />} />
        <Route path="/transactions/view/:transactionId" element={<TransactionView />} />
        <Route path="/transactions/view" element={<TransactionView />} />
        <Route path="/transactions/add" element={<TransactionAdd />} />
      </Routes>
    </>
  )
}

export default App
