import { Link } from "react-router-dom"

export function AppFooter() {
  return (
    <footer className="border-t border-brand-100 bg-cream">
      <div className="mx-auto flex max-w-[1160px] flex-col gap-3 px-4 py-6 text-xs text-ink-muted sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <p>© {new Date().getFullYear()} Mâm Xanh · Ẩm thực thuần lành</p>
        <nav aria-label="Liên kết cuối trang" className="flex flex-wrap gap-5">
          <Link to="/kham-pha" className="hover:text-brand-600">
            Công thức
          </Link>
          <Link to="/cong-dong" className="hover:text-brand-600">
            Cộng đồng
          </Link>
          <Link to="/ho-so/dinh-duong" className="hover:text-brand-600">
            Hồ sơ dinh dưỡng
          </Link>
        </nav>
      </div>
    </footer>
  )
}
