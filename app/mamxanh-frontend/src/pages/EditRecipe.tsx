import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, ImagePlus, LoaderCircle, Save, Trash2 } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { ApiError, asApiError } from '../lib/apiClient';
import { recipeApi, type RecipePost, type RecipeReferenceData, type UpdateRecipePost } from '../api/recipes';

const categories = [
  ['NOODLE_SOUP', 'Món nước'], ['STIR_FRY', 'Món xào'], ['HOT_POT', 'Lẩu'], ['BRAISED', 'Món kho'],
  ['SOUP', 'Canh'], ['FRIED', 'Món chiên'], ['STEAMED', 'Món hấp'], ['SALAD', 'Gỏi / salad'],
  ['ROLL', 'Món cuốn'], ['GRILLED', 'Món nướng'], ['DESSERT', 'Tráng miệng'],
];
const diets = [['VEGAN', 'Thuần chay'], ['LACTO', 'Chay có sữa'], ['OVO', 'Chay có trứng'], ['LACTO_OVO', 'Chay trứng và sữa']];
const difficulties = [['EASY', 'Dễ'], ['MEDIUM', 'Trung bình'], ['HARD', 'Khó']];

function messageFor(error: ApiError) {
  if (error.status === 401) return 'Bạn cần đăng nhập bằng tài khoản chuyên gia để sửa công thức. Đăng nhập hiện chưa kết nối Backend trong ứng dụng.';
  if (error.code === 'RECIPE_HIDDEN') return 'Công thức đang bị quản trị viên ẩn. Bạn không thể sửa cho đến khi quản trị viên phục hồi bài.';
  if (error.code === 'RECIPE_EDIT_NOT_ALLOWED' || error.status === 403) return 'Bạn không có quyền sửa công thức này.';
  if (error.status === 404) return 'Không tìm thấy công thức công khai này.';
  return error.message;
}

