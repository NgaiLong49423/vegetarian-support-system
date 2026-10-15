import { useEffect, useRef, useState } from "react"
import { Link } from "react-router-dom"
import {
  ArrowDown,
  ArrowRight,
  Bookmark,
  CalendarDays,
  ChefHat,
  Leaf,
  Menu,
  MessageCircle,
  Search,
  ShoppingBasket,
  X,
} from "lucide-react"
import { useAuth } from "../components/AuthContext"
import { Logo } from "../components/Logo"
import "./LandingPage.css"

const sections = [
  { id: "cong-thuc", label: "Công thức" },
  { id: "lich-an", label: "Lịch ăn" },
  { id: "di-cho", label: "Đi chợ" },
  { id: "dinh-duong", label: "Dinh dưỡng" },
  { id: "tro-ly-ai", label: "AI" },
]

function LandingActions({ final = false }: { final?: boolean }) {
  const { isAuthenticated } = useAuth()
  return (
    <div className="landing-actions">
      <Link
        className={`landing-button${final ? " landing-button-light" : ""}`}
        to="/trang-chu"
      >
        {isAuthenticated ? "Vào ứng dụng" : "Bắt đầu khám phá"}
        <ArrowRight size={18} aria-hidden="true" />
      </Link>
      <Link className="landing-text-link" to="/kham-pha">
        Khám phá công thức
      </Link>
    </div>
  )
}

/** Screenshots are captured from the existing application; no live account data is loaded here. */
const previewSizes = {
  home: [1280, 820],
  explore: [1280, 820],
  saved: [1280, 662],
  planner: [1280, 820],
  shopping: [1280, 988],
  nutrition: [477, 609],
  ai: [672, 586],
} as const

