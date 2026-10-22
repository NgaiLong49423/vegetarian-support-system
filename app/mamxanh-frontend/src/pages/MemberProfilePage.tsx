import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CalendarDays, Eye, Heart, Pencil } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card, EmptyState } from '../components/ui';
import { MemberAvatar } from '../components/MemberAvatar';
import { MemberFollowPanel } from '../components/MemberFollowPanel';
import { PublicRecipeCard } from '../components/PublicRecipeCard';
import { joinedMonthLabel, membersApi, type MemberProfile, type MemberRecipeStatistics } from '../api/members';
import { recipesApi, type RecipeFormOptions, type RecipeSearchResult } from '../api/recipes';
import { asApiError } from '../lib/apiClient';
import { useAuth } from '../components/AuthContext';

/**
 * UC-23.2 public member profile (`/thanh-vien/:userId`): name, avatar, bio, join month and the
 * member's published Recipe Posts, plus follow counts, the follow button and lists (FR-59). No
 * email, role label or private data (AC-23.2, BR-18). Published recipes use the same cards
 * as recipe discovery, including metadata and save/meal-plan actions.
 */
export function MemberProfilePage() {
  const { account } = useAuth();
  const { userId: rawId } = useParams();
  const userId = Number(rawId);
  const validId = Number.isSafeInteger(userId) && userId > 0;
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [recipes, setRecipes] = useState<RecipeSearchResult | null>(null);
  const [recipeStatistics, setRecipeStatistics] = useState<MemberRecipeStatistics | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(!validId);
  const [retry, setRetry] = useState(0);
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);

  useEffect(() => {
    let alive = true;
    recipesApi.getFormOptions()
      .then((data) => { if (alive) setOptions(data); })
      // Cards retain their API codes if display labels are temporarily unavailable.
      .catch(() => {});
    return () => { alive = false; };
  }, []);

  useEffect(() => {
    if (!validId) return;
    let alive = true;
    setError('');
    Promise.all([membersApi.getProfile(userId), membersApi.getRecipes(userId, page), membersApi.getRecipeStatistics(userId)])
      .then(([member, list, statistics]) => {
        if (!alive) return;
        setProfile(member);
        setRecipes(list);
        setRecipeStatistics(statistics);
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
  if (!profile || !recipes || !recipeStatistics) {
    return <PageContainer className="py-12"><p role="status" className="text-center text-sm text-ink-muted">Đang tải hồ sơ...</p></PageContainer>;
  }

  return <PageContainer className="py-8">
    <Card className="p-6 sm:p-8">
      <div className="relative">
        {account?.id === profile.userId && account.role !== 'ADMIN' && <Link to="/ho-so/cai-dat" className="mb-4 ml-auto inline-flex w-fit items-center gap-2 rounded-full border border-brand-200 px-4 py-2 text-sm font-semibold text-brand-700 hover:bg-brand-50 lg:absolute lg:right-0 lg:top-0 lg:mb-0"><Pencil className="h-4 w-4" />Chỉnh sửa hồ sơ</Link>}
        <div className="flex flex-col items-center gap-5 text-center sm:flex-row sm:items-start sm:text-left">
          <MemberAvatar name={profile.displayName} url={profile.avatarUrl} size="lg" />
          <div className="min-w-0 lg:pr-44">
            <h1 className="break-words text-2xl font-extrabold text-ink sm:text-3xl">{profile.displayName}</h1>
            <p className="mt-2 inline-flex items-center gap-1.5 text-sm text-ink-muted"><CalendarDays className="h-4 w-4" />Tham gia {joinedMonthLabel(profile.joinedMonth)}</p>
            {profile.bio && <p className="mt-3 whitespace-pre-line break-words leading-7 text-ink-soft">{profile.bio}</p>}
            <MemberFollowPanel key={profile.userId} userId={profile.userId} />
            <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-ink-soft" aria-label="Tổng tương tác trên các công thức công khai">
              <span className="inline-flex items-center gap-1.5"><Eye className="h-4 w-4 text-brand-700" aria-hidden="true" /><strong className="text-ink">{recipeStatistics.totalViews.toLocaleString('vi-VN')}</strong> tổng lượt xem</span>
              <span className="inline-flex items-center gap-1.5"><Heart className="h-4 w-4 text-brand-700" aria-hidden="true" /><strong className="text-ink">{recipeStatistics.totalLikes.toLocaleString('vi-VN')}</strong> tổng lượt thích</span>
            </div>
          </div>
        </div>
      </div>
    </Card>

    <section className="mt-8" aria-labelledby="member-recipes">
      <div className="flex flex-wrap items-end justify-between gap-2 border-b border-brand-100 pb-3">
        <h2 id="member-recipes" className="text-xl font-extrabold text-ink">Bài viết</h2>
        <span className="text-sm text-ink-muted"><strong className="text-ink">{recipes.totalElements.toLocaleString('vi-VN')}</strong> công thức</span>
      </div>
      {recipes.items.length === 0
        ? <p className="mt-4 rounded-2xl border border-brand-100 bg-paper p-6 text-sm text-ink-muted">Chưa có công thức công khai nào.</p>
        : <>
          <div className="mt-4 grid grid-cols-1 gap-x-5 gap-y-7 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {recipes.items.map((recipe) => <PublicRecipeCard key={recipe.id} recipe={recipe} options={options} />)}
          </div>
          {recipes.totalPages > 1 && <div className="mt-8 flex items-center justify-center gap-4"><Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button><span className="text-sm text-ink-muted">Trang {page + 1}/{recipes.totalPages}</span><Button variant="outline" disabled={page + 1 >= recipes.totalPages} onClick={() => setPage((current) => current + 1)}>Trang sau</Button></div>}
        </>}
    </section>
  </PageContainer>;
}
