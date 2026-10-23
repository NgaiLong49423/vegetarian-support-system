import { useEffect, useState } from 'react';
import { Award, Search, Users } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, EmptyState } from '../components/ui';
import { PageContainer } from '../components/Layout';
import { MemberAvatar } from '../components/MemberAvatar';
import { useAuth } from '../components/AuthContext';
import { discoveryApi, type ExpertDirectoryEntry, type PageResponse } from '../api/discovery';
import { membersApi } from '../api/members';

export function ExpertDirectoryPage() {
  const { account, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<PageResponse<ExpertDirectoryEntry> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError('');
      discoveryApi.searchExperts(query.trim(), page)
        .then((data) => { if (active) setResult(data); })
        .catch(() => { if (active) setError('Không tải được danh sách chuyên gia. Hãy thử lại.'); })
        .finally(() => { if (active) setLoading(false); });
    }, query.trim() ? 250 : 0);
    return () => { active = false; window.clearTimeout(timer); };
  }, [query, page, retry]);

  const toggleFollow = async (expert: ExpertDirectoryEntry) => {
    if (!isAuthenticated) {
      navigate('/dang-nhap', { state: { returnTo: '/chuyen-gia' } });
      return;
    }
    setPendingId(expert.userId);
    setActionError('');
    try {
      const status = expert.following
        ? await membersApi.unfollow(expert.userId)
        : await membersApi.follow(expert.userId);
      setResult((current) => current && ({
        ...current,
        content: current.content.map((item) => item.userId === expert.userId
          ? { ...item, following: status.following, followerCount: status.followerCount }
          : item),
      }));
    } catch {
      setActionError('Không thể cập nhật theo dõi lúc này. Trạng thái vẫn theo dữ liệu máy chủ.');
    } finally {
      setPendingId(null);
    }
  };

  const canFollow = account?.role === 'CUSTOMER' || account?.role === 'EXPERT';

  return <PageContainer className="py-8 sm:py-10">
    <header className="mb-7 max-w-3xl">
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-700">Mâm Xanh / Khám phá</p>
      <div className="mt-2 flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-brand-100 text-brand-700"><Award className="h-5 w-5" /></span>
        <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Chuyên gia ẩm thực chay</h1>
      </div>
      <p className="mt-3 text-sm leading-6 text-ink-muted">Tìm hiểu hồ sơ công khai và theo dõi những người chia sẻ công thức bạn yêu thích.</p>
      <label className="mt-5 flex max-w-xl items-center gap-2 rounded-2xl border border-brand-200 bg-paper px-4 py-3 focus-within:ring-2 focus-within:ring-brand-300">
        <Search aria-hidden="true" className="h-4 w-4 shrink-0 text-brand-700" />
        <input aria-label="Tìm chuyên gia" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} placeholder="Tìm theo tên hoặc giới thiệu" className="min-w-0 flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-ink-muted" />
      </label>
    </header>

    {actionError && <p role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800">{actionError}</p>}
    {loading && <p role="status" className="py-10 text-center text-sm text-ink-muted">Đang tải danh sách chuyên gia…</p>}
    {!loading && error && <EmptyState title="Chưa tải được danh sách" description={error} action={<Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} />}
    {!loading && !error && result && result.content.length === 0 && <EmptyState icon="🌿" title="Chưa tìm thấy chuyên gia" description={query ? 'Thử tên khác hoặc xóa từ khóa tìm kiếm.' : 'Danh sách chuyên gia đang được cập nhật.'} />}

    {!loading && !error && result && result.content.length > 0 && <>
      <div className="mb-3 flex items-center gap-2 text-sm text-ink-muted"><Users className="h-4 w-4" /><span>{result.totalElements} chuyên gia</span></div>
      <ul className="divide-y divide-brand-100 rounded-2xl border border-brand-100 bg-paper px-4 sm:px-6">
        {result.content.map((expert) => <li key={expert.userId} className="flex flex-col gap-4 py-5 sm:flex-row sm:items-center">
          <Link to={`/thanh-vien/${expert.userId}`} aria-label={`Xem hồ sơ ${expert.displayName}`} className="flex min-w-0 flex-1 items-center gap-4 rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400">
            <MemberAvatar name={expert.displayName} url={expert.avatarUrl} size="lg" />
            <span className="min-w-0">
              <span className="flex flex-wrap items-center gap-2 font-bold text-ink"><span className="truncate">{expert.displayName}</span><span className="inline-flex items-center gap-1 rounded-full bg-leaf-50 px-2 py-0.5 text-[11px] font-semibold text-leaf-800"><Award className="h-3 w-3" />Chuyên gia</span></span>
              {expert.bio && <span className="mt-1 block line-clamp-2 max-w-3xl text-sm leading-6 text-ink-soft">{expert.bio}</span>}
              <span className="mt-1 block text-xs text-ink-muted">{expert.followerCount.toLocaleString('vi-VN')} người theo dõi</span>
            </span>
          </Link>
          {account?.role !== 'ADMIN' && <Button variant={expert.following ? 'outline' : 'secondary'} size="sm" disabled={pendingId === expert.userId || !canFollow && isAuthenticated} onClick={() => void toggleFollow(expert)}>
            {pendingId === expert.userId ? 'Đang cập nhật…' : expert.following ? 'Đang theo dõi' : 'Theo dõi'}
          </Button>}
        </li>)}
      </ul>
      {result.totalPages > 1 && <div className="mt-6 flex items-center justify-center gap-4"><Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button><span className="text-sm text-ink-muted">Trang {page + 1}/{result.totalPages}</span><Button variant="outline" disabled={page + 1 >= result.totalPages} onClick={() => setPage((current) => current + 1)}>Trang sau</Button></div>}
    </>}
  </PageContainer>;
}
