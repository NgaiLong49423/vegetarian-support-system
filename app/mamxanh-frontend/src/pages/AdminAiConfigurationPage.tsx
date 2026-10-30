import { useEffect, useState } from 'react';
import { AlertCircle, Check, LoaderCircle, Plus, RefreshCw, Save, Trash2 } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { asApiError } from '../lib/apiClient';
import {
  adminAiConfigurationApi,
  type AdminAiConfiguration,
  type AiModelSetting,
} from '../services/adminAiConfigurationApi';

const fieldClass = 'mt-1 block w-full rounded-xl border border-brand-200 bg-paper px-3 py-2.5 text-sm text-ink focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100';
const tierLabels = { FREE: 'Free', PLUS: 'Plus', PRO: 'Pro' } as const;
const emptyModel = (): AiModelSetting => ({
  modelId: '', displayName: '', enabled: true,
  freeAllowed: false, freeDefault: false,
  plusAllowed: false, plusDefault: false,
  proAllowed: false, proDefault: false,
});

export function AdminAiConfigurationPage() {
  const [configuration, setConfiguration] = useState<AdminAiConfiguration | null>(null);
  const [sendEmailNotification, setSendEmailNotification] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try { setConfiguration(await adminAiConfigurationApi.get()); }
    catch (cause) { setError(asApiError(cause).message); }
    finally { setLoading(false); }
  };

  useEffect(() => { void load(); }, []);

  const updatePlan = (tier: string, key: 'priceVnd' | 'tokenQuota', value: number) => {
    setConfiguration(current => current && ({
      ...current,
      plans: current.plans.map(plan => plan.tier === tier ? { ...plan, [key]: value } : plan),
    }));
  };

  const updateModel = (index: number, key: keyof AiModelSetting, value: string | boolean) => {
    setConfiguration(current => current && ({
      ...current,
      models: current.models.map((model, row) => row === index ? { ...model, [key]: value } : model),
    }));
  };

  const save = async () => {
    if (!configuration) return;
    setSaving(true); setError(''); setSuccess('');
    try {
      const saved = await adminAiConfigurationApi.update({ ...configuration, sendEmailNotification });
      setConfiguration(saved);
      setSuccess('Đã lưu cấu hình AI. Các thay đổi hiệu lực theo chính sách từng cấu hình.');
    } catch (cause) { setError(asApiError(cause).message); }
    finally { setSaving(false); }
  };

  if (loading) return <PageContainer className="py-10"><div className="flex items-center gap-2 text-ink-muted"><LoaderCircle className="h-5 w-5 animate-spin" />Đang tải cấu hình AI…</div></PageContainer>;
  if (!configuration) return <PageContainer className="py-10"><Card className="p-6"><p role="alert" className="text-sm text-red-700">{error || 'Không tải được cấu hình.'}</p><Button className="mt-4" onClick={() => void load()}><RefreshCw className="mr-2 h-4 w-4" />Thử lại</Button></Card></PageContainer>;

  return (
    <PageContainer className="space-y-6 py-6 sm:py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="text-xs font-extrabold uppercase tracking-[0.16em] text-leaf-700">Quản trị hệ thống</p><h1 className="mt-2 text-3xl font-black text-ink">Cấu hình AI</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-ink-muted">Quản lý gói, Gemini model và quy đổi AI Credit. Cấu hình mới không thay đổi snapshot của chu kỳ gói đã kích hoạt.</p></div>
        <Button onClick={() => void load()} disabled={saving} variant="outline"><RefreshCw className="mr-2 h-4 w-4" />Tải lại</Button>
      </div>

      {error && <div role="alert" className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{error}</div>}
      {success && <div role="status" className="flex items-start gap-2 rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-800"><Check className="mt-0.5 h-4 w-4 shrink-0" />{success}</div>}

      <Card className="p-5 sm:p-6">
        <h2 className="text-xl font-extrabold text-ink">Gói và hạn mức token</h2>
        <p className="mt-1 text-sm text-ink-muted">Token Free tính theo tháng dương lịch, giờ Việt Nam; Plus/Pro tính theo từng tháng trong chu kỳ gói. Giá và quota Plus/Pro áp dụng cho lần kích hoạt mới.</p>
        <div className="mt-5 grid gap-4 md:grid-cols-3">
          {configuration.plans.map(plan => <section key={plan.tier} className="rounded-2xl border border-brand-100 bg-paper p-4">
            <h3 className="font-bold text-ink">{tierLabels[plan.tier]}</h3>
            <label className="mt-4 block text-sm font-semibold text-ink">Giá (VND)<input className={fieldClass} type="number" min="0" step="1000" value={plan.priceVnd} disabled={plan.tier === 'FREE'} onChange={event => updatePlan(plan.tier, 'priceVnd', Number(event.target.value))} /></label>
            <label className="mt-3 block text-sm font-semibold text-ink">Token mỗi tháng<input className={fieldClass} type="number" min="0" step="1000" value={plan.pendingTokenQuota ?? plan.tokenQuota} onChange={event => updatePlan(plan.tier, 'tokenQuota', Number(event.target.value))} /></label>
            {plan.pendingTokenQuota !== null && <p className="mt-3 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-900">Free sắp đổi thành {plan.pendingTokenQuota.toLocaleString('vi-VN')} token từ {plan.pendingEffectiveAt ? new Date(plan.pendingEffectiveAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : 'chu kỳ tới'}.</p>}
          </section>)}
        </div>
      </Card>

      <Card className="p-5 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-xl font-extrabold text-ink">Gemini model catalog</h2><p className="mt-1 text-sm text-ink-muted">Model ID mới được kiểm tra với Gemini trước khi lưu; mỗi tier cần đúng một model mặc định đang bật.</p></div><Button variant="outline" onClick={() => setConfiguration(current => current && ({ ...current, models: [...current.models, emptyModel()] }))}><Plus className="mr-2 h-4 w-4" />Thêm model</Button></div>
        <div className="mt-5 space-y-4">
          {configuration.models.map((model, index) => <section key={`${model.modelId}-${index}`} className="rounded-2xl border border-brand-100 p-4">
            <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto_auto] md:items-end">
              <label className="text-sm font-semibold text-ink">Model ID<input className={fieldClass} placeholder="gemini-model-id" value={model.modelId} onChange={event => updateModel(index, 'modelId', event.target.value)} /></label>
              <label className="text-sm font-semibold text-ink">Tên hiển thị<input className={fieldClass} placeholder="Gemini model" value={model.displayName} onChange={event => updateModel(index, 'displayName', event.target.value)} /></label>
              <label className="flex items-center gap-2 pb-3 text-sm font-semibold text-ink"><input type="checkbox" checked={model.enabled} onChange={event => updateModel(index, 'enabled', event.target.checked)} />Đang bật</label>
              <Button aria-label="Xóa model khỏi cấu hình" variant="outline" onClick={() => setConfiguration(current => current && ({ ...current, models: current.models.filter((_, row) => row !== index) }))}><Trash2 className="h-4 w-4" /></Button>
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">{(['FREE', 'PLUS', 'PRO'] as const).map(tier => {
              const allowedKey = `${tier.toLowerCase()}Allowed` as keyof AiModelSetting;
              const defaultKey = `${tier.toLowerCase()}Default` as keyof AiModelSetting;
              return <div key={tier} className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl bg-raised px-3 py-2.5 text-sm"><strong>{tierLabels[tier]}</strong><label className="flex items-center gap-2"><input type="checkbox" checked={Boolean(model[allowedKey])} onChange={event => updateModel(index, allowedKey, event.target.checked)} />Cho phép</label><label className="flex items-center gap-2"><input type="radio" name={`default-${tier}`} checked={Boolean(model[defaultKey])} disabled={!model[`${tier.toLowerCase()}Allowed` as keyof AiModelSetting]} onChange={() => setConfiguration(current => current && ({ ...current, models: current.models.map((entry, row) => ({ ...entry, [`${tier.toLowerCase()}Default`]: row === index })) }))} />Mặc định</label></div>;
            })}</div>
          </section>)}
        </div>
      </Card>

      <Card className="p-5 sm:p-6">
        <h2 className="text-xl font-extrabold text-ink">AI Credit và nạp tiền</h2><p className="mt-1 text-sm text-ink-muted">Tỷ lệ overage có hiệu lực ngay; tỷ lệ nạp và giới hạn áp dụng cho đơn mới.</p>
        <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <label className="text-sm font-semibold text-ink">Credit / 1.000đ<input className={fieldClass} type="number" min="0.000001" step="1" value={configuration.credit.creditsPer1000Vnd} onChange={event => setConfiguration(current => current && ({ ...current, credit: { ...current.credit, creditsPer1000Vnd: Number(event.target.value) } }))} /></label>
          <label className="text-sm font-semibold text-ink">Credit / 1.000 token overage<input className={fieldClass} type="number" min="1" value={configuration.credit.creditsPer1000OverageTokens} onChange={event => setConfiguration(current => current && ({ ...current, credit: { ...current.credit, creditsPer1000OverageTokens: Number(event.target.value) } }))} /></label>
          <label className="text-sm font-semibold text-ink">Nạp tối thiểu (VND)<input className={fieldClass} type="number" min="1" step="1000" value={configuration.credit.minimumTopUpVnd} onChange={event => setConfiguration(current => current && ({ ...current, credit: { ...current.credit, minimumTopUpVnd: Number(event.target.value) } }))} /></label>
          <label className="text-sm font-semibold text-ink">Nạp tối đa (VND)<input className={fieldClass} type="number" min="1" step="1000" value={configuration.credit.maximumTopUpVnd} onChange={event => setConfiguration(current => current && ({ ...current, credit: { ...current.credit, maximumTopUpVnd: Number(event.target.value) } }))} /></label>
        </div>
      </Card>

      <Card className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
        <label className="flex items-start gap-3 text-sm text-ink"><input className="mt-1" type="checkbox" checked={sendEmailNotification} onChange={event => setSendEmailNotification(event.target.checked)} /><span><strong className="block">Gửi email thông báo thay đổi</strong><span className="text-ink-muted">Thông báo in-app luôn được gửi tới Customer/Expert đang hoạt động.</span></span></label>
        <Button onClick={() => void save()} disabled={saving}>{saving ? <LoaderCircle className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}Lưu cấu hình</Button>
      </Card>
    </PageContainer>
  );
}
