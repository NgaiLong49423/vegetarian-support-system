import { Link } from 'react-router-dom';

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <Link to="/" aria-label="Mâm Xanh · Trang chủ" className="flex shrink-0 items-center gap-2 whitespace-nowrap">
      <span className="relative block h-8 w-8 shrink-0 overflow-hidden rounded-full" aria-hidden="true">
        <img src="/logo.png" alt="" className="absolute -left-4 -top-[10px] h-16 w-16 max-w-none" />
      </span>
      {!compact && (
        <span className="text-lg font-semibold tracking-tight text-ink">Mâm Xanh</span>
      )}
    </Link>
  );
}
