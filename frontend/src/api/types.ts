// Request and response shapes for the gateway API. These mirror the backend
// DTOs exactly; money fields are kept as strings because the server sends
// decimals and we never want floating-point rounding to touch a balance.

export type AccountType = 'CHECKING' | 'SAVINGS';
export type AccountStatus = 'ACTIVE' | 'FROZEN';
export type TransferStatus = 'PENDING' | 'COMPLETED' | 'FAILED';
export type AuditEventType = 'REQUESTED' | 'COMPLETED' | 'FAILED';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  userId: string;
  username: string;
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  fullName: string;
  createdAt: string;
}

export interface Account {
  id: string;
  ownerId: string;
  accountNumber: string;
  type: AccountType;
  // Decimal sent as a JSON number by the server; treated as a string for display
  // safety. Axios may parse it as a number, so callers normalise on read.
  balance: string | number;
  currency: string;
  status: AccountStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAccountRequest {
  type: AccountType;
  currency: string;
}

export interface DepositRequest {
  amount: string;
}

export interface TransferRequest {
  fromAccountId: string;
  toAccountId: string;
  amount: string;
  currency: string;
}

export interface TransferAccepted {
  transferId: string;
  status: TransferStatus;
  statusUrl: string;
}

export interface Transfer {
  transferId: string;
  fromAccountId: string;
  toAccountId: string;
  amount: string | number;
  currency: string;
  status: TransferStatus;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEntry {
  eventType: AuditEventType;
  amount: string | number | null;
  currency: string | null;
  reason: string | null;
  occurredAt: string;
}

export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  reply: string;
}

// RFC 9457 ProblemDetail. The server always sets `detail`; the rest is optional.
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
}
