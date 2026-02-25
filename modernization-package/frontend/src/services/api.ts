const API_BASE = '/api/v1';

export interface TransactionSummary {
  transactionId: string;
  typeCode: string;
  categoryCode: number;
  source: string;
  description: string;
  amount: number;
  cardNumber: string;
  originationTimestamp: string;
  processingTimestamp: string;
}

export interface TransactionListResponse {
  content: TransactionSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface TransactionDetail {
  transactionId: string;
  accountId: string;
  cardNumber: string;
  typeCode: string;
  categoryCode: number;
  source: string;
  description: string;
  amount: number;
  merchantId: number;
  merchantName: string;
  merchantCity: string;
  merchantZip: string;
  originationTimestamp: string;
  processingTimestamp: string;
}

export interface AddTransactionRequest {
  accountId: string | null;
  cardNumber: string | null;
  typeCode: string;
  categoryCode: string;
  source: string;
  description: string;
  amount: string;
  originationDate: string;
  processingDate: string;
  merchantId: string;
  merchantName: string;
  merchantCity: string;
  merchantZip: string;
  confirmation: string | null;
}

export interface AddTransactionResponse {
  transactionId: string;
  message: string;
  transaction: TransactionDetail;
}

export interface ConfirmationRequiredResponse {
  confirmationRequired: boolean;
  message: string;
  resolvedAccountId: string;
  resolvedCardNumber: string;
}

export interface LatestTransactionResponse {
  transactionId: string;
  typeCode: string;
  categoryCode: number;
  source: string;
  description: string;
  amount: number;
  originationDate: string;
  processingDate: string;
  merchantId: number;
  merchantName: string;
  merchantCity: string;
  merchantZip: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  field: string | null;
  businessRule: string | null;
  phase?: number;
}

export async function listTransactions(
  page: number = 0,
  size: number = 10,
  startTransactionId?: string
): Promise<TransactionListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (startTransactionId) params.set('startTransactionId', startTransactionId);
  const response = await fetch(`${API_BASE}/transactions?${params}`);
  if (!response.ok) {
    const err: ErrorResponse = await response.json();
    throw err;
  }
  return response.json();
}

export async function viewTransaction(transactionId: string): Promise<TransactionDetail> {
  const response = await fetch(`${API_BASE}/transactions/${transactionId}`);
  if (!response.ok) {
    const err: ErrorResponse = await response.json();
    throw err;
  }
  return response.json();
}

export async function addTransaction(
  request: AddTransactionRequest
): Promise<AddTransactionResponse | ConfirmationRequiredResponse> {
  const response = await fetch(`${API_BASE}/transactions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (response.status === 201) return response.json() as Promise<AddTransactionResponse>;
  if (response.status === 200) return response.json() as Promise<ConfirmationRequiredResponse>;
  const err: ErrorResponse = await response.json();
  throw err;
}

export async function getLatestTransaction(): Promise<LatestTransactionResponse> {
  const response = await fetch(`${API_BASE}/transactions/latest`);
  if (!response.ok) {
    const err: ErrorResponse = await response.json();
    throw err;
  }
  return response.json();
}
