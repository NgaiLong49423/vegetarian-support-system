import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, ArrowLeft, ArrowRight, Check, CircleDollarSign, Cpu, Layers3, LoaderCircle, Pencil, Plus, RefreshCw, Save, Trash2 } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { asApiError } from '../lib/apiClient';
import {
  adminAiConfigurationApi,
  type AdminAiConfiguration,
  type AiModelSetting,
} from '../services/adminAiConfigurationApi';

type ConfigurationSection = 'plans' | 'models' | 'credit';

const sectionTitles: Record<ConfigurationSection, string> = {
  plans: 'Gói và hạn mức token',
  models: 'Gemini model catalog',
  credit: 'AI Credit và nạp tiền',
};
const sectionRoutes: Record<ConfigurationSection, string> = {
  plans: '/admin/ai-config/packages',
  models: '/admin/ai-config/models',
  credit: '/admin/ai-config/credits',
};
const sectionDescriptions: Record<ConfigurationSection, string> = {
  plans: 'Đặt giá và hạn mức token cho từng gói. Giá và quota gói trả phí áp dụng cho lần kích hoạt mới.',
  models: 'Quản lý model Gemini, bật/tắt và quyền truy cập theo từng gói.',
  credit: 'Cấu hình quy đổi AI Credit, mức sử dụng vượt quota và giới hạn nạp tiền.',
};

const fieldClass = 'mt-1 block w-full rounded-xl border border-brand-200 bg-paper px-3 py-2.5 text-sm text-ink focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100';
const tierLabels = { FREE: 'Free', PLUS: 'Plus', PRO: 'Pro' } as const;
const emptyModel = (): AiModelSetting => ({
  modelId: '', displayName: '', enabled: true,
  freeAllowed: false, freeDefault: false,
  plusAllowed: false, plusDefault: false,
  proAllowed: false, proDefault: false,
});

