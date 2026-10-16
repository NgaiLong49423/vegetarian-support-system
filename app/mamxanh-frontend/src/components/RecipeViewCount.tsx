import { Eye } from 'lucide-react';

export function RecipeViewCount({ count }: { count: number | undefined }) {
  return <span className="inline-flex items-center gap-1.5"><Eye aria-hidden="true" className="h-4 w-4 shrink-0" />{count === undefined ? 'Chưa có dữ liệu lượt xem' : `${count.toLocaleString('vi-VN')} lượt xem`}</span>;
}
