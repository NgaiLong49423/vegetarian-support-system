import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

export function Modal({
  open,
  onClose,
  title,
  children,
  size = 'md',
  tone = 'default',
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  size?: 'md' | 'lg';
  tone?: 'default' | 'dark';
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;

  return createPortal((
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        type="button"
        aria-label="Đóng nền"
        className="modal-backdrop-enter absolute inset-0 h-full w-full bg-black/70 backdrop-blur-sm cursor-default"
        onClick={onClose}
      />
      <div
        className={`modal-panel-enter relative w-full ${size === 'lg' ? 'max-w-2xl' : 'max-w-md'} max-h-[90vh] overflow-y-auto rounded-2xl border p-6 shadow-2xl ${tone === 'dark' ? 'border-white/10 bg-[#191919] text-white' : 'border-brand-100 bg-paper'}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <h3 className={`text-lg font-extrabold ${tone === 'dark' ? 'text-white' : 'text-ink'}`}>{title}</h3>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className={`flex h-8 w-8 items-center justify-center rounded-lg transition-colors ${tone === 'dark' ? 'text-white/75 hover:bg-white/10 hover:text-white' : 'text-ink-muted hover:bg-brand-100'}`}
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  ), document.body);
}
