import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// The authenticated session. We keep only what the UI needs: the bearer token
// and the identity returned at login. Persisting to localStorage survives a
// reload; a 401 from the API clears it (see the axios interceptor).
export interface AuthState {
  token: string | null;
  userId: string | null;
  username: string | null;
  setSession: (session: { token: string; userId: string; username: string }) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userId: null,
      username: null,
      setSession: ({ token, userId, username }) =>
        set({ token, userId, username }),
      clear: () => set({ token: null, userId: null, username: null }),
    }),
    { name: 'vbank-auth' },
  ),
);

export const isAuthenticated = () => Boolean(useAuthStore.getState().token);
