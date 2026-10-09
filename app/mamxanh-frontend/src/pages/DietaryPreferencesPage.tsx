import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, Info, LoaderCircle, LockKeyhole, Sparkles } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Card } from '../components/ui';
import { useAuth } from '../components/AuthContext';
import { DietaryPreferencesForm } from '../components/DietaryPreferencesForm';
import { toProblem } from '../lib/problem';
import {
  getDietaryPreferences,
  saveDietaryPreferences,
  type DietaryPreferences,
  type DietaryRequirement,
  type SaveDietaryPreferencesPayload,
} from '../services/dietaryPreferencesApi';

const REQUIREMENT_LABELS: Record<DietaryRequirement, string> = {
  VEGETARIAN_TYPE: 'Loại ăn chay',
  AVOID_INGREDIENTS: 'Nguyên liệu dị ứng/kiêng',
  DISLIKED_INGREDIENTS: 'Món không thích',
};

/** UC-31.3: view and update the private dietary-preference profile from Settings. */
export function DietaryPreferencesPage() {
  const { isAuthenticated, account } = useAuth();
  const [profile, setProfile] = useState<DietaryPreferences | null>(null);
  const [loading, setLoading] = useState(isAuthenticated);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const isAdmin = account?.role === 'ADMIN';

  useEffect(() => {
    if (!isAuthenticated || isAdmin) {
      setLoading(false);
      return;
    }
    getDietaryPreferences()
      .then(setProfile)
      .catch((cause: unknown) => setLoadError(toProblem(cause)?.detail ?? 'Chưa tải được sở thích ăn uống. Vui lòng thử lại.'))
      .finally(() => setLoading(false));
  }, [isAuthenticated, isAdmin]);

  const save = async (payload: SaveDietaryPreferencesPayload) => {
    setSaved(false);
    setProfile(await saveDietaryPreferences(payload));
    setSaved(true);
  };

  const ai = profile?.aiPersonalization;

  return (
    <PageContainer className="py-8 md:py-10">
      <nav aria-label="Điều hướng trang" className="mb-5 text-sm text-ink-muted">
        <Link to="/ho-so" className="hover:text-leaf-700">Hồ sơ</Link><span className="mx-2">/</span><span aria-current="page">Sở thích ăn uống</span>
      </nav>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-ink md:text-4xl">Sở thích ăn uống</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-ink-muted">Cập nhật loại ăn chay, nguyên liệu cần tránh và món không thích. Thay đổi áp dụng cho các yêu cầu AI tiếp theo, không sửa thực đơn đã lưu.</p>
        </div>
        <div className="inline-flex items-center gap-2 rounded-full border border-leaf-100 bg-white px-3 py-2 text-xs font-semibold text-ink-soft"><LockKeyhole aria-hidden="true" className="h-4 w-4 text-leaf-700" /> Chỉ bạn xem được</div>
      </div>

      {!isAuthenticated && (
        <p role="alert" className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950">
          Đăng nhập tài khoản thật để xem và cập nhật sở thích ăn uống. <Link to="/dang-nhap" className="font-bold underline underline-offset-2">Đăng nhập</Link>
        </p>
      )}
      {isAdmin && <p role="alert" className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950">Tài khoản quản trị không có hồ sơ sở thích ăn uống.</p>}
      {loadError && <p role="alert" className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950">{loadError}</p>}

      {ai && (
        ai.eligible ? (
          <div role="status" className="mt-6 flex gap-3 rounded-2xl border border-leaf-200 bg-leaf-50 p-4 text-sm leading-6 text-ink-soft">
            <CheckCircle2 aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0 text-leaf-700" />
            <p><strong className="text-ink">Đã đủ thông tin cho AI cá nhân hóa.</strong> AI sẽ gợi ý món và lập thực đơn theo các ràng buộc dưới đây.</p>
          </div>
        ) : (
          <aside role="note" aria-label="Thông tin còn thiếu cho AI cá nhân hóa" className="mt-6 flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950">
            <Sparkles aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0" />
            <div>
              <p>Bạn cần hoàn tất 3 thông tin cơ bản về chế độ ăn chay (Loại ăn chay, Nguyên liệu dị ứng/kiêng, Món không thích) để AI có thể gợi ý chính xác và an toàn.</p>
              <p className="mt-1 font-semibold">Còn thiếu: {ai.missing.map((item) => REQUIREMENT_LABELS[item] || item).join(', ')}.</p>
            </div>
          </aside>
        )
      )}

      {isAuthenticated && !isAdmin && (
        <Card className="mt-6 max-w-3xl border-leaf-100 p-5 shadow-sm shadow-leaf-900/5 md:p-7">
          {loading ? (
            <div className="flex items-center gap-2 py-12 text-sm text-ink-muted"><LoaderCircle aria-hidden="true" className="h-4 w-4 animate-spin" /> Đang tải sở thích…</div>
          ) : (
            <>
              {saved && <p role="status" className="mb-4 flex items-center gap-2 rounded-xl bg-leaf-50 p-3 text-sm font-semibold text-leaf-800"><CheckCircle2 aria-hidden="true" className="h-4 w-4" /> Đã lưu sở thích ăn uống.</p>}
              <DietaryPreferencesForm profile={profile} submitLabel="Lưu thay đổi" disabled={Boolean(loadError)} onSubmit={save} />
            </>
          )}
        </Card>
      )}

      <p className="mt-6 flex max-w-3xl gap-2 text-xs leading-5 text-ink-muted"><Info aria-hidden="true" className="h-4 w-4 shrink-0" /> Khai báo dị ứng giúp cá nhân hóa món ăn, không thay thế tư vấn y tế hay kiểm tra an toàn thực phẩm thực tế.</p>
    </PageContainer>
  );
}
