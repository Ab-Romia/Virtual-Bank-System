import { useState } from 'react';
import { errorMessage } from '../api/client';
import { useAccounts, useCreateAccount, useDeposit, useProfile } from '../api/hooks';
import type { Account, AccountType } from '../api/types';
import {
  Button,
  Card,
  EmptyState,
  ErrorNote,
  Field,
  Input,
  Modal,
  Select,
  Spinner,
  StatusBadge,
} from '../components/ui';
import { formatMoney, maskAccountNumber } from '../lib/format';
import { RecentTransfers } from '../components/RecentTransfers';

export function DashboardPage() {
  const profile = useProfile();
  const accountsQuery = useAccounts();
  const [openAccount, setOpenAccount] = useState(false);
  const [depositFor, setDepositFor] = useState<Account | null>(null);

  return (
    <div className="space-y-12">
      <section>
        <p className="text-sm text-muted">
          {profile.data ? `Welcome back, ${profile.data.fullName}` : ' '}
        </p>
        <div className="mt-2 flex items-center justify-between">
          <h1 className="text-xl font-semibold tracking-tight">Accounts</h1>
          <Button onClick={() => setOpenAccount(true)}>Open account</Button>
        </div>

        <div className="mt-5">
          {accountsQuery.isLoading && (
            <div className="flex justify-center py-12 text-muted">
              <Spinner />
            </div>
          )}

          {accountsQuery.isError && (
            <ErrorNote>{errorMessage(accountsQuery.error, 'Could not load accounts')}</ErrorNote>
          )}

          {accountsQuery.data && accountsQuery.data.length === 0 && (
            <EmptyState
              title="No accounts yet"
              description="Open a checking or savings account to start moving money."
              action={<Button onClick={() => setOpenAccount(true)}>Open account</Button>}
            />
          )}

          {accountsQuery.data && accountsQuery.data.length > 0 && (
            <div className="grid gap-4 sm:grid-cols-2">
              {accountsQuery.data.map((account) => (
                <AccountCard
                  key={account.id}
                  account={account}
                  onDeposit={() => setDepositFor(account)}
                />
              ))}
            </div>
          )}
        </div>
      </section>

      <section>
        <h2 className="text-base font-semibold tracking-tight">Recent transfers</h2>
        <div className="mt-4">
          <RecentTransfers />
        </div>
      </section>

      {openAccount && <OpenAccountModal onClose={() => setOpenAccount(false)} />}
      {depositFor && (
        <DepositModal account={depositFor} onClose={() => setDepositFor(null)} />
      )}
    </div>
  );
}

function AccountCard({
  account,
  onDeposit,
}: {
  account: Account;
  onDeposit: () => void;
}) {
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted">{account.type}</p>
          <p className="mt-1 font-mono text-sm text-ink">
            {maskAccountNumber(account.accountNumber)}
          </p>
        </div>
        <StatusBadge status={account.status} />
      </div>
      <p className="tabular mt-6 text-2xl font-semibold tracking-tight">
        {formatMoney(account.balance, account.currency)}
      </p>
      <div className="mt-4">
        <Button variant="secondary" onClick={onDeposit}>
          Deposit
        </Button>
      </div>
    </Card>
  );
}

function OpenAccountModal({ onClose }: { onClose: () => void }) {
  const [type, setType] = useState<AccountType>('CHECKING');
  const [currency, setCurrency] = useState('USD');
  const createAccount = useCreateAccount();

  const onSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    createAccount.mutate({ type, currency }, { onSuccess: onClose });
  };

  return (
    <Modal title="Open account" onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="Account type" htmlFor="account-type">
          <Select
            id="account-type"
            value={type}
            onChange={(e) => setType(e.target.value as AccountType)}
          >
            <option value="CHECKING">Checking</option>
            <option value="SAVINGS">Savings</option>
          </Select>
        </Field>
        <Field label="Currency" htmlFor="account-currency">
          <Select
            id="account-currency"
            value={currency}
            onChange={(e) => setCurrency(e.target.value)}
          >
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
            <option value="GBP">GBP</option>
          </Select>
        </Field>
        {createAccount.isError && (
          <ErrorNote>{errorMessage(createAccount.error, 'Could not open account')}</ErrorNote>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={createAccount.isPending}>
            Open account
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function DepositModal({ account, onClose }: { account: Account; onClose: () => void }) {
  const [amount, setAmount] = useState('');
  const deposit = useDeposit();

  const onSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    deposit.mutate({ id: account.id, amount }, { onSuccess: onClose });
  };

  return (
    <Modal title="Deposit" onClose={onClose}>
      <form onSubmit={onSubmit} className="space-y-4">
        <p className="text-sm text-muted">
          Into {account.type} {maskAccountNumber(account.accountNumber)}
        </p>
        <Field label={`Amount (${account.currency})`} htmlFor="deposit-amount">
          <Input
            id="deposit-amount"
            type="number"
            inputMode="decimal"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </Field>
        {deposit.isError && (
          <ErrorNote>{errorMessage(deposit.error, 'Could not deposit')}</ErrorNote>
        )}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={deposit.isPending}>
            Deposit
          </Button>
        </div>
      </form>
    </Modal>
  );
}
