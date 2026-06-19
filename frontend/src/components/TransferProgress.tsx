import { useQuery } from '@tanstack/react-query';
import { audit, transfers } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { queryKeys } from '../api/hooks';
import type { Transfer } from '../api/types';
import { Card, ErrorNote, Spinner, StatusBadge } from './ui';
import { formatDate, formatMoney } from '../lib/format';

const isTerminal = (status: Transfer['status']) =>
  status === 'COMPLETED' || status === 'FAILED';

/**
 * Follows a single transfer to its outcome. While the transfer is PENDING it
 * polls every second; once it settles the polling stops and the audit history
 * is shown.
 */
export function TransferProgress({ transferId }: { transferId: string }) {
  const transfer = useQuery({
    queryKey: queryKeys.transfer(transferId),
    queryFn: () => transfers.get(transferId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && isTerminal(status) ? false : 1000;
    },
  });

  const settled = transfer.data ? isTerminal(transfer.data.status) : false;

  const history = useQuery({
    queryKey: queryKeys.audit(transferId),
    queryFn: () => audit.forTransfer(transferId),
    enabled: settled,
  });

  return (
    <Card className="space-y-4 p-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted">Transfer</p>
          <p className="mt-1 font-mono text-sm">{transferId}</p>
        </div>
        {transfer.data && <StatusBadge status={transfer.data.status} />}
      </div>

      {transfer.isError && (
        <ErrorNote>{errorMessage(transfer.error, 'Could not read transfer')}</ErrorNote>
      )}

      {transfer.data && (
        <>
          <p className="tabular text-lg font-semibold">
            {formatMoney(transfer.data.amount, transfer.data.currency)}
          </p>

          {!settled && (
            <p className="flex items-center gap-2 text-sm text-muted">
              <Spinner className="h-4 w-4" /> Processing transfer...
            </p>
          )}

          {transfer.data.status === 'FAILED' && transfer.data.failureReason && (
            <ErrorNote>{transfer.data.failureReason}</ErrorNote>
          )}

          {settled && (
            <div>
              <p className="mb-2 text-xs font-medium uppercase tracking-wide text-muted">
                History
              </p>
              {history.isLoading && (
                <p className="text-sm text-muted">Loading history...</p>
              )}
              {history.isError && (
                <ErrorNote>
                  {errorMessage(history.error, 'Could not load history')}
                </ErrorNote>
              )}
              {history.data && history.data.length > 0 && (
                <ol className="space-y-2">
                  {history.data.map((entry, index) => (
                    <li
                      key={`${entry.eventType}-${index}`}
                      className="flex items-center justify-between border-b border-line py-1.5 text-sm last:border-0"
                    >
                      <span className="font-medium">{entry.eventType}</span>
                      <span className="text-muted">{formatDate(entry.occurredAt)}</span>
                    </li>
                  ))}
                </ol>
              )}
              {history.data && history.data.length === 0 && (
                <p className="text-sm text-muted">No history recorded.</p>
              )}
            </div>
          )}
        </>
      )}
    </Card>
  );
}
