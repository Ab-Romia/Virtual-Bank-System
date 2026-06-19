import { useState } from 'react';
import type { Account } from '../api/types';
import { Button, ErrorNote, Field, Input, Select } from './ui';
import { formatMoney, maskAccountNumber } from '../lib/format';

export interface TransferFormValues {
  fromAccountId: string;
  toAccountId: string;
  amount: string;
  currency: string;
}

/**
 * The transfer form. It owns only input state; submitting hands clean values up
 * to the parent, which generates the Idempotency-Key and calls the API. Keeping
 * the form pure makes it straightforward to test in isolation.
 */
export function TransferForm({
  accounts,
  onSubmit,
  submitting,
  error,
}: {
  accounts: Account[];
  onSubmit: (values: TransferFormValues) => void;
  submitting: boolean;
  error?: string;
}) {
  const [fromAccountId, setFromAccountId] = useState(accounts[0]?.id ?? '');
  const [toAccountId, setToAccountId] = useState('');
  const [amount, setAmount] = useState('');

  const from = accounts.find((a) => a.id === fromAccountId);
  const currency = from?.currency ?? 'USD';

  // Destinations the user owns (other than the source), offered as a convenience.
  const ownDestinations = accounts.filter((a) => a.id !== fromAccountId);

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    onSubmit({ fromAccountId, toAccountId: toAccountId.trim(), amount, currency });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-5" aria-label="Transfer form">
      <Field label="From account" htmlFor="from-account">
        <Select
          id="from-account"
          value={fromAccountId}
          onChange={(e) => setFromAccountId(e.target.value)}
          required
        >
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.type} {maskAccountNumber(account.accountNumber)} (
              {formatMoney(account.balance, account.currency)})
            </option>
          ))}
        </Select>
      </Field>

      <Field
        label="To account"
        htmlFor="to-account"
        hint="Paste any account id, or pick one of your own below."
      >
        <Input
          id="to-account"
          value={toAccountId}
          onChange={(e) => setToAccountId(e.target.value)}
          placeholder="Destination account id"
          required
        />
      </Field>

      {ownDestinations.length > 0 && (
        <Field label="Or one of your accounts" htmlFor="to-account-own">
          <Select
            id="to-account-own"
            value=""
            onChange={(e) => e.target.value && setToAccountId(e.target.value)}
          >
            <option value="">Select an account</option>
            {ownDestinations.map((account) => (
              <option key={account.id} value={account.id}>
                {account.type} {maskAccountNumber(account.accountNumber)}
              </option>
            ))}
          </Select>
        </Field>
      )}

      <Field label={`Amount (${currency})`} htmlFor="amount">
        <Input
          id="amount"
          type="number"
          inputMode="decimal"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
        />
      </Field>

      {error && <ErrorNote>{error}</ErrorNote>}

      <Button type="submit" loading={submitting}>
        Send transfer
      </Button>
    </form>
  );
}