export function EditRecipe() {
  const { recipeId: rawId } = useParams();
  const recipeId = Number(rawId);
  const navigate = useNavigate();
  const [recipe, setRecipe] = useState<RecipePost | null>(null);
  const [references, setReferences] = useState<RecipeReferenceData | null>(null);
  const [form, setForm] = useState<UpdateRecipePost | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let alive = true;
    if (!Number.isSafeInteger(recipeId) || recipeId < 1) {
      setError('Đường dẫn cần có ID công thức hợp lệ. Ví dụ: /cong-thuc/123/chinh-sua.');
      setLoading(false);
      return;
    }
    Promise.all([recipeApi.getForAuthor(recipeId), recipeApi.getReferenceData()]).then(([data, refData]) => {
      if (!alive) return;
      setRecipe(data);
      setReferences(refData);
      setForm({
        title: data.title,
        description: data.description ?? '',
        instructions: data.instructions,
        dishCategory: data.dishCategory,
        vegetarianType: data.vegetarianType,
        difficulty: data.difficulty,
        servings: data.servings,
        prepTimeMin: data.prepTimeMin,
        cookTimeMin: data.cookTimeMin,
        youtubeUrl: data.youtubeUrl ?? '',
        ingredients: data.ingredients.map(({ ingredientId, customName, unitId, quantity }) => ({ ingredientId, customName, unitId, quantity })),
        media: data.media.map((image) => ({ ...image })),
      });
    }).catch((cause: unknown) => { if (alive) setError(messageFor(asApiError(cause))); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [recipeId]);

  const setField = <K extends keyof UpdateRecipePost>(key: K, value: UpdateRecipePost[K]) => {
    setForm((current) => current ? { ...current, [key]: value } : current);
    setFieldErrors((current) => ({ ...current, [key]: '' }));
    setError('');
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (!form) return;
    setSaving(true);
    setError('');
    setFieldErrors({});
    try {
      const normalized = { ...form, media: form.media.map((image, index) => ({ ...image, displayOrder: index + 1 })) };
      const updated = await recipeApi.update(recipeId, normalized);
      navigate('/ho-so/chuyen-gia-demo', { replace: true, state: { recipeUpdated: { id: updated.id, title: updated.title, updatedLabel: 'Đã cập nhật thành công' }, notice: 'Lưu thay đổi thành công.' } });
    } catch (cause) {
      const apiError = asApiError(cause);
      setError(messageFor(apiError));
      setFieldErrors(Object.fromEntries(apiError.errors.map(({ field, message }) => [field, message])));
    } finally { setSaving(false); }
  };

  if (loading) return <PageContainer className="py-16 text-center text-ink-muted"><LoaderCircle className="mx-auto mb-3 h-7 w-7 animate-spin" />Đang tải công thức…</PageContainer>;
  if (!recipe || !form || !references) return <PageContainer className="py-12"><Card className="mx-auto max-w-2xl p-6 sm:p-8">
    <h1 className="mb-3 flex items-center gap-2 text-lg font-bold text-ink"><AlertTriangle className="h-5 w-5 text-amber-600" />Không thể mở biểu mẫu sửa</h1>
    <p className="text-sm leading-6 text-ink-muted">{error}</p>
    {error.includes('quản trị viên ẩn') && <p className="mt-3 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">Hãy liên hệ quản trị viên để phục hồi bài. Tác giả không thể tự sửa hoặc mở lại bài đang bị ẩn.</p>}
    <Link className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700" to="/kham-pha"><ArrowLeft className="h-4 w-4" />Quay lại khám phá</Link>
  </Card></PageContainer>;

  const input = 'mt-1 w-full rounded-xl border border-brand-200 bg-white px-3.5 py-3 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
  const label = 'block text-sm font-semibold text-ink-soft';
  const fieldError = (key: string) => fieldErrors[key] && <p className="mt-1 text-xs text-red-700">{fieldErrors[key]}</p>;
  const fieldLabels: Record<string, string> = {
    title: 'Tên món', description: 'Mô tả', instructions: 'Hướng dẫn nấu', dishCategory: 'Danh mục món',
    vegetarianType: 'Chế độ chay', difficulty: 'Độ khó', servings: 'Khẩu phần', prepTimeMin: 'Thời gian chuẩn bị',
    cookTimeMin: 'Thời gian nấu', youtubeUrl: 'Video YouTube', ingredients: 'Nguyên liệu', media: 'Hình ảnh',
  };

  return <PageContainer className="py-6 sm:py-9">
    <Link to={`/cong-thuc/id/${recipeId}`} className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeft className="h-4 w-4" />Quay lại công thức</Link>
    <div className="mb-6"><p className="mb-1 text-xs font-bold uppercase tracking-widest text-brand-600">Chỉnh sửa bài của bạn · #{recipe.id}</p><h1 className="text-2xl font-extrabold text-ink sm:text-3xl">Chỉnh sửa công thức</h1><p className="mt-1 text-sm text-ink-muted">Cập nhật sẽ hiển thị công khai ngay sau khi lưu.</p></div>
    {error && <div role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">{error}</div>}
    {Object.entries(fieldErrors).some(([, message]) => message) && <ul role="alert" className="mb-5 list-inside list-disc rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      {Object.entries(fieldErrors).filter(([, message]) => message).map(([field, message]) => {
        const rootField = field.match(/^[^.[]+/)?.[0] ?? field;
        return <li key={field}><strong>{fieldLabels[rootField] ?? 'Thông tin'}:</strong> {message}</li>;
      })}
    </ul>}
    <form onSubmit={save} className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_300px]">
      <div className="space-y-5">
        <Card className="space-y-4 p-5 sm:p-6">
          <h2 className="text-lg font-bold text-ink">Thông tin công thức</h2>
          <label className={label}>Tên món *<input className={input} required minLength={3} maxLength={120} value={form.title} onChange={(e) => setField('title', e.target.value)} />{fieldError('title')}</label>
          <label className={label}>Mô tả<textarea className={input} rows={3} maxLength={2000} value={form.description} onChange={(e) => setField('description', e.target.value)} /></label>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className={label}>Loại món<select className={input} value={form.dishCategory} onChange={(e) => setField('dishCategory', e.target.value)}>{categories.map(([value, name]) => <option key={value} value={value}>{name}</option>)}</select></label>
            <label className={label}>Chế độ chay<select className={input} value={form.vegetarianType} onChange={(e) => setField('vegetarianType', e.target.value)}>{diets.map(([value, name]) => <option key={value} value={value}>{name}</option>)}</select></label>
            <label className={label}>Độ khó<select className={input} value={form.difficulty} onChange={(e) => setField('difficulty', e.target.value)}>{difficulties.map(([value, name]) => <option key={value} value={value}>{name}</option>)}</select></label>
            <label className={label}>Khẩu phần<input className={input} type="number" min={1} max={50} required value={form.servings} onChange={(e) => setField('servings', Number(e.target.value))} /></label>
            <label className={label}>Thời gian chuẩn bị (phút)<input className={input} type="number" min={0} max={1440} required value={form.prepTimeMin} onChange={(e) => setField('prepTimeMin', Number(e.target.value))} /></label>
            <label className={label}>Thời gian nấu (phút)<input className={input} type="number" min={0} max={1440} required value={form.cookTimeMin} onChange={(e) => setField('cookTimeMin', Number(e.target.value))} /></label>
          </div>
          <label className={label}>Hướng dẫn nấu *<textarea className={input} rows={8} minLength={10} maxLength={5000} required value={form.instructions} onChange={(e) => setField('instructions', e.target.value)} /><span className="mt-1 block text-right text-xs font-normal text-ink-muted">{form.instructions.length}/5000</span>{fieldError('instructions')}</label>
          <label className={label}>Video YouTube (không bắt buộc)<input className={input} type="url" value={form.youtubeUrl} onChange={(e) => setField('youtubeUrl', e.target.value)} placeholder="https://youtu.be/..." /></label>
        </Card>
        <Card className="p-5 sm:p-6"><div className="mb-4 flex items-center justify-between"><div><h2 className="text-lg font-bold text-ink">Nguyên liệu</h2><p className="text-xs text-ink-muted">Giữ nguyên mã nguyên liệu và đơn vị hiện tại; chỉnh tên hiển thị, số lượng.</p></div></div>
          <div className="space-y-3">{form.ingredients.map((ingredient, index) => {
            const allowedUnits = ingredient.ingredientId === null ? references.customIngredientUnits
              : references.ingredients.filter((item) => item.ingredientId === ingredient.ingredientId).map((item) => ({ unitId: item.unitId, code: item.unitCode, name: item.unitName }));
            return <div key={`${ingredient.ingredientId}-${index}`} className="grid gap-2 rounded-xl bg-brand-50/70 p-3 sm:grid-cols-[minmax(0,1fr)_120px_120px_auto] sm:items-end">
            <label className="text-xs font-medium text-ink-muted">Nguyên liệu{ingredient.ingredientId === null
              ? <input className={input} required maxLength={200} value={ingredient.customName ?? ''} onChange={(e) => setField('ingredients', form.ingredients.map((item, i) => i === index ? { ...item, customName: e.target.value } : item))} />
              : <select className={input} value={ingredient.ingredientId} onChange={(e) => { const chosen = references.ingredients.find((item) => item.ingredientId === Number(e.target.value)); if (chosen) setField('ingredients', form.ingredients.map((item, i) => i === index ? { ...item, ingredientId: chosen.ingredientId, unitId: chosen.unitId } : item)); }}>{[...new Map(references.ingredients.map((item) => [item.ingredientId, item])).values()].map((item) => <option key={item.ingredientId} value={item.ingredientId}>{item.name}</option>)}</select>}</label>
            <label className="text-xs font-medium text-ink-muted">Số lượng<input className={input} type="number" min="0.01" step="0.01" required value={ingredient.quantity} onChange={(e) => setField('ingredients', form.ingredients.map((item, i) => i === index ? { ...item, quantity: Number(e.target.value) } : item))} /></label>
            <label className="text-xs font-medium text-ink-muted">Đơn vị<select className={input} value={ingredient.unitId} onChange={(e) => setField('ingredients', form.ingredients.map((item, i) => i === index ? { ...item, unitId: Number(e.target.value) } : item))}>{allowedUnits.map((unit) => <option key={unit.unitId} value={unit.unitId}>{unit.name} ({unit.code})</option>)}</select></label>
            <Button type="button" variant="ghost" size="sm" disabled={form.ingredients.length === 1} aria-label="Xóa nguyên liệu" onClick={() => setField('ingredients', form.ingredients.filter((_, i) => i !== index))}><Trash2 className="h-4 w-4 text-red-600" /></Button>
          </div>; })}</div>
          <button type="button" className="mt-4 text-sm font-semibold text-brand-700" onClick={() => setField('ingredients', [...form.ingredients, { ingredientId: references.ingredients[0]?.ingredientId ?? null, customName: references.ingredients.length ? null : '', unitId: references.ingredients[0]?.unitId ?? references.customIngredientUnits[0]?.unitId ?? 1, quantity: 1 }])}>+ Thêm dòng nguyên liệu</button>
          {fieldError('ingredients')}
        </Card>
        <Card className="p-5 sm:p-6"><div className="mb-3 flex items-center justify-between"><div><h2 className="text-lg font-bold text-ink">Hình ảnh</h2><p className="text-xs text-ink-muted">Tối đa 5 ảnh; có thể bỏ toàn bộ ảnh. Chọn đúng một ảnh bìa.</p></div><ImagePlus className="h-5 w-5 text-brand-600" /></div>
          <div className="space-y-3">{form.media.map((image, index) => <div className="flex gap-3 rounded-xl border border-brand-100 p-3" key={`${image.displayOrder}-${index}`}><img src={image.url} alt="Ảnh công thức" className="h-16 w-16 rounded-lg object-cover" /><div className="min-w-0 flex-1"><input className={input} aria-label={`Đường dẫn ảnh ${index + 1}`} value={image.url} onChange={(e) => setField('media', form.media.map((entry, i) => i === index ? { ...entry, url: e.target.value } : entry))} /><label className="mt-2 flex items-center gap-2 text-xs"><input type="radio" name="cover" checked={image.cover} onChange={() => setField('media', form.media.map((entry, i) => ({ ...entry, cover: i === index })))} />Ảnh bìa</label></div><button type="button" aria-label="Xóa ảnh" onClick={() => setField('media', form.media.filter((_, i) => i !== index))}><Trash2 className="h-4 w-4 text-red-600" /></button></div>)}</div>
          {form.media.length < 5 && <button type="button" className="mt-3 text-sm font-semibold text-brand-700" onClick={() => setField('media', [...form.media, { url: '', mimeType: 'image/jpeg', displayOrder: form.media.length + 1, cover: form.media.length === 0 }])}>+ Thêm URL ảnh</button>}{fieldError('media')}
        </Card>
      </div>
      <aside className="space-y-4 lg:sticky lg:top-6"><Card className="p-5"><p className="mb-1 text-xs font-bold uppercase tracking-wide text-brand-600">Đang chỉnh sửa</p><h2 className="text-lg font-bold text-ink">{recipe.title}</h2><p className="mt-2 text-sm text-ink-muted">Bài đang công khai. Khi lưu hợp lệ, nội dung mới được cập nhật ngay.</p><Button type="submit" disabled={saving} className="mt-5 w-full">{saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}Lưu thay đổi</Button><Button type="button" variant="outline" className="mt-3 w-full" onClick={() => navigate('/ho-so/chuyen-gia-demo')}>Hủy</Button></Card><div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-xs leading-5 text-amber-950"><strong>Lưu ý:</strong> Bài bị quản trị viên ẩn sẽ bị chặn mọi thao tác sửa. Chỉ quản trị viên mới có thể phục hồi.</div></aside>
    </form>
  </PageContainer>;
}
