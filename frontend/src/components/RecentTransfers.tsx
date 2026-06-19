import { errorMessage } from '../api/client';
import { useTransfers } from '../api/hooks';
import { formatDate, formatMoney, maskAccountNumber } from '../lib/format';
import { EmptyState, ErrorNote, Spinner, StatusBadge } from './ui';

export function RecentTransfers() {
  const { data, isLoading, isError, error } = useTransfers();

  if (isLoading) {
    return (
      <div className="flex justify-center py-10 text-muted">
        <Spinner />
      </div>
    );
  }

  if (isError) {
    return <ErrorNote>{errorMessage(error, 'Could not load transfers')}</ErrorNote>;
  }

  if (!data || data.length === 0) {
    return (
      <EmptyState
        title="No transfers yet"
        description="Transfers you make will appear here."
      />
    );
  }

  // Newest first.
  const sorted = [...data].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  );

  return (
    <div className="overflow-hidden rounded-lg border border-line">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-line bg-paper text-left text-xs uppercase tracking-wide text-muted">
            <th className="px-4 py-2.5 font-medium">Date</th>
            <th className="px-4 py-2.5 font-medium">From</th>
            <th className="px-4 py-2.5 font-medium">To</th>
            <th className="px-4 py-2.5 text-right font-medium">Amount</th>
            <th className="px-4 py-2.5 font-medium">Status</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((t) => (
            <tr key={t.transferId} className="border-b border-line last:border-0">
              <td className="px-4 py-3 text-muted">{formatDate(t.createdAt)}</td>
              <td className="px-4 py-3 font-mono text-xs">
                {maskAccountNumber(t.fromAccountId)}
              </td>
              <td className="px-4 py-3 font-mono text-xs">
                {maskAccountNumber(t.toAccountId)}
              </td>
              <td className="tabular px-4 py-3 text-right font-medium">
                {formatMoney(t.amount, t.currency)}
              </td>
              <td className="px-4 py-3">
                <StatusBadge status={t.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
