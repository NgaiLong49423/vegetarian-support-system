import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Activity } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';

export function NutritionProfile() {
  const [age, setAge] = useState('');
  const [height, setHeight] = useState('');
  const [weight, setWeight] = useState('');
  const [eligible, setEligible] = useState(false);
  const [bmi, setBmi] = useState<number | null>(null);
  const [error, setError] = useState('');

  const calculate = (event: FormEvent) => {
    event.preventDefault();
    const ageValue = Number(age);
    const heightValue = Number(height);
    const weightValue = Number(weight);
    if (!eligible) { setBmi(null); setError('Vui lòng xác nhận phạm vi hỗ trợ trước khi tính.'); return; }
    if (!Number.isInteger(ageValue) || ageValue < 18 || ageValue > 120 || heightValue < 100 || heightValue > 250 || weightValue < 30 || weightValue > 300) {
      setBmi(null); setError('Tuổi: 18–120; chiều cao: 100–250 cm; cân nặng: 30–300 kg.'); return;
    }
    setBmi(Math.round(weightValue / (heightValue / 100) ** 2 * 10) / 10);
    setError('');
  };
  const category = bmi === null ? '' : bmi < 18.5 ? 'Thiếu cân' : bmi < 25 ? 'Trong khoảng tham khảo' : bmi < 30 ? 'Thừa cân' : 'Béo phì';

  return <PageContainer className="py-8">
    <nav className="mb-5 text-sm text-ink-muted"><Link to="/ho-so" className="hover:text-brand-600">Hồ sơ</Link> / Hồ sơ dinh dưỡng</nav>
    <h1 className="flex items-center gap-2 text-3xl font-extrabold text-ink"><Activity className="h-7 w-7 text-brand-600" /> Hồ sơ dinh dưỡng</h1>
    <p className="mt-2 max-w-2xl text-sm text-ink-muted">Nhập thông tin để xem BMI tham khảo. Dữ liệu chỉ được xử lý trong trình duyệt và không được lưu trong bản demo.</p>
    <div className="mt-6 grid gap-6 lg:grid-cols-[1.2fr_1fr]">
      <Card className="p-6">
        <h2 className="text-lg font-bold text-ink">Thông số cơ bản</h2>
        <form onSubmit={calculate} className="mt-5 space-y-4">
          <div className="grid gap-4 sm:grid-cols-3">
            {[
              { label: 'Tuổi', value: age, set: setAge, min: 18, max: 120, unit: '' },
              { label: 'Chiều cao', value: height, set: setHeight, min: 100, max: 250, unit: 'cm' },
              { label: 'Cân nặng', value: weight, set: setWeight, min: 30, max: 300, unit: 'kg' },
            ].map((field) => <label key={field.label} className="text-sm font-semibold text-ink">{field.label}
              <div className="mt-2 flex items-center rounded-xl border border-brand-200 bg-white pr-3">
                <input type="number" min={field.min} max={field.max} step={field.label === 'Tuổi' ? 1 : 0.1} required value={field.value}
                  onChange={(event) => { field.set(event.target.value); setBmi(null); }}
                  className="min-w-0 w-full rounded-xl p-3 text-sm outline-none" />
                <span className="text-xs text-ink-muted">{field.unit}</span>
              </div>
            </label>)}
          </div>
          <label className="flex items-start gap-3 text-sm text-ink-soft">
            <input type="checkbox" checked={eligible} onChange={(event) => { setEligible(event.target.checked); setBmi(null); }} className="mt-1" />
            <span>Tôi từ 18 tuổi, không mang thai hoặc cho con bú và không cần chế độ ăn điều trị bệnh lý đặc biệt.</span>
          </label>
          {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
          <Button type="submit">Tính BMI tham khảo</Button>
        </form>
      </Card>
      <Card className="p-6">
        <h2 className="text-lg font-bold text-ink">Kết quả tham khảo</h2>
        {bmi === null ? <p className="mt-5 text-sm text-ink-muted">Nhập thông số và xác nhận phạm vi hỗ trợ để xem kết quả.</p> : <div className="mt-5">
          <p className="text-5xl font-extrabold text-brand-700">{bmi.toFixed(1)}</p>
          <p className="mt-2 font-semibold text-ink">{category}</p>
          <p className="mt-3 text-sm text-ink-muted">BMI = cân nặng (kg) / chiều cao (m)².</p>
        </div>}
        <p className="mt-6 rounded-xl bg-amber-50 p-4 text-sm leading-relaxed text-ink-soft">
          BMI chỉ là chỉ số sàng lọc tham khảo, không phải chẩn đoán. Kết quả này không tự xác định nhu cầu calorie, dưỡng chất hoặc món ăn phù hợp. Hãy tham vấn chuyên gia y tế khi cần tư vấn cá nhân.
        </p>
        <a href="https://www.who.int/europe/news-room/fact-sheets/item/nutrition---maintaining-a-healthy-lifestyle" target="_blank" rel="noreferrer" className="mt-3 inline-block text-xs font-semibold text-brand-600">Nguồn phân loại BMI: WHO</a>
      </Card>
    </div>
  </PageContainer>;
}
