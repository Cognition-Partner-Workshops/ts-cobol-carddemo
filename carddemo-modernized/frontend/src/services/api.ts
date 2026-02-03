import { 
  ApiResponse, 
  LoginRequest, 
  LoginResponse, 
  Customer, 
  Account, 
  Card, 
  Transaction, 
  User,
  PageResponse,
  TransactionRequest,
  BillPaymentRequest
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

class ApiService {
  private token: string | null = null;

  setToken(token: string | null) {
    this.token = token;
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  }

  getToken(): string | null {
    if (!this.token) {
      this.token = localStorage.getItem('token');
    }
    return this.token;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const token = this.getToken();
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      if (response.status === 401) {
        this.setToken(null);
        window.location.href = '/login';
      }
      const error = await response.json();
      throw new Error(error.message || 'An error occurred');
    }

    return response.json();
  }

  async login(credentials: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    return this.request<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
  }

  async getCurrentUser(): Promise<ApiResponse<LoginResponse>> {
    return this.request<LoginResponse>('/api/auth/me');
  }

  async getCustomers(): Promise<ApiResponse<Customer[]>> {
    return this.request<Customer[]>('/api/customers');
  }

  async getCustomer(customerId: string): Promise<ApiResponse<Customer>> {
    return this.request<Customer>(`/api/customers/${customerId}`);
  }

  async createCustomer(customer: Customer): Promise<ApiResponse<Customer>> {
    return this.request<Customer>('/api/customers', {
      method: 'POST',
      body: JSON.stringify(customer),
    });
  }

  async updateCustomer(customerId: string, customer: Customer): Promise<ApiResponse<Customer>> {
    return this.request<Customer>(`/api/customers/${customerId}`, {
      method: 'PUT',
      body: JSON.stringify(customer),
    });
  }

  async deleteCustomer(customerId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/customers/${customerId}`, {
      method: 'DELETE',
    });
  }

  async getAccounts(): Promise<ApiResponse<Account[]>> {
    return this.request<Account[]>('/api/accounts');
  }

  async getAccount(accountId: string): Promise<ApiResponse<Account>> {
    return this.request<Account>(`/api/accounts/${accountId}`);
  }

  async createAccount(account: Account): Promise<ApiResponse<Account>> {
    return this.request<Account>('/api/accounts', {
      method: 'POST',
      body: JSON.stringify(account),
    });
  }

  async updateAccount(accountId: string, account: Account): Promise<ApiResponse<Account>> {
    return this.request<Account>(`/api/accounts/${accountId}`, {
      method: 'PUT',
      body: JSON.stringify(account),
    });
  }

  async deleteAccount(accountId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/accounts/${accountId}`, {
      method: 'DELETE',
    });
  }

  async getCards(): Promise<ApiResponse<Card[]>> {
    return this.request<Card[]>('/api/cards');
  }

  async getCard(cardNumber: string): Promise<ApiResponse<Card>> {
    return this.request<Card>(`/api/cards/${cardNumber}`);
  }

  async getCardsByAccount(accountId: string): Promise<ApiResponse<Card[]>> {
    return this.request<Card[]>(`/api/cards/account/${accountId}`);
  }

  async createCard(card: Card): Promise<ApiResponse<Card>> {
    return this.request<Card>('/api/cards', {
      method: 'POST',
      body: JSON.stringify(card),
    });
  }

  async updateCard(cardNumber: string, card: Card): Promise<ApiResponse<Card>> {
    return this.request<Card>(`/api/cards/${cardNumber}`, {
      method: 'PUT',
      body: JSON.stringify(card),
    });
  }

  async deleteCard(cardNumber: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/cards/${cardNumber}`, {
      method: 'DELETE',
    });
  }

  async getTransactions(page = 0, size = 10): Promise<ApiResponse<PageResponse<Transaction>>> {
    return this.request<PageResponse<Transaction>>(`/api/transactions?page=${page}&size=${size}`);
  }

  async getTransaction(transactionId: string): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>(`/api/transactions/${transactionId}`);
  }

  async getTransactionsByCard(cardNumber: string): Promise<ApiResponse<Transaction[]>> {
    return this.request<Transaction[]>(`/api/transactions/card/${cardNumber}`);
  }

  async createTransaction(transaction: TransactionRequest): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>('/api/transactions', {
      method: 'POST',
      body: JSON.stringify(transaction),
    });
  }

  async processBillPayment(request: BillPaymentRequest): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>('/api/transactions/bill-payment', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getUsers(): Promise<ApiResponse<User[]>> {
    return this.request<User[]>('/api/users');
  }

  async getUser(userId: string): Promise<ApiResponse<User>> {
    return this.request<User>(`/api/users/${userId}`);
  }

  async updateUser(userId: string, user: User): Promise<ApiResponse<User>> {
    return this.request<User>(`/api/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(user),
    });
  }

  async deleteUser(userId: string): Promise<ApiResponse<void>> {
    return this.request<void>(`/api/users/${userId}`, {
      method: 'DELETE',
    });
  }
}

export const api = new ApiService();
