const API_BASE = '/api';

function getHeaders() {
  const headers = { 'Content-Type': 'application/json' };
  const token = localStorage.getItem('token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

async function request(url, options = {}) {
  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers: getHeaders(),
  });
  if (response.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/';
    return;
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }));
    throw new Error(error.message || 'Request failed');
  }
  if (response.status === 204) return null;
  return response.json();
}

export const authApi = {
  login: (userId, password) =>
    request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ userId, password }),
    }),
};

export const accountApi = {
  list: (page = 0, size = 10) => request(`/accounts?page=${page}&size=${size}`),
  get: (id) => request(`/accounts/${id}`),
  update: (id, data) => request(`/accounts/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
};

export const cardApi = {
  list: (page = 0, size = 10, acctId) => {
    let url = `/cards?page=${page}&size=${size}`;
    if (acctId) url += `&acctId=${acctId}`;
    return request(url);
  },
  get: (cardNum) => request(`/cards/${cardNum}`),
  update: (cardNum, data) => request(`/cards/${cardNum}`, { method: 'PUT', body: JSON.stringify(data) }),
};

export const transactionApi = {
  list: (page = 0, size = 10, filters = {}) => {
    let url = `/transactions?page=${page}&size=${size}`;
    if (filters.cardNum) url += `&cardNum=${filters.cardNum}`;
    if (filters.acctId) url += `&acctId=${filters.acctId}`;
    return request(url);
  },
  get: (id) => request(`/transactions/${id}`),
  add: (data) => request('/transactions', { method: 'POST', body: JSON.stringify(data) }),
  report: (startDate, endDate) => request(`/transactions/reports?startDate=${startDate}&endDate=${endDate}`),
};

export const billPaymentApi = {
  pay: (acctId, amount) =>
    request('/bill-payments', { method: 'POST', body: JSON.stringify({ acctId, amount }) }),
};

export const userApi = {
  list: (page = 0, size = 10) => request(`/admin/users?page=${page}&size=${size}`),
  get: (id) => request(`/admin/users/${id}`),
  create: (data) => request('/admin/users', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/admin/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/admin/users/${id}`, { method: 'DELETE' }),
};

export const transactionTypeApi = {
  list: () => request('/transaction-types'),
  get: (typeCd) => request(`/transaction-types/${typeCd}`),
  create: (data) => request('/transaction-types', { method: 'POST', body: JSON.stringify(data) }),
  update: (typeCd, data) => request(`/transaction-types/${typeCd}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (typeCd) => request(`/transaction-types/${typeCd}`, { method: 'DELETE' }),
  listCategories: (typeCd) => request(`/transaction-types/${typeCd}/categories`),
};

export const authorizationApi = {
  list: (page = 0, size = 10, acctId) => {
    let url = `/authorizations?page=${page}&size=${size}`;
    if (acctId) url += `&acctId=${acctId}`;
    return request(url);
  },
  get: (id) => request(`/authorizations/${id}`),
  markFraud: (id) => request(`/authorizations/${id}/fraud`, { method: 'PUT' }),
};
