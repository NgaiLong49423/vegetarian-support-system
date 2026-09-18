import type { ReactNode } from 'react';

/* --------------------------------------------------------------------------
 * Small reusable design-system primitives shared across pages.
 * ------------------------------------------------------------------------ */

type Tone = 'brand' | 'leaf' | 'neutral';

export function Badge({
  children,
  tone = 'brand',
  soft = true,
  className = '',
}: {
  children: ReactNode;
  tone?: Tone;
  soft?: boolean;
  className?: string;
}) {
  const tones: Record<Tone, string> = soft
    ? {
        brand: 'bg-brand-100 text-brand-700 border-brand-200',
        leaf: 'bg-leaf-100 text-leaf-700 border-leaf-100',
        neutral: 'bg-brand-50 text-ink-soft border-brand-100',
      }
    : {
        brand: 'bg-brand-600 text-white border-brand-600',
        leaf: 'bg-leaf-600 text-white border-leaf-600',
        neutral: 'bg-ink text-white border-ink',
      };
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold whitespace-nowrap ${tones[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline';

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  className = '',
  as = 'button',
  ...rest
}: {
  children: ReactNode;
  variant?: ButtonVariant;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  as?: 'button' | 'span';
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  const variants: Record<ButtonVariant, string> = {
    primary:
      'bg-brand-600 text-white shadow-sm shadow-brand-600/25 hover:bg-brand-700 active:scale-[.98]',
    secondary: 'bg-brand-100 text-brand-700 hover:bg-brand-200 active:scale-[.98]',
    ghost: 'text-ink-soft hover:bg-brand-100/60',
    outline:
      'border border-brand-200 bg-white text-ink-soft hover:border-brand-300 hover:text-brand-700',
  };
  const sizes = {
    sm: 'px-3 py-1.5 text-sm gap-1.5',
    md: 'px-5 py-2.5 text-sm gap-2',
    lg: 'px-6 py-3 text-base gap-2',
  };
  const cls = `inline-flex items-center justify-center rounded-xl font-semibold transition-all disabled:opacity-50 disabled:pointer-events-none ${variants[variant]} ${sizes[size]} ${className}`;
  if (as === 'span') return <span className={cls}>{children}</span>;
  return (
    <button className={cls} {...rest}>
      {children}
    </button>
  );
}

export function Card({
  children,
  className = '',
  hover = false,
}: {
  children: ReactNode;
  className?: string;
  hover?: boolean;
}) {
  return (
    <div
      className={`rounded-2xl border border-brand-100 bg-white ${
        hover ? 'transition-all hover:-translate-y-1 hover:shadow-xl hover:shadow-brand-900/5 hover:border-brand-200' : ''
      } ${className}`}
    >
      {children}
    </div>
  );
}

export function SectionHeading({
  eyebrow,
  title,
  action,
}: {
  eyebrow?: string;
  title: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="mb-6 flex items-end justify-between gap-4">
      <div>
        {eyebrow && (
          <p className="mb-1 text-xs font-bold uppercase tracking-[0.15em] text-brand-600">
            {eyebrow}
          </p>
        )}
        <h2 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">{title}</h2>
      </div>
      {action}
    </div>
  );
}

export function EmptyState({
  icon = '🌱',
  title,
  description,
  action,
}: {
  icon?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-brand-200 bg-brand-50/50 px-6 py-16 text-center">
      <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-brand-100 text-2xl">
        {icon}
      </div>
      <h3 className="mb-1 text-lg font-bold text-ink">{title}</h3>
      {description && <p className="max-w-sm text-sm text-ink-muted">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function Stat({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-brand-100 bg-white px-4 py-3">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-100 text-brand-600">
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-xs text-ink-muted">{label}</p>
        <p className="truncate font-bold text-ink">{value}</p>
      </div>
    </div>
  );
}

export function ProgressBar({ pct, tone = 'brand' }: { pct: number; tone?: 'brand' | 'leaf' | 'amber' }) {
  const colors = {
    brand: 'bg-brand-500',
    leaf: 'bg-leaf-500',
    amber: 'bg-amber-400',
  };
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-brand-100">
      <div
        className={`h-full rounded-full ${colors[tone]} transition-all`}
        style={{ width: `${Math.min(100, pct)}%` }}
      />
    </div>
  );
}
