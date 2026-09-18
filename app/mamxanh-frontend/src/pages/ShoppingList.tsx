import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Check,
  ClipboardCopy,
  Download,
  FileText,
  Plus,
  Trash2,
  X,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, ProgressBar } from '../components/ui';
import { shoppingList as initialList, nutritionTargets } from '../data/mockData';
import type { ShoppingListItem } from '../types';

type Tab = 'all' | 'todo' | 'done';

export function ShoppingList() {
  const [items, setItems] = useState<ShoppingListItem[]>(initialList);
  const [tab, setTab] = useState<Tab>('all');
  const [adding, setAdding] = useState('');
  const [toast, setToast] = useState<string | null>(null);

  const flash = (m: string) => {
    setToast(m);
    setTimeout(() => setToast(null), 2500);
  };

  const counts = {
    all: items.length,
    todo: items.filter((i) => !i.checked).length,
    done: items.filter((i) => i.checked).length,
  };

  const visible = items.filter((i) => (tab === 'todo' ? !i.checked : tab === 'done' ? i.checked : true));
  const grouped = useMemo(() => {
    return visible.reduce<Record<string, ShoppingListItem[]>>((acc, it) => {
      (acc[it.group] ??= []).push(it);
      return acc;
    }, {});
  }, [visible]);

  const toggle = (id: string) => setItems((p) => p.map((i) => (i.id === id ? { ...i, checked: !i.checked } : i)));
  const remove = (id: string) => setItems((p) => p.filter((i) => i.id !== id));
  const clearDone = () => setItems((p) => p.filter((i) => !i.checked));
  const addItem = () => {
    if (!adding.trim()) return;
    setItems((p) => [...p, { id: `n${Date.now()}`, group: 'Nhóm khác', name: adding.trim(), quantity: 'vừa đủ', checked: false }]);
    setAdding('');
  };
  const exportTxt = () => {
    const text = items.map((i) => `${i.checked ? '[x]' : '[ ]'} ${i.name} — ${i.quantity}`).join('\n');
    const blob = new Blob([`DANH SÁCH ĐI CHỢ — MÂM XANH\n\n${text}`], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'danh-sach-di-cho.txt';
    a.click();
    URL.revokeObjectURL(url);
    flash('Đã xuất file .TXT!');
  };
  const copyClip = () => {
    navigator.clipboard?.writeText(items.map((i) => `- ${i.name}: ${i.quantity}`).join('\n'));
    flash('Đã sao chép danh sách vào bộ nhớ đệm!');
  };

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Danh sách đi chợ & Dinh dưỡng</span>
      </nav>

      <div className="mb-6 flex flex-col items-start justify-between gap-4 rounded-2xl border border-brand-100 bg-gradient-to-r from-brand-50 to-white p-5 sm:flex-row sm:items-center">
        <div className="flex items-center gap-4">
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-600 text-white">
            <FileText className="h-6 w-6" />
          </span>
          <div>
            <h1 className="text-xl font-extrabold text-ink sm:text-2xl">Danh sách đi chợ & Kiểm tra vi chất ngày</h1>
            <p className="text-sm text-ink-muted">Tự động tổng hợp nguyên liệu từ kế hoạch tuần của bạn.</p>
          </div>
        </div>
        <div className="flex gap-1 rounded-full bg-white p-1 ring-1 ring-brand-100">
          {([['all', `Tất cả (${counts.all})`], ['todo', `Cần mua (${counts.todo})`], ['done', `Đã xong (${counts.done})`]] as const).map(([t, label]) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`rounded-full px-3.5 py-1.5 text-sm font-semibold transition-colors ${tab === t ? 'bg-brand-600 text-white' : 'text-ink-soft hover:text-brand-700'}`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* main list */}
        <div>
          <div className="mb-4 flex flex-wrap gap-2">
            <div className="flex flex-1 items-center gap-2 rounded-xl border border-brand-200 bg-white px-3">
              <Plus className="h-4 w-4 text-brand-600" />
              <input
                value={adding}
                onChange={(e) => setAdding(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && addItem()}
                placeholder="Thêm thủ công nguyên liệu..."
                className="w-full bg-transparent py-2.5 text-sm outline-none placeholder:text-ink-muted"
              />
              {adding && <Button size="sm" onClick={addItem}>Thêm</Button>}
            </div>
            <Button variant="outline" onClick={copyClip}><ClipboardCopy className="h-4 w-4" /> Sao chép</Button>
            <Button variant="outline" onClick={exportTxt}><Download className="h-4 w-4" /> Xuất file .TXT</Button>
          </div>

          {Object.entries(grouped).length === 0 ? (
            <div className="rounded-2xl border border-dashed border-brand-200 bg-brand-50/50 p-12 text-center text-sm text-ink-muted">
              Không có mục nào trong danh mục này.
            </div>
          ) : (
            <div className="space-y-5">
              {Object.entries(grouped).map(([group, list]) => {
                const bought = list.filter((i) => i.checked).length;
                return (
                  <div key={group} className="rounded-2xl border border-brand-100 bg-white p-5">
                    <div className="mb-3 flex items-center justify-between">
                      <div>
                        <h3 className="font-bold text-ink">{group}</h3>
                        <p className="text-xs text-ink-muted">{list.length} loại · Nguồn nội trợ tại chợ</p>
                      </div>
                      <Badge tone={bought === list.length ? 'leaf' : 'brand'}>{bought}/{list.length} đã mua</Badge>
                    </div>
                    <div className="space-y-1.5">
                      {list.map((it) => (
                        <div key={it.id} className="group flex items-center gap-3 rounded-xl px-2 py-2 transition-colors hover:bg-brand-50/60">
                          <button
                            onClick={() => toggle(it.id)}
                            className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition-colors ${it.checked ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-300 bg-white'}`}
                          >
                            {it.checked && <Check className="h-3.5 w-3.5" />}
                          </button>
                          <div className="min-w-0 flex-1">
                            <p className={`text-sm font-medium ${it.checked ? 'text-ink-muted line-through' : 'text-ink'}`}>{it.name}</p>
                            {it.note && <p className="truncate text-xs text-ink-muted">{it.note}</p>}
                          </div>
                          <Badge tone="neutral">{it.quantity}</Badge>
                          <button onClick={() => remove(it.id)} className="flex h-7 w-7 items-center justify-center rounded-md text-ink-muted opacity-0 transition-all hover:bg-white hover:text-red-500 group-hover:opacity-100">
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {counts.done > 0 && (
            <div className="mt-4 flex justify-end">
              <Button variant="ghost" onClick={clearDone}><Trash2 className="h-4 w-4" /> Xoá {counts.done} mục đã mua</Button>
            </div>
          )}
        </div>

        {/* nutrition sidebar */}
        <aside className="space-y-4">
          <div className="rounded-2xl border border-brand-100 bg-white p-5">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="font-bold text-ink">Bộ chọn ngày kiểm tra</h3>
            </div>
            <div className="flex items-center gap-3 rounded-xl bg-brand-50 p-3">
              <div className="flex h-12 w-12 flex-col items-center justify-center rounded-xl bg-brand-600 text-white">
                <span className="text-[10px] font-semibold uppercase">T3</span>
                <span className="text-lg font-extrabold leading-none">15</span>
              </div>
              <div>
                <p className="font-bold text-ink">Thứ Ba, 15/10/2024</p>
                <p className="text-xs text-ink-muted">3 bữa · kiểm tra vi chất</p>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-brand-100 bg-white p-5">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="font-bold text-ink">Năng lượng mục tiêu</h3>
              <Badge tone="leaf">Đạt chuẩn khuyến nghị</Badge>
            </div>
            <div className="mb-4 flex items-center gap-4">
              <RingProgress pct={92} />
              <div>
                <p className="text-2xl font-extrabold text-ink">1,850 <span className="text-sm font-semibold text-ink-muted">kcal</span></p>
                <p className="text-xs text-ink-muted">/ 2,000 kcal</p>
              </div>
            </div>
            <p className="text-xs leading-relaxed text-ink-muted">
              Năng lượng nạp đạt 92.5% mức tiêu ngày, tỷ lệ đạm – béo – đường được cân bằng lý tưởng.
            </p>
          </div>

          <div className="rounded-2xl border border-brand-100 bg-white p-5">
            <h3 className="mb-4 font-bold text-ink">9 chỉ tiêu dinh dưỡng khoa học</h3>
            <div className="space-y-3">
              {nutritionTargets.map((n) => (
                <div key={n.key}>
                  <div className="mb-1 flex items-center justify-between text-xs">
                    <span className="font-medium text-ink-soft">{n.key}</span>
                    <span className="font-bold text-ink">{n.actual}</span>
                  </div>
                  <ProgressBar pct={n.pct} tone={n.status === 'missing' ? 'amber' : n.status === 'low' ? 'amber' : 'leaf'} />
                </div>
              ))}
            </div>
            <div className="mt-4 rounded-xl bg-brand-50 p-3 text-xs leading-relaxed text-ink-soft">
              <strong className="text-brand-700">Khuyến cáo y khoa theo PRO:</strong> Đối chiếu toàn diện với cơ sở dữ liệu Viện Dinh Dưỡng Việt Nam & USDA FoodData Central.
            </div>
          </div>
        </aside>
      </div>

      {toast && (
        <div className="fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 items-center gap-2 rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white shadow-xl">
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-leaf-500"><Check className="h-3.5 w-3.5" /></span>
          {toast}
          <button onClick={() => setToast(null)} className="ml-1 text-white/70 hover:text-white"><X className="h-4 w-4" /></button>
        </div>
      )}
    </PageContainer>
  );
}

function RingProgress({ pct }: { pct: number }) {
  const r = 30;
  const c = 2 * Math.PI * r;
  return (
    <svg viewBox="0 0 72 72" className="h-[72px] w-[72px] -rotate-90" width={72} height={72}>
      <circle cx={36} cy={36} r={r} fill="none" stroke="#ffedd5" strokeWidth={7} />
      <circle
        cx={36} cy={36} r={r} fill="none" stroke="#ea580c" strokeWidth={7} strokeLinecap="round"
        strokeDasharray={c} strokeDashoffset={c - (c * pct) / 100}
      />
    </svg>
  );
}
