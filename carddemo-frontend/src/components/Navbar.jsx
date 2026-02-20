import React from 'react'
import { Link } from 'react-router-dom'

function Navbar({ user, onLogout }) {
  return (
    <nav className="navbar">
      <Link to="/" style={{ color: 'white', textDecoration: 'none' }}>
        <h1>CardDemo</h1>
      </Link>
      <div className="navbar-user">
        <span>{user.firstName} {user.lastName} ({user.userType === 'A' ? 'Admin' : 'User'})</span>
        <button className="btn btn-secondary btn-sm" onClick={onLogout}>Logout</button>
      </div>
    </nav>
  )
}

export default Navbar
