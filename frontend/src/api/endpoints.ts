import { api } from './client';
import type {
  Account,
  AuditEntry,
  ChatResponse,
  CreateAccountRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  Transfer,
  TransferAccepted,
  TransferRequest,
  UserProfile,
} from './types';

export const auth = {
  register: (body: RegisterRequest) =>
    api.post<void>('/auth/register', body).then((r) => r.data),
  login: (body: LoginRequest) =>
    api.post<LoginResponse>('/auth/login', body).then((r) => r.data),
};

export const users = {
  me: () => api.get<UserProfile>('/users/me').then((r) => r.data),
};

export const accounts = {
  list: () => api.get<Account[]>('/accounts').then((r) => r.data),
  get: (id: string) => api.get<Account>(`/accounts/${id}`).then((r) => r.data),
  create: (body: CreateAccountRequest) =>
    api.post<Account>('/accounts', body).then((r) => r.data),
  deposit: (id: string, amount: string) =>
    api.post<Account>(`/accounts/${id}/deposit`, { amount }).then((r) => r.data),
};

export const transfers = {
  list: () => api.get<Transfer[]>('/transfers').then((r) => r.data),
  get: (id: string) => api.get<Transfer>(`/transfers/${id}`).then((r) => r.data),
  // A fresh Idempotency-Key per submission makes a retried POST resolve to the
  // same transfer instead of moving money twice.
  create: (body: TransferRequest, idempotencyKey: string) =>
    api
      .post<TransferAccepted>('/transfers', body, {
        headers: { 'Idempotency-Key': idempotencyKey },
      })
      .then((r) => r.data),
};

export const audit = {
  forTransfer: (transferId: string) =>
    api.get<AuditEntry[]>(`/audit/transfers/${transferId}`).then((r) => r.data),
};

export const assistant = {
  chat: (message: string) =>
    api.post<ChatResponse>('/assistant/chat', { message }).then((r) => r.data),
};
