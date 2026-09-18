import { Link } from 'react-router-dom';
import { Logo } from './Logo';

const columns = [
  {
    title: 'Khám phá',
    links: [
      { label: 'Công thức thuần chay', to: '/kham-pha' },
      { label: 'Thực đơn 7 ngày thuần chay', to: '/ke-hoach' },
      { label: 'Tính toán đạm thực vật', to: '/dinh-duong' },
      { label: 'Cẩm nang phối ngũ vị thực vật', to: '/cong-dong' },
    ],
  },
  {
    title: 'Công cụ thông minh',
    links: [
      { label: 'Gemini AI gợi ý món chay', to: '/kham-pha' },
      { label: 'Thực đơn theo mùa', to: '/ke-hoach' },
      { label: 'Lập lịch đi chợ', to: '/di-cho' },
      { label: 'Giỏ đi chợ Phú', to: '/di-cho' },
    ],
  },
  {
    title: 'Chính sách & Hỗ trợ',
    links: [
      { label: 'Nguyên tắc cộng đồng', to: '/cong-dong' },
      { label: 'Bảo mật & Quyền riêng tư', to: '/ho-so' },
      { label: 'Liên hệ ban cố vấn dinh dưỡng', to: '/ho-so' },
      { label: 'Góp ý công thức mới', to: '/dang-cong-thuc' },
    ],
  },
];

export function AppFooter() {
  return (
    <footer className="mt-16 border-t border-brand-100 bg-white/60">
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid gap-10 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
          <div className="max-w-xs">
            <Logo />
            <p className="mt-4 text-sm leading-relaxed text-ink-muted">
              Hệ sinh thái thông minh đồng hành cùng lối sống ẩm thực thực vật bền vững, nuôi
              dưỡng thân tâm và cân bằng vi chất khoa học cho gia đình Việt.
            </p>
          </div>
          {columns.map((col) => (
            <div key={col.title}>
              <h4 className="mb-4 text-xs font-bold uppercase tracking-wider text-ink">{col.title}</h4>
              <ul className="space-y-2.5">
                {col.links.map((l) => (
                  <li key={l.label}>
                    <Link to={l.to} className="text-sm text-ink-muted transition-colors hover:text-brand-600">
                      {l.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="mt-10 flex flex-col items-center justify-between gap-3 border-t border-brand-100 pt-6 text-sm text-ink-muted sm:flex-row">
          <p>© 2025 Mâm Xanh System. Nuôi dưỡng lối sống xanh lành.</p>
          <p>
            <span className="font-semibold text-brand-600">Phiên bản 2.4.0-VN</span> · Hỗ trợ ẩm thực
            thực vật toàn diện
          </p>
        </div>
      </div>
    </footer>
  );
}
