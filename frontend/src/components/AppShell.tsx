import { NavLink, useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '../auth/store';
import { cn } from '../lib/cn';

const navItems = [
  { to: '/', label: 'Overview', end: true },
  { to: '/transfer', label: 'Transfer' },
  { to: '/assistant', label: 'Assistant' },
];

export function AppShell({ children }: { children: ReactNode }) {
  const username = useAuthStore((s) => s.username);
  const clear = useAuthStore((s) => s.clear);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const signOut = () => {
    clear();
    queryClient.clear();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex h-14 max-w-5xl items-center gap-6 px-6">
          <span className="text-sm font-semibold tracking-tight">Virtual Bank</span>
          <nav className="flex items-center gap-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    'rounded-md px-3 py-1.5 text-sm transition-colors',
                    isActive
                      ? 'bg-accent-soft text-accent'
                      : 'text-muted hover:text-ink',
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-3">
            {username && (
              <span className="hidden text-sm text-muted sm:inline">{username}</span>
            )}
            <button
              onClick={signOut}
              className="rounded-md px-3 py-1.5 text-sm text-muted transition-colors hover:text-ink"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-10">{children}</main>
    </div>
  );
}
