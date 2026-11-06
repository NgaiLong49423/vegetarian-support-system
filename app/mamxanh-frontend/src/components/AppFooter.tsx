import { Link } from "react-router-dom"

export function AppFooter() {
  return (
    <footer className="border-t border-footer-border bg-footer transition-colors">
      <div className="mx-auto flex max-w-[1344px] flex-col gap-3 px-4 py-6 text-xs text-ink-soft sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:py-8 lg:px-8">
        <p>© {new Date().getFullYear()} Mâm Xanh · Ẩm thực thuần lành</p>
        <nav
          aria-label="Liên kết cuối trang"
          className="flex flex-wrap gap-x-5 gap-y-2 font-medium sm:gap-6"
        >
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
