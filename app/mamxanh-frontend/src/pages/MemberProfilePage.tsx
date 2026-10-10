import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CalendarDays } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card, EmptyState } from '../components/ui';
import { MemberAvatar } from '../components/MemberAvatar';
import { ApiRecipeCard } from '../components/ApiRecipeCard';
import { joinedMonthLabel, membersApi, type MemberProfile } from '../api/members';
import type { RecipeSearchResult } from '../api/recipes';
import { asApiError } from '../lib/apiClient';

/**
 * UC-23.2 public member profile (`/thanh-vien/:userId`): name, avatar, bio, join month and the
 * member's published Recipe Posts. No email, role label or private data (AC-23.2, BR-18).
 */
export function MemberProfilePage() {
  const { userId: rawId } = useParams();
  const userId = Number(rawId);
  const validId = Number.isSafeInteger(userId) && userId > 0;
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [recipes, setRecipes] = useState<RecipeSearchResult | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(!validId);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    if (!validId) return;
    let alive = true;
    setError('');
    Promise.all([membersApi.getProfile(userId), membersApi.getRecipes(userId, page)])
      .then(([member, list]) => {
        if (!alive) return;
        setProfile(member);
        setRecipes(list);
      })
      .catch((cause: unknown) => {
        if (!alive) return;
        const failure = asApiError(cause);
        if (failure.status === 404) setNotFound(true);
        else setError(failure.message);
      });
    return () => { alive = false; };
  }, [userId, validId, page, retry]);

  if (notFound) {
    return <PageContainer className="py-12"><EmptyState title="Không tìm thấy hồ sơ thành viên" description="Hồ sơ này không tồn tại hoặc không phải hồ sơ thành viên công khai." action={<Link to="/kham-pha" className="inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeft className="h-4 w-4" />Khám phá công thức</Link>} /></PageContainer>;
  }
  if (error) {
    return <PageContainer className="py-12"><EmptyState title="Không tải được hồ sơ" description={error} action={<Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} /></PageContainer>;
  }
  if (!profile || !recipes) {
    return <PageContainer className="py-12"><p role="status" className="text-center text-sm text-ink-muted">Đang tải hồ sơ...</p></PageContainer>;
  }

  return <PageContainer className="py-8">
    <Card className="p-6 sm:p-8">
      <div className="flex flex-col items-center gap-5 text-center sm:flex-row sm:items-start sm:text-left">
        <MemberAvatar name={profile.displayName} url={profile.avatarUrl} size="lg" />
        <div className="min-w-0">
          <h1 className="break-words text-2xl font-extrabold text-ink sm:text-3xl">{profile.displayName}</h1>
          <p className="mt-2 inline-flex items-center gap-1.5 text-sm text-ink-muted"><CalendarDays className="h-4 w-4" />Tham gia {joinedMonthLabel(profile.joinedMonth)}</p>
          {profile.bio && <p className="mt-3 whitespace-pre-line break-words leading-7 text-ink-soft">{profile.bio}</p>}
        </div>
      </div>
    </Card>

    <section className="mt-8" aria-labelledby="member-recipes">
      <h2 id="member-recipes" className="text-xl font-extrabold text-ink">Công thức đã đăng</h2>
      {recipes.items.length === 0
        ? <p className="mt-4 rounded-2xl border border-brand-100 bg-white p-6 text-sm text-ink-muted">Thành viên chưa có công thức công khai nào.</p>
        : <>
          <p className="mt-1 text-sm text-ink-muted"><strong className="text-ink">{recipes.totalElements}</strong> công thức</p>
          <div className="mt-4 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{recipes.items.map((recipe) => <ApiRecipeCard key={recipe.id} recipe={recipe} showAuthor={false} />)}</div>
          {recipes.totalPages > 1 && <div className="mt-8 flex items-center justify-center gap-4"><Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button><span className="text-sm text-ink-muted">Trang {page + 1}/{recipes.totalPages}</span><Button variant="outline" disabled={page + 1 >= recipes.totalPages} onClick={() => setPage((current) => current + 1)}>Trang sau</Button></div>}
        </>}
    </section>
  </PageContainer>;
}
