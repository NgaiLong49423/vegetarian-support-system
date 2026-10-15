import { LoaderCircle, Sparkles } from 'lucide-react';
import type { RecipeAiContentType } from '../api/recipes';

interface RecipeAiAssistanceProps {
  contentType: RecipeAiContentType;
  loading: RecipeAiContentType | null;
  error: string;
  onGenerate: (contentType: RecipeAiContentType) => void;
}

export function RecipeAiAssistance({ contentType, loading, error, onGenerate }: RecipeAiAssistanceProps) {
  const isDescription = contentType === 'DESCRIPTION';
  const label = isDescription ? 'AI gợi ý viết mô tả' : 'AI gợi ý viết hướng dẫn chế biến';
  const loadingLabel = isDescription ? 'Đang viết mô tả…' : 'Đang viết hướng dẫn…';
  const isLoading = loading === contentType;

  return (
    <div className="mt-2 space-y-2">
      <button
        type="button"
        disabled={loading !== null}
        onClick={() => onGenerate(contentType)}
        aria-label={label}
        aria-busy={isLoading}
        className="inline-flex min-h-9 items-center gap-2 rounded-lg border border-brand-200 bg-raised px-3 py-2 text-sm font-semibold text-brand-500 transition-colors hover:border-brand-400 hover:bg-brand-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-wait disabled:opacity-70"
      >
        {isLoading ? <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" /> : <Sparkles className="h-4 w-4" aria-hidden="true" />}
        {isLoading ? loadingLabel : label}
      </button>
      {isLoading && <p role="status" aria-live="polite" className="text-sm text-ink-muted">Đang tạo nội dung, có thể mất đến 45 giây.</p>}
      {error && <p role="alert" className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm leading-5 text-amber-950">{error}</p>}
    </div>
  );
}
