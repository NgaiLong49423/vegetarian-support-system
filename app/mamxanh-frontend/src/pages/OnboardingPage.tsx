import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { LoaderCircle, Sparkles } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { useAuth } from '../components/AuthContext';
import { DietaryPreferencesForm } from '../components/DietaryPreferencesForm';
import { toProblem } from '../lib/problem';
import { getDietaryPreferences, saveDietaryPreferences, skipOnboarding, type DietaryPreferences } from '../services/dietaryPreferencesApi';

/** UC-31.1 / UC-31.2: one-time questionnaire offered to a new Member right after the first sign-in. */
export function OnboardingPage() {
  const { isAuthenticated, account } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<DietaryPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [skipping, setSkipping] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated || account?.role === 'ADMIN') return;
    getDietaryPreferences()
      .then(setProfile)
      .catch((cause: unknown) => setError(toProblem(cause)?.detail ?? 'Chưa tải được bảng câu hỏi. Bạn có thể bỏ qua và cập nhật sau trong Cài đặt.'))
      .finally(() => setLoading(false));
  }, [isAuthenticated, account?.role]);

  if (!isAuthenticated) return <Navigate to="/dang-nhap" replace />;
  if (account?.role === 'ADMIN') return <Navigate to="/" replace />;

  const complete = async (payload: Parameters<typeof saveDietaryPreferences>[0]) => {
    await saveDietaryPreferences(payload);
    navigate('/kham-pha', { replace: true });
  };

  const skip = async () => {
    setSkipping(true);
    setError(null);
    try {
      await skipOnboarding();
      navigate('/kham-pha', { replace: true });
    } catch (cause) {
      setError(toProblem(cause)?.detail ?? 'Chưa bỏ qua được. Vui lòng thử lại.');
      setSkipping(false);
    }
  };

  return (
    <PageContainer className="py-8 md:py-10">
      <div className="mx-auto max-w-3xl">
        <p className="mb-2 inline-flex items-center gap-2 rounded-full bg-leaf-50 px-3 py-1 text-xs font-bold text-leaf-700"><Sparkles aria-hidden="true" className="h-4 w-4" /> CHÀO MỪNG ĐẾN MÂM XANH</p>
        <h1 className="text-3xl font-extrabold tracking-tight text-ink md:text-4xl">Cho Mâm Xanh biết khẩu vị của bạn</h1>
        <p className="mt-2 text-sm leading-6 text-ink-muted">
          Ba câu đầu giúp AI gợi ý món và lập thực đơn an toàn cho bạn. Bạn có thể bỏ qua: mọi tính năng thông thường vẫn dùng được,
          và bạn hoàn tất sau trong mục Sở thích ăn uống. Thông tin này chỉ mình bạn xem được.
        </p>

        <Card className="mt-6 border-leaf-100 p-5 shadow-sm shadow-leaf-900/5 md:p-7">
          {error && <p role="alert" className="mb-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">{error}</p>}
          {loading && !error ? (
            <div className="flex items-center gap-2 py-12 text-sm text-ink-muted"><LoaderCircle aria-hidden="true" className="h-4 w-4 animate-spin" /> Đang tải bảng câu hỏi…</div>
          ) : (
            <DietaryPreferencesForm
              profile={profile}
              submitLabel="Hoàn tất"
              disabled={skipping}
              onSubmit={complete}
              secondaryAction={<Button type="button" variant="ghost" disabled={skipping} onClick={skip}>Bỏ qua</Button>}
            />
          )}
        </Card>
      </div>
    </PageContainer>
  );
}
