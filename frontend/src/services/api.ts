import axios, { AxiosError } from 'axios';
import type {
  UserRegistrationRequest,
  UserRegistrationResponse,
  UserLoginRequest,
  UserLoginResponse,
  UserProfileResponse,
  CreateAccountRequest,
  CreateAccountResponse,
  UserAccountsResponse,
  AccountResponse,
  TransferInitiationRequest,
  TransferInitiationResponse,
  TransferExecutionRequest,
  TransferExecutionResponse,
  TransactionsResponse,
  DashboardResponse,
  ErrorResponse,
} from '../types/api';

// API Base URLs
const BFF_BASE_URL = 'http://localhost:8080/bff';
const USER_BASE_URL = 'http://localhost:8081/users';
const ACCOUNT_BASE_URL = 'http://localhost:8082';
const TRANSACTION_BASE_URL = 'http://localhost:8083';

// Create axios instances
const bffApi = axios.create({
  baseURL: BFF_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'APP-NAME': 'PORTAL',
  },
});

const userApi = axios.create({
  baseURL: USER_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

const accountApi = axios.create({
  baseURL: ACCOUNT_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

const transactionApi = axios.create({
  baseURL: TRANSACTION_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Error handler
export const handleApiError = (error: unknown): ErrorResponse => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ErrorResponse>;
    if (axiosError.response?.data) {
      return axiosError.response.data;
    }
    return {
      status: axiosError.response?.status || 500,
      error: 'Network Error',
      message: axiosError.message || 'An unexpected error occurred',
    };
  }
  return {
    status: 500,
    error: 'Unknown Error',
    message: 'An unexpected error occurred',
  };
};

// User Service APIs
export const userService = {
  register: async (data: UserRegistrationRequest): Promise<UserRegistrationResponse> => {
    const response = await userApi.post<UserRegistrationResponse>('/register', data);
    return response.data;
  },

  login: async (data: UserLoginRequest): Promise<UserLoginResponse> => {
    const response = await userApi.post<UserLoginResponse>('/login', data);
    return response.data;
  },

  getProfile: async (userId: string): Promise<UserProfileResponse> => {
    const response = await userApi.get<UserProfileResponse>(`/${userId}/profile`);
    return response.data;
  },
};

// Account Service APIs
export const accountService = {
  createAccount: async (data: CreateAccountRequest): Promise<CreateAccountResponse> => {
    const response = await accountApi.post<CreateAccountResponse>('/accounts', data);
    return response.data;
  },

  getAccount: async (accountId: string): Promise<AccountResponse> => {
    const response = await accountApi.get<AccountResponse>(`/accounts/${accountId}`);
    return response.data;
  },

  getUserAccounts: async (userId: string): Promise<UserAccountsResponse[]> => {
    const response = await accountApi.get<UserAccountsResponse[]>(`/users/${userId}/accounts`);
    return response.data;
  },

  getAccountByNumber: async (accountNumber: string): Promise<AccountResponse> => {
    const response = await accountApi.get<AccountResponse>(`/accounts/number/${accountNumber}`);
    return response.data;
  },
};

// Transaction Service APIs
export const transactionService = {
  initiateTransfer: async (data: TransferInitiationRequest): Promise<TransferInitiationResponse> => {
    const response = await transactionApi.post<TransferInitiationResponse>(
      '/transactions/transfer/initiation',
      data
    );
    return response.data;
  },

  executeTransfer: async (data: TransferExecutionRequest): Promise<TransferExecutionResponse> => {
    const response = await transactionApi.post<TransferExecutionResponse>(
      '/transactions/transfer/execution',
      data
    );
    return response.data;
  },

  getAccountTransactions: async (accountId: string): Promise<TransactionsResponse[]> => {
    const response = await transactionApi.get<TransactionsResponse[]>(
      `/accounts/${accountId}/transactions`
    );
    return response.data;
  },
};

// BFF Service APIs
export const bffService = {
  getDashboard: async (userId: string): Promise<DashboardResponse> => {
    const response = await bffApi.get<DashboardResponse>(`/dashboard/${userId}`);
    return response.data;
  },
};