export function AdminAiConfigurationPage({ section }: { section?: ConfigurationSection }) {
  const [configuration, setConfiguration] = useState<AdminAiConfiguration | null>(null);
  const [savedConfiguration, setSavedConfiguration] = useState<AdminAiConfiguration | null>(null);
  const [editingSection, setEditingSection] = useState<ConfigurationSection | null>(null);
  const [confirmSection, setConfirmSection] = useState<ConfigurationSection | null>(null);
  const [confirmStep, setConfirmStep] = useState<1 | 2 | null>(null);
  const [sendEmailNotification, setSendEmailNotification] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const loaded = await adminAiConfigurationApi.get();
      setConfiguration(loaded);
      setSavedConfiguration(loaded);
      setEditingSection(null);
      setConfirmSection(null);
      setConfirmStep(null);
    }
    catch (cause) { setError(asApiError(cause).message); }
    finally { setLoading(false); }
  };

  useEffect(() => { void load(); }, []);

  const updatePlan = (tier: string, key: 'priceVnd' | 'tokenQuota', value: number) => {
    setConfiguration(current => current && ({
      ...current,
      plans: current.plans.map(plan => plan.tier === tier ? {
        ...plan,
        [key]: value,
        ...(tier === 'FREE' && key === 'tokenQuota' ? { pendingTokenQuota: null, pendingEffectiveAt: null } : {}),
      } : plan),
    }));
  };

  const updateModel = (index: number, key: keyof AiModelSetting, value: string | boolean) => {
    setConfiguration(current => current && ({
      ...current,
      models: current.models.map((model, row) => row === index ? { ...model, [key]: value } : model),
    }));
  };

  const startEditing = (section: ConfigurationSection) => {
    if (!configuration || editingSection) return;
    setError(''); setSuccess(''); setSendEmailNotification(false);
    if (section === 'plans') {
      setConfiguration(current => current && ({
        ...current,
        plans: current.plans.map(plan => plan.tier === 'FREE'
          ? { ...plan, tokenQuota: plan.pendingTokenQuota ?? plan.tokenQuota, pendingTokenQuota: null, pendingEffectiveAt: null }
          : plan),
      }));
    }
    setEditingSection(section);
  };

  const cancelEditing = () => {
    if (saving) return;
    setConfiguration(savedConfiguration);
    setEditingSection(null); setConfirmSection(null); setConfirmStep(null); setSendEmailNotification(false); setError('');
  };

  const sectionChanges = (section: ConfigurationSection): string[] => {
    if (!configuration || !savedConfiguration) return [];
    const changes: string[] = [];
    if (section === 'plans') {
      for (const plan of configuration.plans) {
        const before = savedConfiguration.plans.find(item => item.tier === plan.tier);
        if (!before) continue;
        const label = tierLabels[plan.tier];
        if (before.priceVnd !== plan.priceVnd) changes.push(`${label}: Giá ${before.priceVnd.toLocaleString('vi-VN')}đ → ${plan.priceVnd.toLocaleString('vi-VN')}đ`);
        const oldQuota = before.pendingTokenQuota ?? before.tokenQuota;
        if (oldQuota !== plan.tokenQuota) changes.push(`${label}: ${oldQuota.toLocaleString('vi-VN')} → ${plan.tokenQuota.toLocaleString('vi-VN')} token/tháng`);
      }
    } else if (section === 'models') {
      const oldModels = new Map(savedConfiguration.models.map(model => [model.modelId, model]));
      const newModels = new Map(configuration.models.map(model => [model.modelId, model]));
      for (const model of configuration.models) {
        const before = oldModels.get(model.modelId);
        if (!before) {
          changes.push(`Thêm model ${model.modelId || '(chưa có ID)'} — ${model.displayName || '(chưa có tên)'}`);
          continue;
        }
        const fields: string[] = [];
        if (before.displayName !== model.displayName) fields.push(`Tên: ${before.displayName} → ${model.displayName}`);
        if (before.enabled !== model.enabled) fields.push(`Trạng thái: ${before.enabled ? 'Bật' : 'Tắt'} → ${model.enabled ? 'Bật' : 'Tắt'}`);
        for (const tier of ['free', 'plus', 'pro'] as const) {
          const label = tierLabels[tier.toUpperCase() as keyof typeof tierLabels];
          const allowedKey = `${tier}Allowed` as keyof AiModelSetting;
          const defaultKey = `${tier}Default` as keyof AiModelSetting;
          if (before[allowedKey] !== model[allowedKey] || before[defaultKey] !== model[defaultKey]) {
            const describe = (item: AiModelSetting) => !item[allowedKey] ? 'Không cấp' : item[defaultKey] ? 'Mặc định' : 'Cho phép';
            fields.push(`${label}: ${describe(before)} → ${describe(model)}`);
          }
        }
        if (fields.length) changes.push(`${model.modelId}: ${fields.join('; ')}`);
      }
      for (const model of savedConfiguration.models) {
        if (!newModels.has(model.modelId)) changes.push(`Xóa model ${model.modelId} — ${model.displayName}`);
      }
    } else {
      const before = savedConfiguration.credit;
      const after = configuration.credit;
      const fields: Array<[string, number, number]> = [
        ['Credit / 1.000đ', before.creditsPer1000Vnd, after.creditsPer1000Vnd],
        ['Credit / 1.000 token overage', before.creditsPer1000OverageTokens, after.creditsPer1000OverageTokens],
        ['Nạp tối thiểu', before.minimumTopUpVnd, after.minimumTopUpVnd],
        ['Nạp tối đa', before.maximumTopUpVnd, after.maximumTopUpVnd],
      ];
      fields.forEach(([label, oldValue, newValue]) => {
        if (oldValue !== newValue) changes.push(`${label}: ${oldValue.toLocaleString('vi-VN')} → ${newValue.toLocaleString('vi-VN')}`);
      });
    }
    return changes;
  };

  const requestSave = () => {
    if (!editingSection || !configuration) return;
    const changes = sectionChanges(editingSection);
    if (!changes.length) {
      setError('Phần này chưa có thay đổi để lưu.');
      return;
    }
    if (editingSection === 'models') {
      for (const tier of ['free', 'plus', 'pro'] as const) {
        const defaults = configuration.models.filter(model => model.enabled && model[`${tier}Allowed`] && model[`${tier}Default`]);
        const available = configuration.models.some(model => model.enabled && model[`${tier}Allowed`]);
        if (!available || defaults.length !== 1) {
          const label = tierLabels[tier.toUpperCase() as keyof typeof tierLabels];
          setError(`Gói ${label} cần có ít nhất một model đang bật và đúng một model mặc định. Hãy chọn model mặc định trước khi lưu.`);
          return;
        }
      }
    }
    setError(''); setSuccess(''); setSendEmailNotification(false); setConfirmSection(editingSection); setConfirmStep(1);
  };

  const confirmSave = async () => {
    if (!configuration || !confirmSection) return;
    setSaving(true); setError(''); setSuccess('');
    try {
      const saved = await adminAiConfigurationApi.update({
        plans: configuration.plans.map(plan => ({
          ...plan,
          tokenQuota: plan.pendingTokenQuota ?? plan.tokenQuota,
        })),
        models: configuration.models,
        credit: configuration.credit,
        sendEmailNotification,
      });
      setConfiguration(saved);
      setSavedConfiguration(saved);
      setSuccess(`Đã lưu phần “${sectionTitles[confirmSection]}”. Thông báo in-app được gửi theo chính sách cấu hình.`);
      setEditingSection(null); setConfirmSection(null); setConfirmStep(null); setSendEmailNotification(false);
    } catch (cause) { setError(asApiError(cause).message); }
    finally { setSaving(false); }
  };

  const sectionHeader = (section: ConfigurationSection, description: string) => (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div><h2 className="text-xl font-extrabold text-ink">{sectionTitles[section]}</h2><p className="mt-1 text-sm text-ink-muted">{description}</p></div>
      {editingSection === section
        ? <div className="flex gap-2"><Button variant="outline" onClick={cancelEditing} disabled={saving}>Hủy</Button><Button onClick={requestSave} disabled={saving}><Save className="mr-2 h-4 w-4" />Lưu phần này</Button></div>
        : <Button variant="outline" onClick={() => startEditing(section)} disabled={Boolean(editingSection)}><Pencil className="mr-2 h-4 w-4" />Sửa</Button>}
    </div>
  );

  if (loading) return <PageContainer className="py-10"><div className="flex items-center gap-2 text-ink-muted"><LoaderCircle className="h-5 w-5 animate-spin" />Đang tải cấu hình AI…</div></PageContainer>;
  if (!configuration) return <PageContainer className="py-10"><Card className="p-6"><p role="alert" className="text-sm text-red-700">{error || 'Không tải được cấu hình.'}</p><Button className="mt-4" onClick={() => void load()}><RefreshCw className="mr-2 h-4 w-4" />Thử lại</Button></Card></PageContainer>;

  return (
    <PageContainer className="space-y-6 py-6 sm:py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="text-xs font-extrabold uppercase tracking-[0.16em] text-leaf-700">Quản trị hệ thống</p><h1 className="mt-2 text-3xl font-black text-ink">{section ? sectionTitles[section] : 'Cấu hình AI'}</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-ink-muted">{section ? sectionDescriptions[section] : 'Tổng quan các cấu hình AI. Chọn một mục để xem chi tiết, chỉnh sửa và xác nhận thay đổi.'}</p></div>
      <Button onClick={() => void load()} disabled={saving || Boolean(editingSection)} variant="outline"><RefreshCw className="mr-2 h-4 w-4" />Tải lại</Button>
      </div>

      {error && <div role="alert" className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{error}</div>}
      {success && <div role="status" className="flex items-start gap-2 rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-800"><Check className="mt-0.5 h-4 w-4 shrink-0" />{success}</div>}

      {section && <nav aria-label="Các mục cấu hình AI" className="flex flex-wrap gap-2">
        <Link to="/admin/ai-config" className="inline-flex min-h-10 items-center gap-2 rounded-full border border-brand-200 bg-paper px-4 text-sm font-semibold text-ink-soft hover:text-brand-700"><ArrowLeft className="h-4 w-4" />Tổng quan</Link>
        {(['plans', 'models', 'credit'] as const).map(item => <Link key={item} aria-current={section === item ? 'page' : undefined} to={sectionRoutes[item]} className={`inline-flex min-h-10 items-center rounded-full border px-4 text-sm font-semibold ${section === item ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-brand-200 bg-paper text-ink-soft hover:text-brand-700'}`}>{sectionTitles[item]}</Link>)}
      </nav>}

      {!section && <div className="grid gap-4 md:grid-cols-3">{(['plans', 'models', 'credit'] as const).map(item => {
        const Icon = item === 'plans' ? Layers3 : item === 'models' ? Cpu : CircleDollarSign;
        const summary = item === 'plans'
          ? `${configuration.plans.length} gói · ${configuration.plans.find(plan => plan.tier === 'PLUS')?.tokenQuota.toLocaleString('vi-VN') ?? '—'} token Plus`
          : item === 'models'
            ? `${configuration.models.length} model · ${configuration.models.filter(model => model.enabled).length} đang bật`
            : `${configuration.credit.creditsPer1000Vnd.toLocaleString('vi-VN')} Credit / 1.000đ · nạp ${configuration.credit.minimumTopUpVnd.toLocaleString('vi-VN')}–${configuration.credit.maximumTopUpVnd.toLocaleString('vi-VN')}đ`;
        return <Link key={item} to={sectionRoutes[item]} className="group rounded-2xl border border-brand-100 bg-paper p-5 transition hover:border-brand-300 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400">
          <div className="flex items-start justify-between gap-3"><span className="rounded-xl bg-brand-50 p-3 text-brand-700"><Icon className="h-5 w-5" /></span><ArrowRight className="mt-2 h-4 w-4 text-ink-muted transition group-hover:translate-x-1 group-hover:text-brand-700" /></div>
          <h2 className="mt-4 text-lg font-extrabold text-ink">{sectionTitles[item]}</h2><p className="mt-1 text-sm leading-5 text-ink-muted">{sectionDescriptions[item]}</p><p className="mt-4 rounded-xl bg-raised px-3 py-2 text-sm font-semibold text-ink-soft">{summary}</p>
        </Link>;
      })}</div>}

      {section === 'plans' && <Card className="p-5 sm:p-6">
        {sectionHeader('plans', 'Token Free tính theo tháng dương lịch, giờ Việt Nam; Plus/Pro tính theo từng tháng trong chu kỳ gói. Giá và quota Plus/Pro áp dụng cho lần kích hoạt mới.')}
        <div className="mt-5 grid gap-4 md:grid-cols-3">
          {configuration.plans.map(plan => <section key={plan.tier} className="rounded-2xl border border-brand-100 bg-paper p-4">
            <h3 className="font-bold text-ink">{tierLabels[plan.tier]}</h3>
            {editingSection === 'plans' ? <>
              <label className="mt-4 block text-sm font-semibold text-ink">Giá (VND)<input className={fieldClass} type="number" min="0" step="1000" value={plan.priceVnd} disabled={plan.tier === 'FREE'} onChange={event => updatePlan(plan.tier, 'priceVnd', Number(event.target.value))} /></label>
              <label className="mt-3 block text-sm font-semibold text-ink">Token mỗi tháng<input className={fieldClass} type="number" min="0" step="1000" value={plan.tokenQuota} onChange={event => updatePlan(plan.tier, 'tokenQuota', Number(event.target.value))} /></label>
            </> : <dl className="mt-4 grid grid-cols-2 gap-3 text-sm"><div><dt className="text-ink-muted">Giá</dt><dd className="mt-1 font-bold text-ink">{plan.priceVnd.toLocaleString('vi-VN')}đ</dd></div><div><dt className="text-ink-muted">Token mỗi tháng</dt><dd className="mt-1 font-bold text-ink">{plan.tokenQuota.toLocaleString('vi-VN')}</dd></div></dl>}
            {plan.pendingTokenQuota !== null && <p className="mt-3 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-900">Free sắp đổi thành {plan.pendingTokenQuota.toLocaleString('vi-VN')} token từ {plan.pendingEffectiveAt ? new Date(plan.pendingEffectiveAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : 'chu kỳ tới'}.</p>}
          </section>)}
        </div>
      </Card>}

      {section === 'models' && <Card className="p-5 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">{sectionHeader('models', 'Model ID mới được kiểm tra với Gemini trước khi lưu; mỗi tier cần đúng một model mặc định đang bật.')}{editingSection === 'models' && <Button variant="outline" onClick={() => setConfiguration(current => current && ({ ...current, models: [...current.models, emptyModel()] }))}><Plus className="mr-2 h-4 w-4" />Thêm model</Button>}</div>
        <div className="mt-5 space-y-4">
          {configuration.models.map((model, index) => <section key={`${model.modelId}-${index}`} className="rounded-2xl border border-brand-100 p-4">
            {editingSection === 'models' ? <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto_auto] md:items-end">
              <label className="text-sm font-semibold text-ink">Model ID<input className={fieldClass} placeholder="gemini-model-id" value={model.modelId} onChange={event => updateModel(index, 'modelId', event.target.value)} /></label>
              <label className="text-sm font-semibold text-ink">Tên hiển thị<input className={fieldClass} placeholder="Gemini model" value={model.displayName} onChange={event => updateModel(index, 'displayName', event.target.value)} /></label>
              <label className="flex items-center gap-2 pb-3 text-sm font-semibold text-ink"><input type="checkbox" checked={model.enabled} onChange={event => updateModel(index, 'enabled', event.target.checked)} />Đang bật</label>
              <Button aria-label="Xóa model khỏi cấu hình" variant="outline" onClick={() => setConfiguration(current => current && ({ ...current, models: current.models.filter((_, row) => row !== index) }))}><Trash2 className="h-4 w-4" /></Button>
            </div> : <div className="flex flex-wrap items-baseline justify-between gap-2"><div><h3 className="font-bold text-ink">{model.displayName}</h3><p className="text-xs text-ink-muted">{model.modelId}</p></div><span className={`rounded-full px-3 py-1 text-xs font-semibold ${model.enabled ? 'bg-green-100 text-green-800' : 'bg-raised text-ink-muted'}`}>{model.enabled ? 'Đang bật' : 'Đang tắt'}</span></div>}
            <div className="mt-4 grid gap-3 sm:grid-cols-3">{(['FREE', 'PLUS', 'PRO'] as const).map(tier => {
              const allowedKey = `${tier.toLowerCase()}Allowed` as keyof AiModelSetting;
              const defaultKey = `${tier.toLowerCase()}Default` as keyof AiModelSetting;
              const describe = !model[allowedKey] ? 'Không cấp' : model[defaultKey] ? 'Mặc định' : 'Cho phép';
              return <div key={tier} className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl bg-raised px-3 py-2.5 text-sm"><strong>{tierLabels[tier]}</strong>{editingSection === 'models' ? <><label className="flex items-center gap-2"><input type="checkbox" checked={Boolean(model[allowedKey])} onChange={event => updateModel(index, allowedKey, event.target.checked)} />Cho phép</label><label className="flex items-center gap-2"><input type="radio" name={`default-${tier}`} checked={Boolean(model[defaultKey])} disabled={!model[`${tier.toLowerCase()}Allowed` as keyof AiModelSetting]} onChange={() => setConfiguration(current => current && ({ ...current, models: current.models.map((entry, row) => ({ ...entry, [`${tier.toLowerCase()}Default`]: row === index })) }))} />Mặc định</label></> : <span className="text-ink-muted">{describe}</span>}</div>;
            })}</div>
          </section>)}
        </div>
      </Card>}

      {section === 'credit' && <Card className="p-5 sm:p-6">
        {sectionHeader('credit', 'Tỷ lệ overage có hiệu lực ngay; tỷ lệ nạp và giới hạn áp dụng cho đơn mới.')}
        <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {([['creditsPer1000Vnd', 'Credit / 1.000đ'], ['creditsPer1000OverageTokens', 'Credit / 1.000 token overage'], ['minimumTopUpVnd', 'Nạp tối thiểu (VND)'], ['maximumTopUpVnd', 'Nạp tối đa (VND)']] as const).map(([key, label]) => <div key={key} className="text-sm font-semibold text-ink">{editingSection === 'credit' ? <label>{label}<input className={fieldClass} type="number" min="1" value={configuration.credit[key]} onChange={event => setConfiguration(current => current && ({ ...current, credit: { ...current.credit, [key]: Number(event.target.value) } }))} /></label> : <><p className="text-ink-muted">{label}</p><p className="mt-2 text-lg font-bold">{configuration.credit[key].toLocaleString('vi-VN')}</p></>}</div>)}
        </div>
      </Card>}

      {confirmSection && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !saving) setConfirmSection(null); }}>
        <section role="dialog" aria-modal="true" aria-labelledby="ai-config-confirm-title" className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-paper p-6 shadow-2xl">
          <p className="text-xs font-bold uppercase tracking-wide text-brand-700">Bước {confirmStep} / 2</p>
          <h2 id="ai-config-confirm-title" className="mt-1 text-xl font-extrabold text-ink">{confirmStep === 1 ? `Kiểm tra thay đổi: ${sectionTitles[confirmSection]}` : 'Xác nhận áp dụng cấu hình'}</h2>
          <p className="mt-2 text-sm text-ink-muted">{confirmStep === 1 ? 'Rà soát các giá trị cũ và mới. Chưa có dữ liệu nào được lưu ở bước này.' : 'Kiểm tra lần cuối phạm vi ảnh hưởng. Chỉ khi bấm nút bên dưới, hệ thống mới cập nhật cấu hình.'}</p>
          <ul className="mt-4 space-y-2 rounded-xl bg-raised p-4 text-sm text-ink">{sectionChanges(confirmSection).map((change, index) => <li key={index} className="list-inside list-disc">{change}</li>)}</ul>
          <div className="mt-4 rounded-xl border border-brand-100 p-4 text-sm leading-6 text-ink-soft"><strong className="text-ink">Ảnh hưởng khi áp dụng</strong><p className="mt-1">{confirmSection === 'plans' ? 'Giá và quota Plus/Pro áp dụng cho lần kích hoạt mới; thay đổi quota Free có hiệu lực từ chu kỳ tháng kế tiếp. Chu kỳ gói đang chạy giữ nguyên snapshot.' : confirmSection === 'models' ? 'Quyền model theo từng gói có hiệu lực sau khi lưu. Model mới sẽ được kiểm tra với Gemini trước khi hệ thống ghi cấu hình.' : 'Tỷ lệ overage mới có hiệu lực cho lượt AI mới; tỷ lệ quy đổi/nạp và giới hạn mới áp dụng cho đơn nạp mới. Đơn đang chờ giữ nguyên snapshot.'}</p><p className="mt-1">Hệ thống gửi thông báo in-app về giá trị thay đổi và thời điểm hiệu lực tới các tài khoản đủ điều kiện.</p>{confirmStep === 2 && <p className="mt-1">{sendEmailNotification ? 'Email bổ sung sẽ được gửi bất đồng bộ tới người dùng đủ điều kiện.' : 'Không gửi email bổ sung.'}</p>}</div>
          {confirmStep === 1 && <label className="mt-4 flex items-start gap-3 text-sm text-ink"><input className="mt-1" type="checkbox" checked={sendEmailNotification} onChange={event => setSendEmailNotification(event.target.checked)} /><span><strong className="block">Gửi thêm email thông báo</strong><span className="text-ink-muted">Email gửi bất đồng bộ tới khách hàng/chuyên gia đang hoạt động và đã xác minh email.</span></span></label>}
          {error && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
          <div className="mt-6 flex flex-wrap justify-end gap-3"><Button variant="outline" onClick={() => confirmStep === 2 ? setConfirmStep(1) : (setConfirmSection(null), setConfirmStep(null))} disabled={saving}>{confirmStep === 2 ? 'Xem lại thay đổi' : 'Quay lại chỉnh sửa'}</Button>{confirmStep === 1 ? <Button onClick={() => setConfirmStep(2)}>Tiếp tục xác nhận<ArrowRight className="ml-2 h-4 w-4" /></Button> : <Button onClick={() => void confirmSave()} disabled={saving}>{saving && <LoaderCircle className="mr-2 h-4 w-4 animate-spin" />}{sendEmailNotification ? 'Đồng ý, áp dụng và gửi email' : 'Đồng ý, áp dụng thay đổi'}</Button>}</div>
        </section>
      </div>}
    </PageContainer>
  );
}
