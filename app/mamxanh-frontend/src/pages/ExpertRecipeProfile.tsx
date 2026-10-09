import { useCallback, useEffect, useState } from 'react';
import { BadgeCheck, ChefHat, Edit3, ExternalLink, Leaf, LoaderCircle, Trash2 } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../components/AuthContext';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { ApiError, asApiError } from '../lib/apiClient';
import { recipeApi, type RecipePost } from '../api/recipes';

function recipeStatus(status: RecipePost['status']) {
  if (status === 'HIDDEN') return 'Đã bị Admin ẩn';
  if (status === 'DELETED') return 'Đã xóa';
  return 'Đang công khai';
}

function errorMessage(error: unknown) {
  const apiError = asApiError(error);
  if (apiError.status === 401) return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại bằng tài khoản Chuyên gia.';
  if (apiError.status === 403) return 'Chỉ Chuyên gia đang hoạt động mới được quản lý bài công thức của mình.';
  return apiError.message;
}

export function ExpertRecipeProfile() {
  const { account, isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [recipes, setRecipes] = useState<RecipePost[]>([]);
  const [loading, setLoading] = useState(true);
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const loadRecipes = useCallback(async () => {
    if (!isAuthenticated || account?.role !== 'EXPERT') {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await recipeApi.listMine();
      setRecipes(response.items);
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setLoading(false);
    }
  }, [account?.role, isAuthenticated]);

  useEffect(() => { void loadRecipes(); }, [loadRecipes]);

  useEffect(() => {
    const state = location.state as { notice?: string; recipeUpdated?: RecipePost } | null;
    if (state?.notice) setNotice(state.notice);
    const updatedRecipe = state?.recipeUpdated;
    if (updatedRecipe) setRecipes((current) => current.map((recipe) => recipe.id === updatedRecipe.id ? updatedRecipe : recipe));
    if (state) navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate]);

  const removeRecipe = async () => {
    if (confirmDelete === null) return;
    setDeleting(true);
    setError('');
    try {
      await recipeApi.delete(confirmDelete);
      setRecipes((current) => current.filter((recipe) => recipe.id !== confirmDelete));
      setNotice('Đã xóa công thức. Bài không còn xuất hiện công khai.');
      setConfirmDelete(null);
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setDeleting(false);
    }
  };

  if (!isAuthenticated) return <PageContainer className="py-12"><Card className="mx-auto max-w-xl p-6 text-center sm:p-8">
    <h1 className="text-xl font-extrabold text-ink">Đăng nhập để quản lý công thức</h1>
    <p className="mt-2 text-sm text-ink-muted">Chỉ tài khoản Chuyên gia đang hoạt động mới xem và quản lý được bài của mình.</p>
    <Link to="/dang-nhap" className="mt-5 inline-flex rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-semibold text-white">Đăng nhập</Link>
  </Card></PageContainer>;

  if (account?.role !== 'EXPERT') return <PageContainer className="py-12"><Card className="mx-auto max-w-xl p-6 text-center sm:p-8">
    <h1 className="text-xl font-extrabold text-ink">Không có quyền quản lý bài công thức</h1>
    <p className="mt-2 text-sm text-ink-muted">Chức năng này dành cho Chuyên gia đang hoạt động.</p>
    <Link to="/ho-so" className="mt-5 inline-flex text-sm font-semibold text-brand-700">Quay lại hồ sơ</Link>
  </Card></PageContainer>;

  return <PageContainer className="py-8">
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
      <Link to="/ho-so" className="text-sm font-semibold text-brand-700 hover:underline">Quay lại hồ sơ</Link>
      <Link to="/kham-pha" className="text-sm font-semibold text-brand-700 hover:underline">Khám phá công thức</Link>
    </div>

    <Card className="mb-6 overflow-hidden">
      <div className="h-28 bg-gradient-to-r from-brand-600 via-brand-400 to-amber-300" />
      <div className="flex flex-col gap-4 p-6 sm:flex-row sm:items-end">
        <div className="-mt-16 flex h-24 w-24 items-center justify-center overflow-hidden rounded-2xl bg-white text-brand-700 ring-4 ring-white shadow-sm">
          {account.avatarUrl ? <img src={account.avatarUrl} alt="" className="h-full w-full object-cover" /> : <ChefHat className="h-12 w-12" aria-hidden="true" />}
        </div>
        <div className="min-w-0 flex-1">
          <p className="mb-1 text-xs font-bold uppercase tracking-wider text-brand-700">Bài công thức cá nhân</p>
          <h1 className="flex items-center gap-2 text-2xl font-extrabold text-ink">
            {account.displayName} <BadgeCheck className="h-5 w-5 text-brand-600" aria-label="Tài khoản Chuyên gia" />
          </h1>
          <span className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-700">
            <Leaf className="h-3.5 w-3.5" aria-hidden="true" /> Chuyên gia ẩm thực
          </span>
        </div>
      </div>
    </Card>

    <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h2 className="text-xl font-extrabold text-ink">Công thức của tôi</h2>
        <p className="mt-1 text-sm text-ink-muted">Bài viết bị tạm ẩn vẫn nằm trong danh sách để bạn xem, vui lòng liên hệ Ban quản trị để được hỗ trợ mở lại.</p>
      </div>
      <span className="rounded-full bg-brand-50 px-3 py-1 text-sm font-semibold text-brand-700">{recipes.length} bài</span>
    </div>

    {notice && <p role="status" className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm font-semibold text-emerald-800">{notice}</p>}
    {error && <div role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800"><p>{error}</p><Button variant="outline" size="sm" className="mt-3" onClick={() => void loadRecipes()}>Thử tải lại</Button></div>}
    {loading && <div role="status" className="py-10 text-center text-sm text-ink-muted"><LoaderCircle className="mx-auto mb-2 h-6 w-6 animate-spin" />Đang tải bài công thức…</div>}

    {!loading && !error && recipes.length === 0 && <Card className="p-6 text-center text-sm text-ink-muted">Bạn chưa có công thức nào.</Card>}
    {!loading && recipes.length > 0 && <div className="space-y-3">
      {recipes.map((recipe) => {
        const hidden = recipe.status === 'HIDDEN';
        const cover = recipe.media.find((item) => item.cover) ?? recipe.media[0];
        return <Card key={recipe.id} className="flex flex-col gap-4 p-4 sm:flex-row sm:items-start sm:justify-between sm:p-5">
          <div className="flex min-w-0 gap-4">
            {cover && <img src={cover.url} alt="" className="h-20 w-20 shrink-0 rounded-xl object-cover" />}
            <div className="min-w-0">
              <h3 className="text-lg font-bold text-ink">{recipe.title}</h3>
              <div className="mt-2 flex flex-wrap gap-2 text-xs font-semibold">
                <span className="rounded-full bg-brand-50 px-2.5 py-1 text-brand-700">{recipe.dishCategory}</span>
                <span className="rounded-full bg-brand-50 px-2.5 py-1 text-brand-700">{recipe.vegetarianType}</span>
                <span className={`rounded-full px-2.5 py-1 ${hidden ? 'bg-amber-100 text-amber-900' : 'bg-emerald-100 text-emerald-800'}`}>{recipeStatus(recipe.status)}</span>
              </div>
              {hidden && <p className="mt-2 text-sm text-amber-900">Bài viết đang ở trạng thái tạm ẩn, bạn có thể xem lại nội dung và liên hệ Ban quản trị khi cần hỗ trợ mở lại.</p>}
              <details className="mt-3 text-sm">
                <summary className="cursor-pointer font-semibold text-brand-700">Xem nội dung bài</summary>
                <div className="mt-3 space-y-2 rounded-xl bg-brand-50/60 p-4 text-ink-soft">
                  {recipe.description && <p>{recipe.description}</p>}
                  <p className="whitespace-pre-line">{recipe.instructions}</p>
                  <p className="font-semibold">Nguyên liệu</p>
                  <ul className="list-inside list-disc">{recipe.ingredients.map((ingredient, index) => <li key={`${ingredient.ingredientId}-${index}`}>{ingredient.name} — {ingredient.quantity} {ingredient.unitName}</li>)}</ul>
                  {recipe.media.length > 0 && <div className="flex flex-wrap gap-2">{recipe.media.map((media) => <img key={media.displayOrder} src={media.url} alt="Ảnh công thức" className="h-16 w-16 rounded-lg object-cover" />)}</div>}
                </div>
              </details>
            </div>
          </div>
          <div className="flex shrink-0 flex-wrap gap-2 sm:justify-end">
            {hidden ? <Button size="sm" disabled aria-label={`Sửa ${recipe.title}`}><Edit3 className="h-4 w-4" />Sửa</Button> : <Link to={`/cong-thuc/${recipe.id}/chinh-sua`}><Button size="sm"><Edit3 className="h-4 w-4" />Sửa</Button></Link>}
            {!hidden && <Link to={`/cong-thuc/id/${recipe.id}`}><Button variant="outline" size="sm"><ExternalLink className="h-4 w-4" />Xem bài</Button></Link>}
            <Button variant="outline" size="sm" className="text-red-700" onClick={() => setConfirmDelete(recipe.id)}><Trash2 className="h-4 w-4" />Xóa</Button>
          </div>
        </Card>;
      })}
    </div>}

    {confirmDelete !== null && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="presentation"><div role="dialog" aria-modal="true" aria-labelledby="profile-delete-heading" className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"><h2 id="profile-delete-heading" className="text-xl font-extrabold text-ink">Bạn chắc chắn muốn xóa bài này?</h2><p className="mt-2 text-sm leading-6 text-ink-muted">Bài sẽ không còn xuất hiện công khai. Nếu bài đang nằm trong Meal Plan của người khác, vị trí bữa ăn vẫn được giữ và sẽ hiện thông báo đã xóa.</p><div className="mt-5 flex justify-end gap-3"><Button variant="outline" disabled={deleting} onClick={() => setConfirmDelete(null)}>Hủy</Button><Button disabled={deleting} className="bg-red-700 hover:bg-red-800" onClick={() => void removeRecipe()}>{deleting ? 'Đang xóa…' : 'Xác nhận xóa'}</Button></div></div></div>}
  </PageContainer>;
}
