import type { ReactNode } from 'react';
import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

export function Modal({
  open,
  onClose,
  title,
  children,
  size = 'md',
  tone = 'default',
  trapFocus = false,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  size?: 'md' | 'lg';
  tone?: 'default' | 'dark';
  trapFocus?: boolean;
}) {
  const panel = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if (e.key !== 'Tab' || !trapFocus || !panel.current) return;
      const controls = Array.from(panel.current.querySelectorAll<HTMLElement>(
        'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]',
      )).filter((element) => element.getClientRects().length > 0);
      const first = controls[0], last = controls[controls.length - 1];
      if (!first) { e.preventDefault(); panel.current.focus(); return; }
      if (!panel.current.contains(document.activeElement) || (e.shiftKey && document.activeElement === first)) {
        e.preventDefault(); (e.shiftKey ? last : first).focus();
      } else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
    };
    window.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose, trapFocus]);

  if (!open) return null;

  return createPortal((
    <div className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-4">
      <button
        type="button"
        aria-label="Đóng nền"
        className="modal-backdrop-enter absolute inset-0 h-full w-full bg-black/70 backdrop-blur-sm cursor-default"
        onClick={onClose}
      />
      <div
        ref={panel}
        tabIndex={-1}
        className={`modal-panel-enter relative w-full ${size === 'lg' ? 'sm:max-w-2xl' : 'sm:max-w-md'} max-h-[92dvh] overflow-y-auto rounded-t-3xl border p-5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] shadow-2xl sm:max-h-[90vh] sm:rounded-2xl sm:p-6 ${tone === 'dark' ? 'border-white/10 bg-[#191919] text-white' : 'border-brand-100 bg-paper'}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <div
          aria-hidden="true"
          className="modal-drag-pill mx-auto mb-3 h-1.5 w-11 rounded-full sm:hidden"
        />
        <div className="mb-4 flex items-start justify-between gap-4">
          <h3 className={`text-lg font-extrabold ${tone === 'dark' ? 'text-white' : 'text-ink'}`}>{title}</h3>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl transition-colors sm:h-8 sm:w-8 sm:rounded-lg ${tone === 'dark' ? 'text-white/75 hover:bg-white/10 hover:text-white' : 'text-ink-muted hover:bg-brand-100'}`}
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  ), document.body);
}
