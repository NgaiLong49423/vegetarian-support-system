import { ThumbsUp } from 'lucide-react';

export function RecipeReactionBadge({ percentage }: { percentage: number | null }) {
  return <span aria-label={percentage == null ? 'Chưa có bình chọn' : `Tỷ lệ Like: ${percentage}%`}
    className="inline-flex items-center gap-1.5 rounded-full border border-brand-100 bg-white px-3 py-1.5 text-sm font-bold text-brand-700 shadow-sm">
    {percentage == null ? 'Mới' : <><ThumbsUp aria-hidden="true" className="h-4 w-4" />{percentage}%</>}
  </span>;
}
