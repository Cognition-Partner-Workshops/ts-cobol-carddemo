import React from 'react'
import { Link } from 'react-router-dom'

function MainMenu({ user }) {
  const menuItems = [
    { path: '/accounts', title: 'Account View', desc: 'View account details and balances' },
    { path: '/accounts', title: 'Account Update', desc: 'Update account information' },
    { path: '/cards', title: 'Credit Card List', desc: 'List all credit cards' },
    { path: '/cards', title: 'Credit Card View', desc: 'View credit card details' },
    { path: '/cards', title: 'Credit Card Update', desc: 'Update credit card information' },
    { path: '/transactions', title: 'Transaction List', desc: 'List all transactions' },
    { path: '/transactions/add', title: 'Transaction Add', desc: 'Add a new transaction' },
    { path: '/reports', title: 'Transaction Reports', desc: 'Generate transaction reports' },
    { path: '/bill-payment', title: 'Bill Payment', desc: 'Make a bill payment' },
  ]

  if (user.userType === 'A') {
    menuItems.push({ path: '/admin/users', title: 'User Administration', desc: 'Manage system users' })
  }

  return (
    <div>
      <div className="card">
        <h3>Main Menu</h3>
        <div className="menu-grid">
          {menuItems.map((item, idx) => (
            <Link key={idx} to={item.path} className="menu-item">
              <h4>{item.title}</h4>
              <p>{item.desc}</p>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}

export default MainMenu
