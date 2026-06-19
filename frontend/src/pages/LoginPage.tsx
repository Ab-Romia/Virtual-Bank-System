import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { auth } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useAuthStore } from '../auth/store';
import { Button, ErrorNote, Field, Input } from '../components/ui';

type Mode = 'login' | 'register';

export function LoginPage() {
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');

  const setSession = useAuthStore((s) => s.setSession);
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/';

  const login = useMutation({
    mutationFn: () => auth.login({ username, password }),
    onSuccess: (data) => {
      setSession({
        token: data.accessToken,
        userId: data.userId,
        username: data.username,
      });
      navigate(redirectTo, { replace: true });
    },
  });

  const register = useMutation({
    mutationFn: () => auth.register({ username, email, password, fullName }),
    // After registering, sign in straight away with the same credentials.
    onSuccess: () => login.mutate(),
  });

  const pending = login.isPending || register.isPending;
  const error = mode === 'login' ? login.error : register.error;

  const onSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (mode === 'login') login.mutate();
    else register.mutate();
  };

  const switchMode = (next: Mode) => {
    setMode(next);
    login.reset();
    register.reset();
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-lg font-semibold tracking-tight">Virtual Bank</h1>
          <p className="mt-1 text-sm text-muted">
            {mode === 'login' ? 'Sign in to your account' : 'Create your account'}
          </p>
        </div>

        <form onSubmit={onSubmit} className="space-y-4">
          <Field label="Username" htmlFor="username">
            <Input
              id="username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </Field>

          {mode === 'register' && (
            <>
              <Field label="Full name" htmlFor="fullName">
                <Input
                  id="fullName"
                  autoComplete="name"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  required
                />
              </Field>
              <Field label="Email" htmlFor="email">
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </Field>
            </>
          )}

          <Field label="Password" htmlFor="password">
            <Input
              id="password"
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </Field>

          {error && <ErrorNote>{errorMessage(error, 'Could not sign in')}</ErrorNote>}

          <Button type="submit" loading={pending} className="w-full">
            {mode === 'login' ? 'Sign in' : 'Create account'}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted">
          {mode === 'login' ? "Don't have an account?" : 'Already have an account?'}{' '}
          <button
            type="button"
            onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}
            className="font-medium text-accent hover:underline"
          >
            {mode === 'login' ? 'Create one' : 'Sign in'}
          </button>
        </p>
      </div>
    </div>
  );
}
