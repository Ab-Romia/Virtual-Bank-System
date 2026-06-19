import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { transfers } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { queryKeys, useAccounts } from '../api/hooks';
import { uuid } from '../lib/uuid';
import { TransferForm } from '../components/TransferForm';
import type { TransferFormValues } from '../components/TransferForm';
import { TransferProgress } from '../components/TransferProgress';
import { Card, EmptyState, ErrorNote, Spinner } from '../components/ui';
import { Link } from 'react-router-dom';

export function TransferPage() {
  const accountsQuery = useAccounts();
  const queryClient = useQueryClient();
  const [transferId, setTransferId] = useState<string | null>(null);

  const submit = useMutation({
    mutationFn: (values: TransferFormValues) =>
      transfers.create(values, uuid()),
    onSuccess: (accepted) => {
      setTransferId(accepted.transferId);
      // The new transfer should show up in the dashboard list once it settles.
      queryClient.invalidateQueries({ queryKey: queryKeys.transfers });
    },
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Transfer money</h1>
        <p className="mt-1 text-sm text-muted">
          Move funds between accounts. The transfer is followed to its outcome.
        </p>
      </div>

      <div className="grid gap-8 lg:grid-cols-2">
        <Card className="p-6">
          {accountsQuery.isLoading && (
            <div className="flex justify-center py-8 text-muted">
              <Spinner />
            </div>
          )}

          {accountsQuery.isError && (
            <ErrorNote>{errorMessage(accountsQuery.error, 'Could not load accounts')}</ErrorNote>
          )}

          {accountsQuery.data && accountsQuery.data.length === 0 && (
            <EmptyState
              title="No accounts to transfer from"
              description="Open an account first."
              action={
                <Link to="/" className="text-sm font-medium text-accent hover:underline">
                  Go to accounts
                </Link>
              }
            />
          )}

          {accountsQuery.data && accountsQuery.data.length > 0 && (
            <TransferForm
              accounts={accountsQuery.data}
              submitting={submit.isPending}
              error={
                submit.isError
                  ? errorMessage(submit.error, 'Could not start transfer')
                  : undefined
              }
              onSubmit={(values) => submit.mutate(values)}
            />
          )}
        </Card>

        <div>
          {transferId ? (
            <TransferProgress key={transferId} transferId={transferId} />
          ) : (
            <EmptyState
              title="No transfer in progress"
              description="Submit the form to start a transfer and watch its status here."
            />
          )}
        </div>
      </div>
    </div>
  );
}
