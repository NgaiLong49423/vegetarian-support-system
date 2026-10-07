import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Clock, Search, Users, X } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
import { apiClient } from '../lib/apiClient';
import type { RecipePost } from '../api/recipes';

type RecipePage = { items: RecipePost[]; page: number; size: number; totalElements: number; totalPages: number };

export function Explore() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState(params.get('q') ?? '');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<RecipePage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError(false);
      apiClient.get<RecipePage>('/recipes', { params: { keyword: query.trim(), page, size: 12 } })
        .then(({ data }) => setResult(data))
        .catch(() => { setResult(null); setError(true); })
        .finally(() => setLoading(false));
    }, query ? 250 : 0);
    return () => window.clearTimeout(timer);
  }, [query, page, retry]);

  const updateQuery = (value: string) => {
    setQuery(value);
    setPage(0);
    setParams(value ? { q: value } : {}, { replace: true });
  };

  return <PageContainer className="py-8">
    <div className="mb-6">
      <p className="mb-1 text-xs font-bold uppercase tracking-[0.15em] text-brand-600">Khám phá</p>
      <h1 className="text-3xl font-extrabold tracking-tight text-ink">Khám phá công thức chay</h1>
      <p className="mt-1 text-ink-muted">Công thức đã xuất bản từ Backend Mâm Xanh.</p>
    </div>
    <div className="mb-6 flex items-center gap-2 rounded-xl border border-brand-200 bg-white px-3">
      <Search className="h-5 w-5 shrink-0 text-ink-muted" />
      <input value={query} onChange={(event) => updateQuery(event.target.value)} placeholder="Tìm món chay hoặc nguyên liệu..." className="w-full bg-transparent py-2.5 text-sm outline-none placeholder:text-ink-muted" />
      {query && <button onClick={() => updateQuery('')} aria-label="Xóa từ khóa" className="text-ink-muted hover:text-brand-600"><X className="h-4 w-4" /></button>}
    </div>
    {loading ? <p role="status" className="py-12 text-center text-sm text-ink-muted">Đang tải công thức...</p>
      : error ? <EmptyState title="Không tải được công thức" description="Không thể kết nối Backend. Kiểm tra Backend rồi thử lại." action={<Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} />
      : result && result.items.length > 0 ? <>
        <p className="mb-4 text-sm text-ink-muted"><strong className="text-ink">{result.totalElements}</strong> công thức</p>
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{result.items.map((recipe) => {
          const image = recipe.media.find((item) => item.cover) ?? recipe.media[0];
          return <Link key={recipe.id} to={`/cong-thuc/id/${recipe.id}`} className="group overflow-hidden rounded-2xl border border-brand-100 bg-white transition-all hover:-translate-y-1 hover:shadow-xl">
            <div className="aspect-[4/3] bg-brand-50">{image ? <img src={image.url} alt={recipe.title} className="h-full w-full object-cover transition-transform group-hover:scale-105" /> : <div className="flex h-full items-center justify-center text-sm text-ink-muted">Ảnh công thức</div>}</div>
            <div className="p-4"><div className="flex flex-wrap gap-2 text-xs font-semibold text-brand-700"><span className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.vegetarianType}</span><span className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.dishCategory}</span></div><h2 className="mt-3 line-clamp-2 font-bold text-ink group-hover:text-brand-700">{recipe.title}</h2><p className="mt-2 flex items-center gap-4 text-xs text-ink-muted"><span className="inline-flex items-center gap-1"><Clock className="h-3.5 w-3.5" />{recipe.prepTimeMin + recipe.cookTimeMin} phút</span><span className="inline-flex items-center gap-1"><Users className="h-3.5 w-3.5" />{recipe.servings} khẩu phần</span></p><p className="mt-3 border-t border-brand-50 pt-3 text-xs text-ink-soft">Tác giả: {recipe.authorName}</p></div>
          </Link>;
        })}</div>
        {result.totalPages > 1 && <div className="mt-8 flex items-center justify-center gap-4"><Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button><span className="text-sm text-ink-muted">Trang {page + 1}/{result.totalPages}</span><Button variant="outline" disabled={page + 1 >= result.totalPages} onClick={() => setPage((current) => current + 1)}>Trang sau</Button></div>}
      </> : <EmptyState title="Chưa có công thức phù hợp" description="Thử một từ khóa khác hoặc quay lại sau khi chuyên gia xuất bản công thức." />}
  </PageContainer>;
}
