import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import {
  AlertCircle,
  ArrowRightLeft,
  Check,
  CirclePlus,
  ExternalLink,
  LoaderCircle,
  Pencil,
  RefreshCw,
  Salad,
  Scale,
  Search,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card, EmptyState } from '../components/ui';
import {
  AdminCatalogApiError,
  adminCatalogApi,
  type CatalogConversion,
  type CatalogIngredient,
  type CatalogUnit,
} from '../services/adminCatalogApi';

type Tab = 'ingredients' | 'units' | 'conversions';

const today = () => new Date().toISOString().slice(0, 10);
const fieldClass =
  'mt-1.5 w-full rounded-xl border border-brand-200/80 bg-white px-3.5 py-2.5 text-sm text-ink outline-none transition-all placeholder:text-ink-muted/50 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
const labelClass = 'block text-sm font-semibold text-ink';

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className={labelClass}>
      {label}
      {children}
    </label>
  );
}

export function AdminCatalogPage() {
  const [tab, setTab] = useState<Tab>('ingredients');
  const [ingredients, setIngredients] = useState<CatalogIngredient[]>([]);
  const [units, setUnits] = useState<CatalogUnit[]>([]);
  const [conversions, setConversions] = useState<CatalogConversion[]>([]);
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [ingredientId, setIngredientId] = useState<number | null>(null);
  const [unitId, setUnitId] = useState<number | null>(null);
  const [conversionEditing, setConversionEditing] = useState(false);
  const [ingredientForm, setIngredientForm] = useState({
    name: '',
    ingredientGroup: '',
    sourceName: '',
    sourceUrl: '',
    referenceDate: today(),
  });
  const [unitForm, setUnitForm] = useState({
    code: '',
    name: '',
    dimension: 'MASS' as CatalogUnit['dimension'],
    baseFactor: '1',
  });
  const [conversionForm, setConversionForm] = useState({
    ingredientId: '',
    unitId: '',
    gramsPerUnit: '',
    approximate: true,
  });
  const [loading, setLoading] = useState(true);
  const [accessDenied, setAccessDenied] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const refresh = async (filter = search) => {
    setLoading(true);
    setError('');
    setAccessDenied(false);
    try {
      const [nextIngredients, nextUnits, nextConversions] = await Promise.all([
        adminCatalogApi.ingredients(filter),
        adminCatalogApi.units(),
        adminCatalogApi.conversions(),
      ]);
      setIngredients(nextIngredients);
      setUnits(nextUnits);
      setConversions(nextConversions);
    } catch (cause) {
      setAccessDenied(
        cause instanceof AdminCatalogApiError &&
          (cause.status === 401 || cause.status === 403),
      );
      setError(
        cause instanceof Error ? cause.message : 'Không tải được danh mục.',
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void refresh('');
  }, []);

  const perform = async (
    action: () => Promise<unknown>,
    successMessage: string,
    onSuccess?: () => void,
  ) => {
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await action();
      onSuccess?.();
      setNotice(successMessage);
      setIngredientId(null);
      setUnitId(null);
      setConversionEditing(false);
      await refresh();
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Không lưu được thay đổi.',
      );
    } finally {
      setSaving(false);
    }
  };

  const submitIngredient = (event: FormEvent) => {
    event.preventDefault();
    const body = {
      ...ingredientForm,
      name: ingredientForm.name.trim(),
      ingredientGroup: ingredientForm.ingredientGroup.trim(),
      sourceName: ingredientForm.sourceName.trim(),
      sourceUrl: ingredientForm.sourceUrl.trim() || null,
    };
    void perform(
      () =>
        ingredientId === null
          ? adminCatalogApi.createIngredient(body)
          : adminCatalogApi.updateIngredient(ingredientId, body),
      ingredientId === null
        ? 'Đã thêm nguyên liệu.'
        : 'Đã cập nhật nguyên liệu.',
      () =>
        setIngredientForm({
          name: '',
          ingredientGroup: '',
          sourceName: '',
          sourceUrl: '',
          referenceDate: today(),
        }),
    );
  };

  const editIngredient = (ingredient: CatalogIngredient) => {
    setIngredientId(ingredient.id);
    setIngredientForm({
      name: ingredient.name,
      ingredientGroup: ingredient.ingredientGroup,
      sourceName: ingredient.sourceName,
      sourceUrl: ingredient.sourceUrl ?? '',
      referenceDate: ingredient.referenceDate,
    });
    setError('');
    setNotice('');
  };

  const submitUnit = (event: FormEvent) => {
    event.preventDefault();
    const body = {
      ...unitForm,
      code: unitForm.code.trim(),
      name: unitForm.name.trim(),
      baseFactor: Number(unitForm.baseFactor),
    };
    void perform(
      () =>
        unitId === null
          ? adminCatalogApi.createUnit(body)
          : adminCatalogApi.updateUnit(unitId, body),
      unitId === null ? 'Đã thêm đơn vị.' : 'Đã cập nhật đơn vị.',
      () =>
        setUnitForm({
          code: '',
          name: '',
          dimension: 'MASS',
          baseFactor: '1',
        }),
    );
  };

  const editUnit = (unit: CatalogUnit) => {
    setUnitId(unit.id);
    setUnitForm({
      code: unit.code,
      name: unit.name,
      dimension: unit.dimension,
      baseFactor: String(unit.baseFactor),
    });
    setError('');
    setNotice('');
  };

  const submitConversion = (event: FormEvent) => {
    event.preventDefault();
    const item: CatalogConversion = {
      ingredientId: Number(conversionForm.ingredientId),
      unitId: Number(conversionForm.unitId),
      gramsPerUnit: Number(conversionForm.gramsPerUnit),
      approximate: conversionForm.approximate,
      active: true,
    };
    void perform(
      () => adminCatalogApi.saveConversion(item, !conversionEditing),
      conversionEditing
        ? 'Đã cập nhật tỷ lệ quy đổi.'
        : 'Đã thêm tỷ lệ quy đổi.',
      () =>
        setConversionForm({
          ingredientId: '',
          unitId: '',
          gramsPerUnit: '',
          approximate: true,
        }),
    );
  };

  const editConversion = (item: CatalogConversion) => {
    setConversionEditing(true);
    setConversionForm({
      ingredientId: String(item.ingredientId),
      unitId: String(item.unitId),
      gramsPerUnit: String(item.gramsPerUnit),
      approximate: item.approximate,
    });
    setError('');
    setNotice('');
  };

  const toggleIngredient = (item: CatalogIngredient) =>
    void perform(
      () => adminCatalogApi.setIngredientActive(item.id, !item.active),
      item.active
        ? 'Đã ngừng sử dụng nguyên liệu; liên kết cũ được giữ nguyên.'
        : 'Đã bật lại nguyên liệu.',
    );

  const toggleUnit = (item: CatalogUnit) =>
    void perform(
      () => adminCatalogApi.setUnitActive(item.id, !item.active),
      item.active
        ? 'Đã ngừng sử dụng đơn vị; liên kết cũ được giữ nguyên.'
        : 'Đã bật lại đơn vị.',
    );

  const toggleConversion = (item: CatalogConversion) =>
    void perform(
      () => adminCatalogApi.setConversionActive(item, !item.active),
      item.active
        ? 'Đã ngừng sử dụng tỷ lệ quy đổi.'
        : 'Đã bật lại tỷ lệ quy đổi.',
    );

  const searchIngredients = (event: FormEvent) => {
    event.preventDefault();
    setSearch(query.trim());
    void refresh(query.trim());
  };

  const tabs: Array<{ id: Tab; label: string; count: number; icon: ReactNode }> = [
    {
      id: 'ingredients',
      label: 'Nguyên liệu',
      count: ingredients.length,
      icon: <Salad className="h-4 w-4" />,
    },
    {
      id: 'units',
      label: 'Đơn vị',
      count: units.length,
      icon: <Scale className="h-4 w-4" />,
    },
    {
      id: 'conversions',
      label: 'Bảng quy đổi',
      count: conversions.length,
      icon: <ArrowRightLeft className="h-4 w-4" />,
    },
  ];

  return (
    <PageContainer className="py-8 sm:py-10">
      {/* Breadcrumb Navigation */}
      <nav aria-label="Đường dẫn trang" className="mb-3 flex items-center gap-2 text-xs font-medium text-ink-muted">
        <span>Trang chủ</span>
        <span className="text-brand-300">/</span>
        <span>Quản trị hệ thống</span>
        <span className="text-brand-300">/</span>
        <span className="font-bold text-brand-700">Danh mục nguyên liệu</span>
      </nav>

      {/* Admin Header */}
      <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-leaf-700">
            <ShieldCheck className="h-4 w-4" /> Khu vực Administrator
          </p>
          <h1 className="text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">
            Danh mục nguyên liệu
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-ink-muted">
            Quản lý nguyên liệu chuẩn, đơn vị đo và quy đổi về gam. Dữ liệu đã được dùng vẫn được giữ lại khi ngừng sử dụng.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={() => void refresh()}
            disabled={loading}
            aria-label="Tải lại danh mục"
          >
            {loading ? (
              <LoaderCircle className="h-4 w-4 animate-spin text-brand-600" />
            ) : (
              <RefreshCw className="h-4 w-4" />
            )}{' '}
            Tải lại
          </Button>
        </div>
      </div>

      {/* Notifications */}
      {error && (
        <div
          role="alert"
          className="mb-5 flex gap-3 rounded-2xl border border-red-200 bg-red-50/90 p-4 text-sm text-red-800 shadow-2xs"
        >
          <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
          <div className="flex-1 font-medium">{error}</div>
        </div>
      )}
      {notice && (
        <div
          role="status"
          className="mb-5 flex gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/90 p-4 text-sm text-leaf-800 shadow-2xs"
        >
          <Check className="h-5 w-5 shrink-0 text-leaf-600" />
          <div className="flex-1 font-medium">{notice}</div>
        </div>
      )}

      {/* 403 Forbidden State */}
      {accessDenied && (
        <Card className="flex flex-col items-center justify-center p-10 sm:p-14 text-center border-red-200 bg-white shadow-xs">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-100 text-red-600 shadow-2xs">
            <ShieldAlert className="h-7 w-7" />
          </div>
          <h2 className="text-xl font-bold text-ink">Quyền truy cập bị từ chối (403 Forbidden)</h2>
          <p className="mt-2 max-w-md text-sm text-ink-muted leading-relaxed">
            Khu vực này chỉ dành riêng cho Quản trị viên hệ thống Mâm Xanh. Vui lòng đăng nhập bằng tài khoản có thẩm quyền để quản lý danh mục.
          </p>
        </Card>
      )}

      {/* Tabs */}
      {!accessDenied && (
        <div
          role="tablist"
          aria-label="Danh mục quản trị"
          className="mb-6 flex gap-2 overflow-x-auto border-b border-brand-200/60 pb-3"
        >
          {tabs.map((item) => (
            <button
              key={item.id}
              type="button"
              role="tab"
              aria-selected={tab === item.id}
              onClick={() => setTab(item.id)}
              className={`shrink-0 flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition-all focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 ${
                tab === item.id
                  ? 'bg-brand-600 text-white shadow-sm shadow-brand-600/25'
                  : 'text-ink-soft hover:bg-brand-100/70 hover:text-brand-900'
              }`}
            >
              {item.icon}
              <span>{item.label}</span>
              <span
                className={`rounded-full px-2 py-0.5 text-xs font-bold ${
                  tab === item.id
                    ? 'bg-white/20 text-white'
                    : 'bg-brand-100 text-brand-800'
                }`}
              >
                {item.count}
              </span>
            </button>
          ))}
        </div>
      )}

      {accessDenied ? null : loading ? (
        <Card className="flex items-center justify-center gap-3 p-12 text-sm text-ink-muted">
          <LoaderCircle className="h-5 w-5 animate-spin text-brand-600" /> Đang tải danh mục…
        </Card>
      ) : (
        <>
          {tab === 'ingredients' && (
            <section className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_minmax(320px,.8fr)]">
              <div>
                <form onSubmit={searchIngredients} className="mb-4 flex gap-2">
                  <label className="relative min-w-0 flex-1">
                    <span className="sr-only">Tìm tên hoặc nhóm nguyên liệu</span>
                    <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-muted" />
                    <input
                      className={`${fieldClass} mt-0 pl-10 pr-4`}
                      value={query}
                      onChange={(event) => setQuery(event.target.value)}
                      placeholder="Tìm tên hoặc nhóm nguyên liệu"
                    />
                  </label>
                  <Button type="submit" variant="outline">
                    Tìm
                  </Button>
                </form>
                <div className="space-y-3">
                  {ingredients.length === 0 ? (
                    <EmptyState
                      icon="🥗"
                      title="Chưa có nguyên liệu phù hợp"
                      description="Thêm nguyên liệu chuẩn hoặc điều chỉnh từ khóa tìm kiếm."
                    />
                  ) : (
                    ingredients.map((item) => (
                      <Card
                        key={item.id}
                        className={`flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between transition-all ${
                          item.active
                            ? 'border-brand-100 bg-white hover:border-brand-200 shadow-2xs'
                            : 'border-dashed border-brand-200 bg-brand-50/30 opacity-80'
                        }`}
                      >
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <h2 className="font-bold text-ink">{item.name}</h2>
                            <span className="rounded-md bg-brand-100/70 px-2 py-0.5 text-xs font-semibold text-brand-900 border border-brand-200/60">
                              {item.ingredientGroup}
                            </span>
                            <span
                              className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold border ${
                                item.active
                                  ? 'bg-leaf-50 text-leaf-700 border-leaf-200/60'
                                  : 'bg-brand-100 text-ink-muted border-brand-200/60'
                              }`}
                            >
                              {item.active && (
                                <span className="size-1.5 rounded-full bg-leaf-500"></span>
                              )}
                              {item.active ? 'Đang dùng' : 'Ngừng dùng'}
                            </span>
                          </div>
                          <p className="mt-1 flex flex-wrap items-center gap-x-2 text-xs text-ink-muted">
                            <span>Nguồn: <strong className="font-medium text-ink-soft">{item.sourceName}</strong></span>
                            <span>·</span>
                            <span>Ngày tham khảo: {item.referenceDate}</span>
                            {item.sourceUrl && (
                              <>
                                <span>·</span>
                                <a
                                  href={item.sourceUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="inline-flex items-center gap-0.5 font-medium text-brand-600 hover:underline"
                                >
                                  Liên kết nguồn <ExternalLink className="h-3 w-3" />
                                </a>
                              </>
                            )}
                          </p>
                        </div>
                        <div className="flex shrink-0 gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => editIngredient(item)}
                            aria-label={`Sửa ${item.name}`}
                          >
                            <Pencil className="h-3.5 w-3.5" /> Sửa
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => toggleIngredient(item)}
                            disabled={saving}
                            aria-label={`${item.active ? 'Ngừng sử dụng' : 'Bật lại'} ${item.name}`}
                          >
                            {item.active ? 'Ngừng dùng' : 'Bật lại'}
                          </Button>
                        </div>
                      </Card>
                    ))
                  )}
                </div>
              </div>
              <Card className="h-fit p-5 sm:p-6 border-brand-200/80 bg-white shadow-xs">
                <div className="flex items-center justify-between pb-3 border-b border-brand-100">
                  <h2 className="text-lg font-extrabold text-ink">
                    {ingredientId === null ? 'Thêm nguyên liệu' : 'Sửa nguyên liệu'}
                  </h2>
                  <span className="text-xs font-semibold text-brand-700 bg-brand-50 px-2.5 py-0.5 rounded-full border border-brand-200/60">
                    {ingredientId === null ? 'Thêm mới' : 'Chỉnh sửa'}
                  </span>
                </div>
                <form onSubmit={submitIngredient} className="mt-4 space-y-4">
                  <Field label="Tên chuẩn tiếng Việt">
                    <input
                      className={fieldClass}
                      required
                      maxLength={200}
                      value={ingredientForm.name}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          name: event.target.value,
                        })
                      }
                      placeholder="Ví dụ: Đậu hũ non, Nấm đông cô"
                    />
                  </Field>
                  <Field label="Nhóm nguyên liệu">
                    <input
                      className={fieldClass}
                      required
                      maxLength={100}
                      value={ingredientForm.ingredientGroup}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          ingredientGroup: event.target.value,
                        })
                      }
                      placeholder="Ví dụ: Rau củ, Nấm, Đậu hạt"
                    />
                  </Field>
                  <Field label="Tên nguồn dữ liệu">
                    <input
                      className={fieldClass}
                      required
                      maxLength={200}
                      value={ingredientForm.sourceName}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          sourceName: event.target.value,
                        })
                      }
                      placeholder="Ví dụ: Bảng Thành phần Thực phẩm Việt Nam"
                    />
                  </Field>
                  <Field label="Đường dẫn nguồn (không bắt buộc)">
                    <input
                      className={fieldClass}
                      type="url"
                      maxLength={2048}
                      value={ingredientForm.sourceUrl}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          sourceUrl: event.target.value,
                        })
                      }
                      placeholder="https://..."
                    />
                  </Field>
                  <Field label="Ngày tham khảo">
                    <input
                      className={fieldClass}
                      type="date"
                      required
                      max={today()}
                      value={ingredientForm.referenceDate}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          referenceDate: event.target.value,
                        })
                      }
                    />
                  </Field>
                  <div className="flex flex-wrap gap-2 pt-2">
                    <Button type="submit" disabled={saving}>
                      {saving ? (
                        'Đang lưu…'
                      ) : ingredientId === null ? (
                        <>
                          <CirclePlus className="h-4 w-4" /> Thêm nguyên liệu
                        </>
                      ) : (
                        'Lưu thay đổi'
                      )}
                    </Button>
                    {ingredientId !== null && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => {
                          setIngredientId(null);
                          setIngredientForm({
                            name: '',
                            ingredientGroup: '',
                            sourceName: '',
                            sourceUrl: '',
                            referenceDate: today(),
                          });
                        }}
                      >
                        Hủy sửa
                      </Button>
                    )}
                  </div>
                </form>
              </Card>
            </section>
          )}

          {tab === 'units' && (
            <section className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_minmax(320px,.8fr)]">
              <div className="space-y-3">
                {units.length === 0 ? (
                  <EmptyState
                    icon="⚖️"
                    title="Chưa có đơn vị đo"
                    description="Thêm đơn vị chuẩn để dùng trong quy đổi nguyên liệu."
                  />
                ) : (
                  units.map((item) => (
                    <Card
                      key={item.id}
                      className={`flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between transition-all ${
                        item.active
                          ? 'border-brand-100 bg-white hover:border-brand-200 shadow-2xs'
                          : 'border-dashed border-brand-200 bg-brand-50/30 opacity-80'
                      }`}
                    >
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <h2 className="font-bold text-ink">{item.name}</h2>
                          <code className="rounded-md bg-brand-100/80 px-2 py-0.5 text-xs font-bold text-brand-900 border border-brand-200/60 font-mono">
                            {item.code}
                          </code>
                          <span className="rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-semibold text-brand-800 border border-brand-200/60">
                            {item.dimension === 'MASS'
                              ? 'Khối lượng (MASS)'
                              : item.dimension === 'VOLUME'
                              ? 'Thể tích (VOLUME)'
                              : 'Đếm (COUNT)'}
                          </span>
                          <span
                            className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold border ${
                              item.active
                                ? 'bg-leaf-50 text-leaf-700 border-leaf-200/60'
                                : 'bg-brand-100 text-ink-muted border-brand-200/60'
                            }`}
                          >
                            {item.active && (
                              <span className="size-1.5 rounded-full bg-leaf-500"></span>
                            )}
                            {item.active ? 'Đang dùng' : 'Ngừng dùng'}
                          </span>
                        </div>
                        <p className="mt-1 text-xs text-ink-muted">
                          Hệ số cơ sở: <strong className="font-semibold text-ink-soft">{item.baseFactor}</strong>
                        </p>
                      </div>
                      <div className="flex shrink-0 gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => editUnit(item)}
                          aria-label={`Sửa ${item.name}`}
                        >
                          <Pencil className="h-3.5 w-3.5" /> Sửa
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => toggleUnit(item)}
                          disabled={saving}
                          aria-label={`${item.active ? 'Ngừng sử dụng' : 'Bật lại'} ${item.name}`}
                        >
                          {item.active ? 'Ngừng dùng' : 'Bật lại'}
                        </Button>
                      </div>
                    </Card>
                  ))
                )}
              </div>
              <Card className="h-fit p-5 sm:p-6 border-brand-200/80 bg-white shadow-xs">
                <div className="flex items-center justify-between pb-3 border-b border-brand-100">
                  <h2 className="text-lg font-extrabold text-ink">
                    {unitId === null ? 'Thêm đơn vị' : 'Sửa đơn vị'}
                  </h2>
                  <span className="text-xs font-semibold text-brand-700 bg-brand-50 px-2.5 py-0.5 rounded-full border border-brand-200/60">
                    {unitId === null ? 'Thêm mới' : 'Chỉnh sửa'}
                  </span>
                </div>
                <form onSubmit={submitUnit} className="mt-4 space-y-4">
                  <Field label="Ký hiệu">
                    <input
                      className={fieldClass}
                      required
                      maxLength={20}
                      value={unitForm.code}
                      onChange={(event) =>
                        setUnitForm({ ...unitForm, code: event.target.value })
                      }
                      placeholder="g, ml, quả, thìa"
                    />
                  </Field>
                  <Field label="Tên đơn vị">
                    <input
                      className={fieldClass}
                      required
                      maxLength={50}
                      value={unitForm.name}
                      onChange={(event) =>
                        setUnitForm({ ...unitForm, name: event.target.value })
                      }
                      placeholder="Gam, Mi-li-lít, Quả"
                    />
                  </Field>
                  <Field label="Thứ nguyên">
                    <select
                      className={fieldClass}
                      value={unitForm.dimension}
                      onChange={(event) =>
                        setUnitForm({
                          ...unitForm,
                          dimension: event.target.value as CatalogUnit['dimension'],
                        })
                      }
                    >
                      <option value="MASS">Khối lượng (MASS)</option>
                      <option value="VOLUME">Thể tích (VOLUME)</option>
                      <option value="COUNT">Đếm (COUNT)</option>
                    </select>
                  </Field>
                  <Field label="Hệ số cơ sở">
                    <input
                      className={fieldClass}
                      type="number"
                      min="0.000001"
                      step="any"
                      required
                      value={unitForm.baseFactor}
                      onChange={(event) =>
                        setUnitForm({ ...unitForm, baseFactor: event.target.value })
                      }
                    />
                  </Field>
                  <div className="flex flex-wrap gap-2 pt-2">
                    <Button type="submit" disabled={saving}>
                      {saving ? 'Đang lưu…' : unitId === null ? 'Thêm đơn vị' : 'Lưu thay đổi'}
                    </Button>
                    {unitId !== null && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => {
                          setUnitId(null);
                          setUnitForm({
                            code: '',
                            name: '',
                            dimension: 'MASS',
                            baseFactor: '1',
                          });
                        }}
                      >
                        Hủy sửa
                      </Button>
                    )}
                  </div>
                </form>
              </Card>
            </section>
          )}

          {tab === 'conversions' && (
            <section className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_minmax(320px,.8fr)]">
              <div className="space-y-3">
                {conversions.length === 0 ? (
                  <EmptyState
                    icon="🔄"
                    title="Chưa có tỷ lệ quy đổi"
                    description="Tạo quy đổi riêng cho từng cặp nguyên liệu và đơn vị."
                  />
                ) : (
                  conversions.map((item) => {
                    const ingredient = ingredients.find(
                      (candidate) => candidate.id === item.ingredientId,
                    );
                    const unit = units.find(
                      (candidate) => candidate.id === item.unitId,
                    );
                    return (
                      <Card
                        key={`${item.ingredientId}-${item.unitId}`}
                        className={`flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between transition-all ${
                          item.active
                            ? 'border-brand-100 bg-white hover:border-brand-200 shadow-2xs'
                            : 'border-dashed border-brand-200 bg-brand-50/30 opacity-80'
                        }`}
                      >
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <h2 className="font-bold text-ink">
                              {ingredient?.name ?? `Nguyên liệu #${item.ingredientId}`}
                            </h2>
                            <span className="text-ink-muted">·</span>
                            <span className="font-semibold text-brand-800">
                              {unit?.name ?? `Đơn vị #${item.unitId}`}
                            </span>
                            <span
                              className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold border ${
                                item.active
                                  ? 'bg-leaf-50 text-leaf-700 border-leaf-200/60'
                                  : 'bg-brand-100 text-ink-muted border-brand-200/60'
                              }`}
                            >
                              {item.active && (
                                <span className="size-1.5 rounded-full bg-leaf-500"></span>
                              )}
                              {item.active ? 'Đang dùng' : 'Ngừng dùng'}
                            </span>
                          </div>
                          <div className="mt-1 flex flex-wrap items-center gap-2 text-xs">
                            <span className="rounded-md bg-brand-50 px-2 py-0.5 font-bold text-brand-900 border border-brand-200/60">
                              1 {unit?.code ?? 'đơn vị'} = {item.gramsPerUnit} g
                            </span>
                            <span
                              className={`rounded-full px-2 py-0.5 font-semibold text-[11px] border ${
                                item.approximate
                                  ? 'bg-amber-50 text-amber-800 border-amber-200/60'
                                  : 'bg-leaf-50 text-leaf-800 border-leaf-200/60'
                              }`}
                            >
                              {item.approximate ? '≈ Xấp xỉ' : '✓ Chính xác'}
                            </span>
                          </div>
                        </div>
                        <div className="flex shrink-0 gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => editConversion(item)}
                            aria-label={`Sửa quy đổi ${ingredient?.name ?? item.ingredientId}`}
                          >
                            <Pencil className="h-3.5 w-3.5" /> Sửa
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => toggleConversion(item)}
                            disabled={saving}
                          >
                            {item.active ? 'Ngừng dùng' : 'Bật lại'}
                          </Button>
                        </div>
                      </Card>
                    );
                  })
                )}
              </div>
              <Card className="h-fit p-5 sm:p-6 border-brand-200/80 bg-white shadow-xs">
                <div className="flex items-center justify-between pb-3 border-b border-brand-100">
                  <h2 className="text-lg font-extrabold text-ink">
                    {conversionEditing ? 'Sửa tỷ lệ quy đổi' : 'Thêm tỷ lệ quy đổi'}
                  </h2>
                  <span className="text-xs font-semibold text-brand-700 bg-brand-50 px-2.5 py-0.5 rounded-full border border-brand-200/60">
                    {conversionEditing ? 'Chỉnh sửa' : 'Thêm mới'}
                  </span>
                </div>
                <form onSubmit={submitConversion} className="mt-4 space-y-4">
                  <Field label="Nguyên liệu">
                    <select
                      className={fieldClass}
                      required
                      disabled={conversionEditing}
                      value={conversionForm.ingredientId}
                      onChange={(event) =>
                        setConversionForm({
                          ...conversionForm,
                          ingredientId: event.target.value,
                        })
                      }
                    >
                      <option value="">Chọn nguyên liệu</option>
                      {ingredients.map((item) => (
                        <option key={item.id} value={item.id} disabled={!item.active}>
                          {item.name}
                          {item.active ? '' : ' (ngừng dùng)'}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Field label="Đơn vị đo">
                    <select
                      className={fieldClass}
                      required
                      disabled={conversionEditing}
                      value={conversionForm.unitId}
                      onChange={(event) =>
                        setConversionForm({
                          ...conversionForm,
                          unitId: event.target.value,
                        })
                      }
                    >
                      <option value="">Chọn đơn vị</option>
                      {units.map((item) => (
                        <option key={item.id} value={item.id} disabled={!item.active}>
                          {item.name} ({item.code})
                          {item.active ? '' : ' (ngừng dùng)'}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Field label="Gam trên mỗi đơn vị">
                    <input
                      className={fieldClass}
                      type="number"
                      min="0.000001"
                      step="any"
                      required
                      value={conversionForm.gramsPerUnit}
                      onChange={(event) =>
                        setConversionForm({
                          ...conversionForm,
                          gramsPerUnit: event.target.value,
                        })
                      }
                      placeholder="Ví dụ: 120"
                    />
                  </Field>
                  <label className="flex items-center gap-2 text-sm font-medium text-ink-soft cursor-pointer">
                    <input
                      type="checkbox"
                      className="size-4 rounded text-brand-600 focus:ring-brand-500 border-brand-300"
                      checked={conversionForm.approximate}
                      onChange={(event) =>
                        setConversionForm({
                          ...conversionForm,
                          approximate: event.target.checked,
                        })
                      }
                    />{' '}
                    Đây là tỷ lệ ước lượng
                  </label>
                  <div className="flex flex-wrap gap-2 pt-2">
                    <Button type="submit" disabled={saving}>
                      {saving
                        ? 'Đang lưu…'
                        : conversionEditing
                        ? 'Lưu thay đổi'
                        : 'Thêm quy đổi'}
                    </Button>
                    {conversionEditing && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => {
                          setConversionEditing(false);
                          setConversionForm({
                            ingredientId: '',
                            unitId: '',
                            gramsPerUnit: '',
                            approximate: true,
                          });
                        }}
                      >
                        Hủy sửa
                      </Button>
                    )}
                  </div>
                </form>
              </Card>
            </section>
          )}
        </>
      )}
    </PageContainer>
  );
}
