/**
 * Mirrors of the backend contracts. Every field here exists in a real API response; nothing is invented.
 */

export type AccountStatus = 'ACTIVE' | 'BLOCKED';

/** `GET /api/accounts`. `balance` is a double in the official model. */
export interface Account {
  id: string;
  customerReference: string;
  status: AccountStatus;
  currency: string;
  balance: number;
}

export interface CreateAccountRequest {
  customerReference: string;
  currency: string;
  balance?: number;
  status?: AccountStatus;
}

/** `GET /api/transactions`. Amounts arrive as JSON numbers. */
export interface Transaction {
  id: string;
  accountId: string;
  amount: number;
  currency: string;
  merchantCategory: string;
  country: string;
  createdAt: string;
}

export interface CreateTransactionRequest {
  accountId: string;
  amount: number;
  currency: string;
  merchantCategory: string;
  country: string;
}

export interface Transfer {
  id: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: string;
  createdAt: string;
}

export interface TransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: string;
  country: string;
}

export interface ChatRequest {
  message: string;
  conversationId?: string;
}

export interface ChatResponse {
  conversationId: string;
  reply: string;
}

/**
 * Normalised failure. The backend returns `{status, error, code, message}`; transport failures are
 * mapped onto the same shape by the error interceptor so components only ever handle one type.
 */
export interface ApiError {
  status: number;
  code: string;
  message: string;
}

/** Transfers are recorded in the history as two legs with these categories. */
export const TRANSFER_OUT = 'TRANSFER_OUT';
export const TRANSFER_IN = 'TRANSFER_IN';
