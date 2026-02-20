import React, { useState } from 'react'
import { authApi } from '../services/api'

function Login({ onLogin }) {
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const response = await authApi.login(userId, password)
      onLogin(response)
    } catch (err) {
      setError(err.message || 'Login failed')
    }
  }

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>CardDemo Sign On</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>User ID</label>
            <input type="text" value={userId} onChange={(e) => setUserId(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          {error && <p className="error-message">{error}</p>}
          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem' }}>Sign In</button>
        </form>
      </div>
    </div>
  )
}

export default Login
