import axios, { AxiosError } from 'axios';
import { useAuthStore } from '../auth/store';
import type { ProblemDetail } from './types';

// Single base URL: everything goes through the gateway. Override with
// VITE_API_BASE for other environments.
const baseURL = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api';

export const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

// Attach the bearer token to every request from the auth store.
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On a 401 the session is no longer valid: clear it and send the user to login.
// We guard against a redirect loop when the failing call is itself login.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;
    const url = error.config?.url ?? '';
    const isAuthCall = url.includes('/auth/');
    if (status === 401 && !isAuthCall) {
      useAuthStore.getState().clear();
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

/**
 * Pull a human-readable message out of any failure. The API returns RFC 9457
 * ProblemDetail JSON, so we surface `detail` when present and fall back to a
 * plain message otherwise.
 */
export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const problem = error.response?.data as ProblemDetail | undefined;
    if (problem?.detail) return problem.detail;
    if (problem?.title) return problem.title;
    if (error.message) return error.message;
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}
