import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  userId: string | null;
  username: string | null;
  isAuthenticated: boolean;
  login: (userId: string, username: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      userId: null,
      username: null,
      isAuthenticated: false,
      login: (userId: string, username: string) => {
        set({ userId, username, isAuthenticated: true });
      },
      logout: () => {
        set({ userId: null, username: null, isAuthenticated: false });
      },
    }),
    {
      name: 'auth-storage',
    }
  )
);
