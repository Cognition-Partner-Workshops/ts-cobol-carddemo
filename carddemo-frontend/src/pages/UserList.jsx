import React, { useState, useEffect } from 'react'
import { userApi } from '../services/api'

function UserList() {
  const [users, setUsers] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [showAdd, setShowAdd] = useState(false)
  const [editUser, setEditUser] = useState(null)
  const [form, setForm] = useState({ userId: '', firstName: '', lastName: '', password: '', userType: 'U' })
  const [error, setError] = useState('')

  useEffect(() => { loadUsers() }, [page])

  const loadUsers = async () => {
    try {
      const data = await userApi.list(page, 10)
      setUsers(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('Failed to load users:', err)
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await userApi.create(form)
      setShowAdd(false)
      setForm({ userId: '', firstName: '', lastName: '', password: '', userType: 'U' })
      loadUsers()
    } catch (err) {
      setError(err.message || 'Failed to create user')
    }
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await userApi.update(editUser.usrId, { firstName: form.firstName, lastName: form.lastName, password: form.password || undefined, userType: form.userType })
      setEditUser(null)
      loadUsers()
    } catch (err) {
      setError(err.message || 'Failed to update user')
    }
  }

  const handleDelete = async (userId) => {
    if (!window.confirm(`Delete user ${userId}?`)) return
    try {
      await userApi.delete(userId)
      loadUsers()
    } catch (err) {
      console.error('Failed to delete user:', err)
    }
  }

  const startEdit = (user) => {
    setEditUser(user)
    setForm({ userId: user.usrId, firstName: user.usrFname, lastName: user.usrLname, password: '', userType: user.usrType })
  }

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <div>
      <div className="card">
        <h3>User Administration</h3>
        <div className="actions">
          <button className="btn btn-primary btn-sm" onClick={() => { setShowAdd(true); setEditUser(null); setForm({ userId: '', firstName: '', lastName: '', password: '', userType: 'U' }) }}>Add User</button>
        </div>
        <table>
          <thead><tr><th>User ID</th><th>First Name</th><th>Last Name</th><th>Type</th><th>Actions</th></tr></thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.usrId}>
                <td>{user.usrId}</td>
                <td>{user.usrFname}</td>
                <td>{user.usrLname}</td>
                <td>{user.usrType === 'A' ? 'Admin' : 'User'}</td>
                <td>
                  <button className="btn btn-primary btn-sm" onClick={() => startEdit(user)} style={{ marginRight: '0.25rem' }}>Edit</button>
                  <button className="btn btn-danger btn-sm" onClick={() => handleDelete(user.usrId)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="pagination">
          <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Previous</button>
          <span>Page {page + 1} of {totalPages}</span>
          <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next</button>
        </div>
      </div>
      {(showAdd || editUser) && (
        <div className="modal-overlay" onClick={() => { setShowAdd(false); setEditUser(null) }}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>{editUser ? 'Edit User' : 'Add User'}</h3>
            <form onSubmit={editUser ? handleUpdate : handleCreate}>
              {!editUser && <div className="form-group"><label>User ID</label><input type="text" value={form.userId} onChange={handleChange('userId')} required maxLength={8} /></div>}
              <div className="form-group"><label>First Name</label><input type="text" value={form.firstName} onChange={handleChange('firstName')} required /></div>
              <div className="form-group"><label>Last Name</label><input type="text" value={form.lastName} onChange={handleChange('lastName')} required /></div>
              <div className="form-group"><label>Password</label><input type="password" value={form.password} onChange={handleChange('password')} required={!editUser} /></div>
              <div className="form-group"><label>Type</label><select value={form.userType} onChange={handleChange('userType')}><option value="U">User</option><option value="A">Admin</option></select></div>
              {error && <p className="error-message">{error}</p>}
              <div className="modal-actions"><button type="button" className="btn btn-secondary" onClick={() => { setShowAdd(false); setEditUser(null) }}>Cancel</button><button type="submit" className="btn btn-primary">{editUser ? 'Update' : 'Create'}</button></div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default UserList
