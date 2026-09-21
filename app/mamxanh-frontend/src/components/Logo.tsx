import { Link } from 'react-router-dom';

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <Link to="/" className="flex shrink-0 items-center gap-2.5 whitespace-nowrap">
      <span className="relative block h-10 w-10 shrink-0 overflow-hidden rounded-full" aria-hidden="true">
        <img src="/logo.png" alt="" className="absolute -left-5 -top-[13px] h-20 w-20 max-w-none" />
      </span>
      {!compact && (
        <span className="leading-tight">
          <span className="block text-lg font-extrabold tracking-tight text-leaf-700">Mâm Xanh</span>
          <span className="block text-[10px] font-bold uppercase tracking-[0.12em] text-brand-600">
            Ẩm thực thuần lành
          </span>
        </span>
      )}
    </Link>
  );
}
