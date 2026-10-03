import { useEffect, useState } from 'react';
import { BadgeCheck, ChefHat, Edit3, ExternalLink, Leaf, Trash2 } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { recipeApi } from '../api/recipes';

type DemoRecipe = {
  id: number;
  title: string;
  category: string;
  diet: string;
  status: string;
  updatedLabel: string;
};

const initialRecipes: DemoRecipe[] = [
  {
    id: 47,
    title: 'Nấm kho tiêu',
    category: 'Món kho',
    diet: 'Thuần chay',
    status: 'Đang công khai',
    updatedLabel: 'Bài mẫu dùng để review Issue #47',
  },
];

/** Visual-only expert profile fixture. It does not represent a signed-in identity. */
export function ExpertRecipeProfile() {
  const location = useLocation();
  const navigate = useNavigate();
  const [recipes, setRecipes] = useState(initialRecipes);
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const state = location.state as { recipeUpdated?: DemoRecipe; recipeDeleted?: number; notice?: string } | null;
    if (!state) return;
    if (state.recipeUpdated) setRecipes((current) => current.map((recipe) => recipe.id === state.recipeUpdated?.id ? { ...recipe, ...state.recipeUpdated } : recipe));
    if (state.recipeDeleted) setRecipes((current) => current.filter((recipe) => recipe.id !== state.recipeDeleted));
    if (state.notice) setNotice(state.notice);
    navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate]);

  const removeRecipe = async () => {
    if (confirmDelete === null) return;
    setDeleting(true);
    setError('');
    try {
      await recipeApi.delete(confirmDelete);
      setRecipes((current) => current.filter((recipe) => recipe.id !== confirmDelete));
      setNotice('Đã xóa công thức khỏi danh sách bài đã đăng.');
      setConfirmDelete(null);
    } catch {
      setError('Chưa xóa được công thức. Vui lòng thử lại.');
      setConfirmDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <PageContainer className="py-8">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <Link to="/ho-so" className="text-sm font-semibold text-brand-700 hover:underline">Quay lại hồ sơ</Link>
        <Link to="/kham-pha" className="text-sm font-semibold text-brand-700 hover:underline">Khám phá công thức</Link>
      </div>

      <Card className="mb-6 overflow-hidden">
        <div className="h-28 bg-gradient-to-r from-brand-600 via-brand-400 to-amber-300" />
        <div className="flex flex-col gap-4 p-6 sm:flex-row sm:items-end">
          <div className="-mt-16 flex h-24 w-24 items-center justify-center rounded-2xl bg-white text-brand-700 ring-4 ring-white shadow-sm">
            <ChefHat className="h-12 w-12" aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="mb-1 text-xs font-bold uppercase tracking-wider text-brand-700">Hồ sơ chuyên gia</p>
            <h1 className="flex items-center gap-2 text-2xl font-extrabold text-ink">
              Chuyên gia Demo Issue #47 <BadgeCheck className="h-5 w-5 text-brand-600" aria-label="Đã xác minh" />
            </h1>
            <p className="mt-1 text-sm text-ink-muted">Chuyên gia ẩm thực Mâm Xanh · Hồ sơ mẫu để review giao diện sửa/xóa.</p>
            <span className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-700">
              <Leaf className="h-3.5 w-3.5" aria-hidden="true" /> Chuyên gia ẩm thực
            </span>
          </div>
        </div>
      </Card>

      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-xl font-extrabold text-ink">Công thức đã đăng</h2>
          <p className="mt-1 text-sm text-ink-muted">Chỉ chuyên gia sở hữu bài mới quản lý được bài đó.</p>
        </div>
        <span className="rounded-full bg-brand-50 px-3 py-1 text-sm font-semibold text-brand-700">{recipes.length} bài</span>
      </div>

      {notice && <p role="status" className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm font-semibold text-emerald-800">{notice}</p>}
      {error && <p role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error}</p>}

      <div className="space-y-3">
        {recipes.length === 0 && <Card className="p-6 text-center text-sm text-ink-muted">Bạn chưa có công thức nào đã đăng.</Card>}
        {recipes.map((recipe) => (
          <Card key={recipe.id} className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
            <div className="min-w-0">
              <h3 className="text-lg font-bold text-ink">{recipe.title}</h3>
              <div className="mt-2 flex flex-wrap gap-2 text-xs font-semibold">
                <span className="rounded-full bg-brand-50 px-2.5 py-1 text-brand-700">{recipe.category}</span>
                <span className="rounded-full bg-brand-50 px-2.5 py-1 text-brand-700">{recipe.diet}</span>
                <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-emerald-700">{recipe.status}</span>
              </div>
              <p className="mt-2 text-xs text-ink-muted">{recipe.updatedLabel}</p>
            </div>
            <div className="flex shrink-0 flex-wrap gap-2">
              <Link to={`/cong-thuc/id/${recipe.id}`}>
                <Button variant="outline" size="sm"><ExternalLink className="h-4 w-4" />Xem bài</Button>
              </Link>
              <Link to={`/cong-thuc/${recipe.id}/chinh-sua`}>
                <Button size="sm"><Edit3 className="h-4 w-4" />Sửa</Button>
              </Link>
              <Button variant="outline" size="sm" className="text-red-700" onClick={() => setConfirmDelete(recipe.id)}><Trash2 className="h-4 w-4" />Xóa</Button>
            </div>
          </Card>
        ))}
      </div>

      <p className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-xs leading-5 text-amber-950">
        Bản xem thử dùng recipe giả. Hồ sơ này không xác thực tài khoản hay quyền chuyên gia; Backend thật vẫn phải kiểm tra role và quyền sở hữu trước khi sửa/xóa.
      </p>

      {confirmDelete !== null && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="presentation"><div role="dialog" aria-modal="true" aria-labelledby="profile-delete-heading" className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"><h2 id="profile-delete-heading" className="text-xl font-extrabold text-ink">Bạn chắc chắn muốn xóa bài này?</h2><p className="mt-2 text-sm leading-6 text-ink-muted">Bài sẽ biến khỏi danh sách công thức đã đăng và tìm kiếm công khai.</p><div className="mt-5 flex justify-end gap-3"><Button variant="outline" disabled={deleting} onClick={() => setConfirmDelete(null)}>Hủy</Button><Button disabled={deleting} className="bg-red-700 hover:bg-red-800" onClick={removeRecipe}>{deleting ? 'Đang xóa…' : 'Xác nhận xóa'}</Button></div></div></div>}
    </PageContainer>
  );
}
