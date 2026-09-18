import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Check, ImagePlus, Info, Plus, Sparkles, Trash2, UploadCloud } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card, ProgressBar } from '../components/ui';
import type { DietTag } from '../types';

const dietOptions: { value: DietTag; label: string; desc: string }[] = [
  { value: 'Thuần Chay', label: 'Thuần Chay (Vegan)', desc: '100% nguyên liệu thực vật, không sản phẩm động vật' },
  { value: 'Lacto', label: 'Chay Có Sữa (Lacto)', desc: 'Có kết hợp sữa và các chế phẩm từ sữa' },
  { value: 'Ovo', label: 'Chay Có Trứng (Ovo)', desc: 'Có kết hợp trứng gia cầm' },
  { value: 'Lacto-Ovo', label: 'Chay Trứng & Sữa', desc: 'Linh hoạt cả trứng và sữa trong chế biến' },
];

interface Row { id: string; name: string; qty: string; unit: string }

export function CreateRecipe() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [diet, setDiet] = useState<DietTag>('Thuần Chay');
  const [rows, setRows] = useState<Row[]>([
    { id: '1', name: 'Đậu hũ non Nhật Bản', qty: '300', unit: 'gram' },
    { id: '2', name: 'Nấm đông cô tươi', qty: '150', unit: 'gram' },
    { id: '3', name: 'Tiêu xanh Phú Quốc', qty: '2', unit: 'nhánh' },
  ]);
  const [saved, setSaved] = useState(false);

  const completeness = Math.round(
    ([title, desc].filter(Boolean).length / 2) * 40 + (rows.filter((r) => r.name && r.qty).length / Math.max(rows.length, 1)) * 60,
  );

  const checklist = [
    { label: 'Đặt tên món hấp dẫn', done: title.length > 3 },
    { label: 'Mô tả câu chuyện món ăn', done: desc.length > 10 },
    { label: 'Chọn chế độ ăn chay', done: true },
    { label: 'Có tối thiểu 3 nguyên liệu', done: rows.filter((r) => r.name).length >= 3 },
  ];

  const addRow = () => setRows((r) => [...r, { id: `${Date.now()}`, name: '', qty: '', unit: 'gram' }]);
  const removeRow = (id: string) => setRows((r) => r.filter((x) => x.id !== id));
  const updateRow = (id: string, patch: Partial<Row>) => setRows((r) => r.map((x) => (x.id === id ? { ...x, ...patch } : x)));

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Đăng công thức</span>
      </nav>

      <div className="mb-6 flex flex-col items-start justify-between gap-2 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Đăng công thức món chay mới</h1>
          <p className="text-sm text-ink-muted">Chia sẻ món chay tâm huyết của bạn tới cộng đồng Mâm Xanh.</p>
        </div>
        <Button variant="outline"><Sparkles className="h-4 w-4" /> Tự động điền bằng Gemini AI</Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-5">
          {/* Step 1 */}
          <Card className="p-6">
            <StepHead num={1} title="Thông tin cơ bản" />
            <div className="space-y-4">
              <Field label="Tên món ăn *">
                <input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="VD: Đậu hũ non sốt nấm đông cô tiêu xanh"
                  className="w-full rounded-xl border border-brand-200 bg-white px-4 py-3 text-sm outline-none focus:border-brand-400"
                />
              </Field>
              <Field label="Mô tả & câu chuyện món ăn *">
                <textarea
                  value={desc}
                  onChange={(e) => setDesc(e.target.value)}
                  rows={4}
                  placeholder="Chia sẻ nguồn cảm hứng, hương vị đặc trưng và bí quyết của món ăn..."
                  className="w-full resize-none rounded-xl border border-brand-200 bg-white px-4 py-3 text-sm outline-none focus:border-brand-400"
                />
              </Field>
              <Field label="Chế độ ăn chay">
                <div className="grid gap-3 sm:grid-cols-2">
                  {dietOptions.map((opt) => (
                    <button
                      key={opt.value}
                      onClick={() => setDiet(opt.value)}
                      className={`rounded-xl border p-3 text-left transition-colors ${diet === opt.value ? 'border-brand-600 bg-brand-50 ring-1 ring-brand-600' : 'border-brand-200 bg-white hover:border-brand-300'}`}
                    >
                      <p className="flex items-center gap-2 font-semibold text-ink">
                        <span className={`flex h-4 w-4 items-center justify-center rounded-full border ${diet === opt.value ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-300'}`}>
                          {diet === opt.value && <Check className="h-2.5 w-2.5" />}
                        </span>
                        {opt.label}
                      </p>
                      <p className="mt-1 pl-6 text-xs text-ink-muted">{opt.desc}</p>
                    </button>
                  ))}
                </div>
              </Field>
              <div className="grid gap-3 sm:grid-cols-4">
                <Field label="Chuẩn bị"><InputUnit unit="phút" placeholder="15" /></Field>
                <Field label="Nấu ăn"><InputUnit unit="phút" placeholder="20" /></Field>
                <Field label="Khẩu phần"><InputUnit unit="người" placeholder="4" /></Field>
                <Field label="Độ khó">
                  <select className="w-full rounded-xl border border-brand-200 bg-white px-3 py-3 text-sm outline-none focus:border-brand-400">
                    <option>Dễ</option><option>Trung bình</option><option>Khó</option>
                  </select>
                </Field>
              </div>
            </div>
          </Card>

          {/* Step 2 */}
          <Card className="p-6">
            <StepHead num={2} title="Hình ảnh" />
            <div className="grid gap-3 sm:grid-cols-3">
              <div className="flex aspect-square flex-col items-center justify-center rounded-xl border-2 border-dashed border-brand-200 bg-brand-50/50 text-center transition-colors hover:border-brand-400 hover:bg-brand-50">
                <UploadCloud className="mb-2 h-7 w-7 text-brand-500" />
                <p className="px-4 text-xs font-semibold text-brand-600">Kéo thả hoặc chọn ảnh bìa</p>
                <p className="text-[10px] text-ink-muted">JPG, PNG tối đa 5MB</p>
              </div>
              <div className="flex aspect-square items-center justify-center rounded-xl border border-brand-100 bg-brand-50/40 text-ink-muted">
                <ImagePlus className="h-6 w-6" />
              </div>
              <div className="flex aspect-square items-center justify-center rounded-xl border border-brand-100 bg-brand-50/40 text-ink-muted">
                <ImagePlus className="h-6 w-6" />
              </div>
            </div>
          </Card>

          {/* Step 3 */}
          <Card className="p-6">
            <StepHead num={3} title="Danh sách nguyên liệu chuẩn hoá" />
            <div className="mb-3 flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-xs text-amber-800">
              <Info className="mt-0.5 h-4 w-4 shrink-0" />
              Hệ thống sẽ tự động chuẩn hoá nguyên liệu để tính toán chính xác 9 chỉ tiêu dinh dưỡng cho món ăn của bạn.
            </div>
            <div className="space-y-2">
              {rows.map((row, i) => (
                <div key={row.id} className="flex items-center gap-2">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-100 text-xs font-bold text-brand-600">{i + 1}</span>
                  <input
                    value={row.name}
                    onChange={(e) => updateRow(row.id, { name: e.target.value })}
                    placeholder="Tên nguyên liệu"
                    className="min-w-0 flex-[2] rounded-lg border border-brand-200 bg-white px-3 py-2 text-sm outline-none focus:border-brand-400"
                  />
                  <input
                    value={row.qty}
                    onChange={(e) => updateRow(row.id, { qty: e.target.value })}
                    placeholder="SL"
                    className="w-16 rounded-lg border border-brand-200 bg-white px-3 py-2 text-sm outline-none focus:border-brand-400"
                  />
                  <select
                    value={row.unit}
                    onChange={(e) => updateRow(row.id, { unit: e.target.value })}
                    className="w-24 rounded-lg border border-brand-200 bg-white px-2 py-2 text-sm outline-none focus:border-brand-400"
                  >
                    {['gram', 'ml', 'củ', 'quả', 'nhánh', 'muỗng', 'hộp'].map((u) => <option key={u}>{u}</option>)}
                  </select>
                  <button onClick={() => removeRow(row.id)} className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-ink-muted hover:bg-brand-100 hover:text-red-500">
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
            <button onClick={addRow} className="mt-3 inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:underline">
              <Plus className="h-4 w-4" /> Thêm nguyên liệu
            </button>
          </Card>

          <div className="flex justify-end gap-3">
            <Button variant="outline">Lưu nháp</Button>
            <Button onClick={() => { setSaved(true); setTimeout(() => navigate('/kham-pha'), 1200); }}>
              {saved ? <><Check className="h-4 w-4" /> Đã đăng!</> : 'Xuất bản công thức'}
            </Button>
          </div>
        </div>

        {/* live sidebar */}
        <aside className="space-y-4">
          <div className="sticky top-20 space-y-4">
            <Card className="p-5">
              <p className="text-xs font-bold uppercase tracking-wider text-brand-600">Dự đoán 9 chỉ tiêu dinh dưỡng</p>
              <div className="mt-3 flex items-end gap-4">
                <div>
                  <p className="text-3xl font-extrabold text-ink">215</p>
                  <p className="text-xs text-ink-muted">calo/phần ước tính</p>
                </div>
                <div>
                  <p className="text-3xl font-extrabold text-leaf-600">14.8<span className="text-base">g</span></p>
                  <p className="text-xs text-ink-muted">đạm thực vật</p>
                </div>
              </div>
              <div className="mt-4 space-y-3">
                {[
                  { l: 'Chất xơ hoà tan', v: 76 },
                  { l: 'Carbohydrate', v: 62 },
                  { l: 'Chất béo tốt', v: 48 },
                ].map((x) => (
                  <div key={x.l}>
                    <div className="mb-1 flex justify-between text-xs"><span className="text-ink-soft">{x.l}</span><span className="font-semibold text-ink">{x.v}%</span></div>
                    <ProgressBar pct={x.v} tone="leaf" />
                  </div>
                ))}
              </div>
              <p className="mt-4 text-[11px] leading-relaxed text-ink-muted">
                Số liệu tự động ước tính theo cơ sở dữ liệu Viện Dinh Dưỡng & USDA, cập nhật ngay khi bạn thay đổi nguyên liệu.
              </p>
            </Card>

            <Card className="p-5">
              <div className="mb-3 flex items-center justify-between">
                <h3 className="font-bold text-ink">Tiêu chuẩn bài viết PRO</h3>
                <Badge tone={completeness >= 80 ? 'leaf' : 'brand'}>{completeness}%</Badge>
              </div>
              <ProgressBar pct={completeness} tone={completeness >= 80 ? 'leaf' : 'brand'} />
              <ul className="mt-4 space-y-2.5">
                {checklist.map((c) => (
                  <li key={c.label} className="flex items-center gap-2 text-sm">
                    <span className={`flex h-5 w-5 items-center justify-center rounded-full ${c.done ? 'bg-leaf-500 text-white' : 'bg-brand-100 text-brand-400'}`}>
                      <Check className="h-3 w-3" />
                    </span>
                    <span className={c.done ? 'text-ink-soft' : 'text-ink-muted'}>{c.label}</span>
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        </aside>
      </div>
    </PageContainer>
  );
}

function StepHead({ num, title }: { num: number; title: string }) {
  return (
    <div className="mb-5 flex items-center gap-3">
      <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{num}</span>
      <h2 className="text-lg font-extrabold text-ink">{title}</h2>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-semibold text-ink-soft">{label}</label>
      {children}
    </div>
  );
}

function InputUnit({ unit, placeholder }: { unit: string; placeholder: string }) {
  return (
    <div className="flex items-center rounded-xl border border-brand-200 bg-white pr-3 focus-within:border-brand-400">
      <input placeholder={placeholder} className="w-full bg-transparent px-3 py-3 text-sm outline-none" />
      <span className="text-xs text-ink-muted">{unit}</span>
    </div>
  );
}
