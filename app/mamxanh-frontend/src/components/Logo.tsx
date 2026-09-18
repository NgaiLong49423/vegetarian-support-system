import { Link } from 'react-router-dom';

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <Link to="/" className="flex items-center gap-2.5">
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-leaf-500 to-leaf-700 shadow-sm shadow-leaf-700/30">
        <svg viewBox="0 0 24 24" className="h-6 w-6 text-white" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
          <path d="M11 20A7 7 0 0 1 4 13c0-5 4-9 9-9 4 0 7 3 7 7a7 7 0 0 1-9 9z" fill="currentColor" fillOpacity={0.25} />
          <path d="M11 20A7 7 0 0 1 4 13c0-5 4-9 9-9 4 0 7 3 7 7a7 7 0 0 1-9 9z" />
          <path d="M11 20c0-4 1-7 5-10" />
        </svg>
      </span>
      {!compact && (
        <span className="leading-tight">
          <span className="block text-lg font-extrabold tracking-tight text-leaf-700">Mâm Xanh</span>
          <span className="block text-[10px] font-bold uppercase tracking-[0.2em] text-brand-600">
            Ẩm thực thuần lành
          </span>
        </span>
      )}
    </Link>
  );
}
