// API Types based on OpenAPI specification

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
}

// User Service Types
export interface UserRegistrationRequest {
  username: string;
  password: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface UserRegistrationResponse {
  userId: string;
  username: string;
  message: string;
}

export interface UserLoginRequest {
  username: string;
  password: string;
}

export interface UserLoginResponse {
  userId: string;
  username: string;
}

export interface UserProfileResponse {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  isActive: boolean;
}

// Account Service Types
export interface CreateAccountRequest {
  userId: string;
  accountType: 'SAVINGS' | 'CHECKING';
  initialBalance: number;
}

export interface CreateAccountResponse {
  accountId: string;
  accountNumber: string;
  message: string;
}

export interface AccountResponse {
  accountId: string;
  userId: string;
  accountNumber: string;
  accountType: string;
  balance: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserAccountsResponse {
  accountId: string;
  accountNumber: string;
  accountType: string;
  balance: number;
  status: string;
}

// Transaction Service Types
export interface TransferInitiationRequest {
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  description: string;
}

export interface TransferInitiationResponse {
  transactionId: string;
  status: string;
  timestamp: string;
}

export interface TransferExecutionRequest {
  transactionId: string;
}

export interface TransferExecutionResponse {
  transactionId: string;
  status: 'SUCCESS' | 'FAILED';
  timestamp: string;
}

export interface TransactionsResponse {
  transactionId: string;
  fromAccountId: string;
  toAccountId: string;
  amount: number;
  description: string;
  timestamp: string;
  status: string;
}

// BFF Service Types
export interface DashboardTransaction {
  transactionId: string;
  amount: number;
  fromAccountId: string;
  toAccountId: string;
  description: string;
  timestamp: string;
  status: string;
}

export interface DashboardAccount {
  accountId: string;
  accountNumber: string;
  accountType: string;
  balance: number;
  transactions: DashboardTransaction[];
}

export interface DashboardResponse {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  accounts: DashboardAccount[];
}
