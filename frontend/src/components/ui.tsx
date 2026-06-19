import { forwardRef } from 'react';
import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
} from 'react';
import { cn } from '../lib/cn';

/* Shared, deliberately small set of primitives. One accent, quiet hovers,
   visible focus rings, real disabled states. */

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost';
  loading?: boolean;
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', loading, className, children, disabled, ...props }, ref) => {
    const base =
      'inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50';
    const styles = {
      primary: 'bg-accent text-white hover:bg-accent-hover',
      secondary:
        'border border-line-strong bg-surface text-ink hover:bg-paper',
      ghost: 'text-accent hover:bg-accent-soft',
    } as const;
    return (
      <button
        ref={ref}
        className={cn(base, styles[variant], className)}
        disabled={disabled || loading}
        {...props}
      >
        {loading && <Spinner className="h-4 w-4" decorative />}
        {children}
      </button>
    );
  },
);
Button.displayName = 'Button';

type FieldProps = {
  label: string;
  htmlFor: string;
  hint?: string;
  children: ReactNode;
};

export function Field({ label, htmlFor, hint, children }: FieldProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-ink">
        {label}
      </label>
      {children}
      {hint && <p className="text-xs text-muted">{hint}</p>}
    </div>
  );
}

const fieldStyles =
  'w-full rounded-md border border-line-strong bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted focus:border-accent focus:outline-none disabled:opacity-50';

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input ref={ref} className={cn(fieldStyles, className)} {...props} />
  ),
);
Input.displayName = 'Input';

export const Select = forwardRef<
  HTMLSelectElement,
  SelectHTMLAttributes<HTMLSelectElement>
>(({ className, children, ...props }, ref) => (
  <select ref={ref} className={cn(fieldStyles, 'appearance-none', className)} {...props}>
    {children}
  </select>
));
Select.displayName = 'Select';

export function Card({
  className,
  children,
}: {
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={cn('rounded-lg border border-line bg-surface', className)}>
      {children}
    </div>
  );
}

export function Spinner({
  className,
  decorative,
}: {
  className?: string;
  // Inside a button the text already conveys state, so the spinner is hidden
  // from assistive tech to keep the button's accessible name clean.
  decorative?: boolean;
}) {
  return (
    <span
      role={decorative ? undefined : 'status'}
      aria-label={decorative ? undefined : 'Loading'}
      aria-hidden={decorative || undefined}
      className={cn(
        'inline-block animate-spin rounded-full border-2 border-current border-t-transparent',
        className ?? 'h-5 w-5',
      )}
    />
  );
}

export function ErrorNote({ children }: { children: ReactNode }) {
  if (!children) return null;
  return (
    <p
      role="alert"
      className="rounded-md border border-danger/30 bg-danger-soft px-3 py-2 text-sm text-danger"
    >
      {children}
    </p>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-line-strong px-6 py-12 text-center">
      <p className="text-sm font-medium text-ink">{title}</p>
      {description && <p className="max-w-sm text-sm text-muted">{description}</p>}
      {action}
    </div>
  );
}

export function Modal({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/30 px-6"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="w-full max-w-md rounded-lg border border-line bg-surface p-6 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-semibold">{title}</h2>
          <button
            onClick={onClose}
            aria-label="Close"
            className="rounded-md px-2 text-muted transition-colors hover:text-ink"
          >
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

const statusStyles: Record<string, string> = {
  ACTIVE: 'bg-accent-soft text-accent',
  COMPLETED: 'bg-accent-soft text-accent',
  PENDING: 'bg-amber-50 text-warn',
  FROZEN: 'bg-paper text-muted',
  FAILED: 'bg-danger-soft text-danger',
};

export function StatusBadge({ status }: { status: string }) {
  const style = statusStyles[status] ?? 'bg-paper text-muted';
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium tracking-wide',
        style,
      )}
    >
      {status}
    </span>
  );
}