function AppPreview({
  image,
  title,
  eager = false,
}: {
  image: keyof typeof previewSizes
  title: string
  eager?: boolean
}) {
  const [width, height] = previewSizes[image]
  return (
    <figure className="landing-preview landing-app-preview">
      <picture>
        {image === "home" && (
          <source
            media="(max-width: 640px)"
            srcSet="/landing/home-mobile.jpg"
            width={390}
            height={780}
          />
        )}
        <img
          src={`/landing/${image}.jpg`}
          alt={`Giao diện Mâm Xanh: ${title}`}
          width={width}
          height={height}
          loading={eager ? "eager" : "lazy"}
          decoding="async"
        />
      </picture>
      <figcaption>{title} · Mâm Xanh</figcaption>
    </figure>
  )
}
export function LandingPage() {
  const { isAuthenticated, account } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuButton = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false)
        menuButton.current?.focus()
      }
    }
    document.addEventListener("keydown", closeOnEscape)
    return () => document.removeEventListener("keydown", closeOnEscape)
  }, [menuOpen])

  const savedDestination = !isAuthenticated
    ? "/dang-ky"
    : account?.role === "ADMIN"
      ? "/trang-chu"
      : "/ho-so"
  const savedLabel = !isAuthenticated
    ? "Tạo bộ sưu tập của bạn"
    : account?.role === "ADMIN"
      ? "Vào ứng dụng"
      : "Xem công thức đã lưu"

  return (
    <div className="landing-page">
      <title>Mâm Xanh — Ăn chay theo cách của bạn</title>
      <a href="#landing-main" className="landing-skip-link">
        Đi tới nội dung chính
      </a>
      <header className="landing-header">
        <div className="landing-container landing-header-inner">
          <Logo />
          <nav className="landing-desktop-nav" aria-label="Giới thiệu sản phẩm">
            {sections.map((section) => (
              <a key={section.id} href={`#${section.id}`}>
                {section.label}
              </a>
            ))}
          </nav>
          <div className="landing-header-actions">
            {!isAuthenticated && (
              <Link className="landing-login" to="/dang-nhap">
                Đăng nhập
              </Link>
            )}
            <Link
              className="landing-button landing-button-small"
              to="/trang-chu"
            >
              {isAuthenticated ? "Vào ứng dụng" : "Bắt đầu"}
            </Link>
            <button
              ref={menuButton}
              className="landing-menu-button"
              type="button"
              aria-label={
                menuOpen ? "Đóng menu giới thiệu" : "Mở menu giới thiệu"
              }
              aria-expanded={menuOpen}
              aria-controls="landing-mobile-nav"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              {menuOpen ? <X size={22} /> : <Menu size={22} />}
            </button>
          </div>
        </div>
        <nav
          id="landing-mobile-nav"
          className="landing-mobile-nav"
          aria-label="Giới thiệu sản phẩm trên điện thoại"
          hidden={!menuOpen}
        >
          {sections.map((section) => (
            <a
              key={section.id}
              href={`#${section.id}`}
              onClick={() => setMenuOpen(false)}
            >
              {section.label}
            </a>
          ))}
          {!isAuthenticated && (
            <Link to="/dang-nhap" onClick={() => setMenuOpen(false)}>
              Đăng nhập
            </Link>
          )}
        </nav>
      </header>
      <main id="landing-main" tabIndex={-1}>
        <section
          className="landing-hero landing-container"
          aria-labelledby="landing-title"
        >
          <p className="landing-eyebrow">
            <Leaf size={15} aria-hidden="true" /> Một căn bếp. Nhiều cảm hứng.
          </p>
          <h1 id="landing-title">
            Ăn chay theo
            <br />
            cách của <span>bạn.</span>
          </h1>
          <p className="landing-hero-copy">
            Từ món ăn bạn muốn thử đến những bữa chay mỗi ngày. Khám phá công
            thức, giữ lại món yêu thích và tìm cảm hứng cho căn bếp của bạn.
          </p>
          <LandingActions />
          <div className="landing-hero-preview">
            <div className="landing-hero-note">
              <span>Công thức</span>
              <span>Lịch ăn</span>
              <span>Cảm hứng mỗi ngày</span>
            </div>
            <AppPreview image="home" title="Trang chủ Guest" eager />
          </div>
          <a className="landing-scroll-hint" href="#cong-thuc">
            Cùng khám phá Mâm Xanh
            <ArrowDown size={16} aria-hidden="true" />
          </a>
        </section>
        <section
          id="cong-thuc"
          className="landing-section landing-container"
          aria-labelledby="discovery-title"
        >
          <div className="landing-section-heading">
            <p className="landing-eyebrow">BẮT ĐẦU TỪ MỘT MÓN ĂN</p>
            <h2 id="discovery-title">
              Món hợp với bạn,
              <br />
              ngay trong tầm tay.
            </h2>
            <p>
              Tìm công thức theo loại ăn chay, nguyên liệu bạn có và thời gian
              dành cho việc nấu. Đọc cách làm, xem định lượng rồi chọn món muốn
              thử.
            </p>
            <Link className="landing-text-link" to="/kham-pha">
              Tìm món cho hôm nay
              <ArrowRight size={17} aria-hidden="true" />
            </Link>
          </div>
          <div className="landing-discovery-screens">
            <AppPreview image="explore" title="Khám phá công thức" />
            <AppPreview image="saved" title="Công thức đã lưu" />
          </div>
          <div className="landing-discovery-details">
            <div>
              <Search size={24} aria-hidden="true" />
              <h3>Tìm theo nhu cầu</h3>
              <p>
                Từ một nguyên liệu quen thuộc đến một món ăn mới, bắt đầu với
                điều bạn đang tìm.
              </p>
            </div>
            <div>
              <ChefHat size={24} aria-hidden="true" />
              <h3>Biết trước khi nấu</h3>
              <p>
                Nguyên liệu, định lượng, khẩu phần và hướng dẫn ở cùng một nơi.
              </p>
            </div>
            <div>
              <Bookmark size={24} aria-hidden="true" />
              <h3>Giữ món mình thích</h3>
              <p>Lưu công thức vào tài khoản để dễ tìm lại khi muốn vào bếp.</p>
              <Link className="landing-text-link" to={savedDestination}>
                {savedLabel}
                <ArrowRight size={16} aria-hidden="true" />
              </Link>
            </div>
          </div>
        </section>
        <section
          id="lich-an"
          className="landing-section landing-section-tinted"
          aria-labelledby="planner-title"
        >
          <div className="landing-container landing-split">
            <div className="landing-section-heading">
              <p className="landing-eyebrow">BỮA ĂN CÓ KẾ HOẠCH</p>
              <h2 id="planner-title">
                Dành chỗ cho
                <br />
                những bữa ăn bạn thích.
              </h2>
              <p>
                Tập hợp công thức vào lịch ăn ngày và tuần. Từ bữa sáng nhẹ
                nhàng đến bữa tối đủ đầy, mỗi lựa chọn đều có chỗ trong kế hoạch
                của bạn.
              </p>
              <div className="landing-feature-line">
                <CalendarDays size={18} aria-hidden="true" />
                <span>Lịch ăn theo ngày · Ba bữa mỗi ngày</span>
              </div>
            </div>
            <AppPreview image="planner" title="Lịch ăn tuần" />
          </div>
        </section>
        <section
          id="di-cho"
          className="landing-section landing-container landing-split landing-split-reverse"
          aria-labelledby="shopping-title"
        >
          <AppPreview image="shopping" title="Danh sách đi chợ" />
          <div className="landing-section-heading">
            <p className="landing-eyebrow">TỪ CĂN BẾP ĐẾN GIỎ HÀNG</p>
            <h2 id="shopping-title">
              Đi chợ với
              <br />
              một danh sách rõ ràng.
            </h2>
            <p>
              Chuẩn bị nguyên liệu từ công thức hoặc lịch ăn. Tổng hợp những thứ
              cần mua, thêm mặt hàng của riêng bạn và đánh dấu khi đã cho vào
              giỏ.
            </p>
            <div className="landing-feature-line">
              <ShoppingBasket size={18} aria-hidden="true" />
              <span>Gom nguyên liệu · Quản lý checklist</span>
            </div>
          </div>
        </section>
        <section
          id="dinh-duong"
          className="landing-section landing-section-tinted"
          aria-labelledby="nutrition-title"
        >
          <div className="landing-container landing-split">
            <div className="landing-section-heading">
              <p className="landing-eyebrow">HIỂU THÊM VỀ BỮA CHAY</p>
              <h2 id="nutrition-title">
                Thêm thông tin.
                <br />
                Thêm cơ sở để lựa chọn.
              </h2>
              <p>
                Tham khảo dinh dưỡng của công thức theo khẩu phần, khai báo hồ
                sơ cá nhân và nhìn lại thực đơn ngày, tuần theo nhu cầu của bạn.
              </p>
              <p className="landing-fine-print">
                Thông tin dinh dưỡng mang tính tham khảo, có thể chưa đầy đủ và
                không thay thế tư vấn của chuyên gia y tế.
              </p>
            </div>
            <AppPreview image="nutrition" title="Dinh dưỡng công thức" />
          </div>
        </section>
        <section
          id="tro-ly-ai"
          className="landing-section landing-container landing-split landing-split-reverse"
          aria-labelledby="ai-title"
        >
          <AppPreview image="ai" title="Hỗ trợ soạn công thức" />
          <div className="landing-section-heading">
            <p className="landing-eyebrow">KHI BẠN CẦN MỘT Ý TƯỞNG</p>
            <h2 id="ai-title">
              Một người bạn
              <br />
              trong hành trình ăn chay.
            </h2>
            <p>
              Hỏi về công thức, tìm gợi ý món từ nội dung công khai và nhận hỗ
              trợ lên thực đơn. Với Chuyên gia, AI còn hỗ trợ viết phần giới
              thiệu và hướng dẫn để bạn xem lại, chỉnh sửa theo cách của mình.
            </p>
            <p className="landing-fine-print">
              Các hình thức hỗ trợ AI được phân quyền theo gói tài khoản. Bạn
              luôn chủ động lựa chọn và quyết định nội dung sử dụng.
            </p>
          </div>
        </section>
        <section
          className="landing-community landing-section"
          aria-labelledby="community-title"
        >
          <div className="landing-container">
            <p className="landing-eyebrow">CẢM HỨNG ĐẾN TỪ NGƯỜI CHIA SẺ</p>
            <h2 id="community-title">
              Một công thức ngon.
              <br />
              Nhiều câu chuyện để trao đổi.
            </h2>
            <p>
              Khám phá công thức từ Chuyên gia ẩm thực chay, chia sẻ trải nghiệm
              qua bình luận và theo dõi người có cùng cảm hứng nấu ăn.
            </p>
            <div className="landing-community-icons" aria-hidden="true">
              <ChefHat />
              <Leaf />
              <MessageCircle />
            </div>
            <Link className="landing-text-link" to="/kham-pha">
              Khám phá những công thức được chia sẻ
              <ArrowRight size={17} aria-hidden="true" />
            </Link>
          </div>
        </section>
        <section className="landing-final-cta" aria-labelledby="final-title">
          <div className="landing-container">
            <p className="landing-eyebrow">
              CĂN BẾP CỦA BẠN, HÀNH TRÌNH CỦA BẠN
            </p>
            <h2 id="final-title">
              Bắt đầu từ món chay
              <br />
              tiếp theo của bạn.
            </h2>
            <p>
              Một món mới để thử. Một công thức để giữ lại. Một chút cảm hứng
              cho bữa ăn hôm nay.
            </p>
            <LandingActions final />
          </div>
        </section>
      </main>
      <footer className="landing-footer landing-container">
        <div>
          <Logo />
          <p>Cảm hứng cho những bữa chay mỗi ngày.</p>
        </div>
        <nav aria-label="Liên kết cuối trang giới thiệu">
          {sections.map((section) => (
            <a key={section.id} href={`#${section.id}`}>
              {section.label}
            </a>
          ))}
          <Link to="/trang-chu">Vào ứng dụng</Link>
        </nav>
        <p className="landing-copyright">
          © {new Date().getFullYear()} Mâm Xanh
        </p>
      </footer>
    </div>
  )
}
