import { afterEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RequireAuth } from '../auth/RequireAuth';
import { useAuthStore } from '../auth/store';

function renderAt(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<div>Login screen</div>} />
        <Route
          path="/protected"
          element={
            <RequireAuth>
              <div>Secret content</div>
            </RequireAuth>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('RequireAuth', () => {
  afterEach(() => {
    useAuthStore.getState().clear();
  });

  it('redirects to login when there is no token', () => {
    useAuthStore.getState().clear();
    renderAt('/protected');

    expect(screen.getByText('Login screen')).toBeInTheDocument();
    expect(screen.queryByText('Secret content')).not.toBeInTheDocument();
  });

  it('renders the protected content when a token is present', () => {
    useAuthStore.getState().setSession({
      token: 'a-valid-token',
      userId: 'user-1',
      username: 'romia',
    });
    renderAt('/protected');

    expect(screen.getByText('Secret content')).toBeInTheDocument();
    expect(screen.queryByText('Login screen')).not.toBeInTheDocument();
  });
});
