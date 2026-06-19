import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TransferForm } from '../components/TransferForm';
import type { Account } from '../api/types';

const accounts: Account[] = [
  {
    id: 'acc-from',
    ownerId: 'user-1',
    accountNumber: '1000000001',
    type: 'CHECKING',
    balance: '500.00',
    currency: 'USD',
    status: 'ACTIVE',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 'acc-to',
    ownerId: 'user-1',
    accountNumber: '1000000002',
    type: 'SAVINGS',
    balance: '0.00',
    currency: 'USD',
    status: 'ACTIVE',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
];

describe('TransferForm', () => {
  it('submits the entered values with the source account currency', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<TransferForm accounts={accounts} onSubmit={onSubmit} submitting={false} />);

    await user.type(screen.getByLabelText('To account'), 'external-account-id');
    // A number input canonicalises its value, so 125.50 reads back as 125.5.
    await user.type(screen.getByLabelText('Amount (USD)'), '125.5');
    await user.click(screen.getByRole('button', { name: 'Send transfer' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      fromAccountId: 'acc-from',
      toAccountId: 'external-account-id',
      amount: '125.5',
      currency: 'USD',
    });
  });

  it('disables the submit button while a transfer is in flight', () => {
    render(
      <TransferForm accounts={accounts} onSubmit={vi.fn()} submitting={true} />,
    );
    expect(screen.getByRole('button', { name: 'Send transfer' })).toBeDisabled();
  });

  it('shows an error message when one is provided', () => {
    render(
      <TransferForm
        accounts={accounts}
        onSubmit={vi.fn()}
        submitting={false}
        error="Insufficient funds"
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Insufficient funds');
  });
});
