import { useEffect, useRef } from "react"
import { Link } from "react-router-dom"
import {
  ArrowDown,
  ArrowRight,
  Bookmark,
  CalendarDays,
  Check,
  ChefHat,
  Clock3,
  Leaf,
  MessageCircle,
  Search,
  ShoppingBasket,
  Sparkles,
} from "lucide-react"
import { useAuth } from "../components/AuthContext"
import { Logo } from "../components/Logo"
import "./LandingPage.css"

const sections = [
  { id: "cong-thuc", label: "Công thức" },
  { id: "lich-an", label: "Lịch ăn" },
  { id: "di-cho", label: "Danh sách mua sắm" },
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

export function LandingPage() {
  const { isAuthenticated, account } = useAuth()
  const pageRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const root = pageRef.current
    if (!root) return

    const revealNodes = Array.from(
      root.querySelectorAll<HTMLElement>("[data-reveal]"),
    )
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches

    if (prefersReducedMotion || typeof IntersectionObserver === "undefined") {
      revealNodes.forEach((node) => node.classList.add("is-visible"))
      return
    }

    root.classList.add("has-scroll-motion")

    const revealObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const target = entry.target as HTMLElement
          if (entry.isIntersecting) {
            target.classList.add("is-visible")
          } else if (entry.boundingClientRect.top > 40) {
            target.classList.remove("is-visible")
          }
        }
      },
      {
        threshold: 0.12,
        rootMargin: "0px 0px -36px 0px",
      },
    )

    revealNodes.forEach((node) => {
      const rect = node.getBoundingClientRect()
      if (rect.top < window.innerHeight * 0.92) {
        node.classList.add("is-visible")
      }
      revealObserver.observe(node)
    })

    return () => {
      revealObserver.disconnect()
    }
  }, [])

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
    <div ref={pageRef} className="landing-page">
      <title>Mâm Xanh — Ăn chay theo cách của bạn</title>
      <a href="#landing-main" className="landing-skip-link">
        Đi tới nội dung chính
      </a>
      <header className="landing-header">
        <div className="landing-container landing-header-inner">
          <Logo />
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
          </div>
        </div>
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

          <div className="landing-hero-preview" data-reveal="up">
            <div className="landing-hero-note">
              <span>Công thức</span>
              <span>Lịch ăn</span>
              <span>Cảm hứng mỗi ngày</span>
            </div>
            <figure className="landing-preview" aria-hidden="true">
              <div className="landing-preview-canvas landing-preview-hero-grid">
                <div className="landing-preview-hero-main">
                  <div className="landing-preview-photo-wrap">
                    <img
                      src="/assets/95427.png"
                      alt=""
                      loading="eager"
                      decoding="async"
                    />
                    <span className="landing-preview-badge">Thuần chay</span>
                  </div>
                  <div className="landing-preview-hero-body">
                    <span className="landing-preview-kicker">
                      Món gợi ý hôm nay · 25 phút
                    </span>
                    <h3>Đậu Hũ Kho Nấm Đùi Gà Thố Đất</h3>
                    <p>
                      Đậm đà vị tiêu xanh, nấm giòn ngọt tự nhiên cùng nước sốt
                      sánh mịn dễ làm tại nhà.
                    </p>
                    <div className="landing-preview-chips">
                      <span>310 kcal / khẩu phần</span>
                      <span>16g đạm thực vật</span>
                      <span>2 khẩu phần</span>
                    </div>
                  </div>
                </div>
                <div className="landing-preview-hero-side">
                  <div className="landing-preview-mini-card">
                    <img
                      src="/assets/cddd4.png"
                      alt=""
                      loading="eager"
                      decoding="async"
                    />
                    <div>
                      <span>Bữa trưa cân bằng</span>
                      <strong>Tô Hạt Diêm Mạch & Bơ Đậu Gà</strong>
                      <small>20 phút · 420 kcal</small>
                    </div>
                  </div>
                  <div className="landing-preview-mini-card">
                    <img
                      src="/assets/40053.png"
                      alt=""
                      loading="eager"
                      decoding="async"
                    />
                    <div>
                      <span>Canh thanh mát</span>
                      <strong>Canh Hạt Sen Nấm Đông Cô</strong>
                      <small>25 phút · Dễ nấu</small>
                    </div>
                  </div>
                  <div className="landing-preview-mini-card">
                    <img
                      src="/assets/8ef48.png"
                      alt=""
                      loading="eager"
                      decoding="async"
                    />
                    <div>
                      <span>Bữa sáng nhanh gọn</span>
                      <strong>Bánh Mì Nguyên Cám Đậu Hũ Nghệ</strong>
                      <small>15 phút · 340 kcal</small>
                    </div>
                  </div>
                </div>
              </div>
              <figcaption>Trang chủ Guest · Mâm Xanh</figcaption>
            </figure>
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
          <div className="landing-section-heading" data-reveal="up">
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
            <figure
              className="landing-preview"
              data-reveal="left"
              aria-hidden="true"
            >
              <div className="landing-preview-canvas">
                <div className="landing-preview-bar">
                  <span className="landing-preview-search">
                    <Search size={14} /> Tìm theo nguyên liệu: đậu hũ, nấm, hạt
                    sen...
                  </span>
                  <span className="landing-preview-pill">Thuần chay</span>
                </div>
                <div className="landing-preview-card-pair">
                  <div className="landing-preview-dish">
                    <img src="/assets/09723.png" alt="" loading="lazy" />
                    <div className="landing-preview-dish-info">
                      <span>Món nước · 30 phút</span>
                      <strong>Bún Riêu Chay Nấm Đông Cô Đậu Hũ</strong>
                      <small>Thanh ngọt từ rau củ · 380 kcal</small>
                    </div>
                  </div>
                  <div className="landing-preview-dish">
                    <img src="/assets/5a53c.png" alt="" loading="lazy" />
                    <div className="landing-preview-dish-info">
                      <span>Món trộn · 15 phút</span>
                      <strong>Gỏi Cuốn Ngũ Sắc Sốt Bơ Đậu Phộng</strong>
                      <small>Giàu chất xơ · 290 kcal</small>
                    </div>
                  </div>
                </div>
              </div>
              <figcaption>Khám phá công thức · Mâm Xanh</figcaption>
            </figure>

            <figure
              className="landing-preview"
              data-reveal="right"
              aria-hidden="true"
            >
              <div className="landing-preview-canvas">
                <div className="landing-preview-bar">
                  <span className="landing-preview-title-sm">
                    <Bookmark size={14} /> Bộ sưu tập món yêu thích
                  </span>
                  <span className="landing-preview-pill">Đã lưu 12 món</span>
                </div>
                <div className="landing-preview-saved-list">
                  <div className="landing-preview-saved-item">
                    <img src="/assets/40c2c.png" alt="" loading="lazy" />
                    <div>
                      <strong>Tàu Hũ Hạt Sen Nước Đường Gừng</strong>
                      <small>Tráng miệng · 25 phút · 2 khẩu phần</small>
                    </div>
                    <span className="landing-preview-saved-tag">Đã lưu</span>
                  </div>
                  <div className="landing-preview-saved-item">
                    <img src="/assets/f2961.png" alt="" loading="lazy" />
                    <div>
                      <strong>Cơm Chiên Lá É Hạt Sen & Đậu Hà Lan</strong>
                      <small>Món chính · 20 phút · 410 kcal</small>
                    </div>
                    <span className="landing-preview-saved-tag">Đã lưu</span>
                  </div>
                  <div className="landing-preview-saved-item">
                    <img src="/assets/95427.png" alt="" loading="lazy" />
                    <div>
                      <strong>Đậu Hũ Kho Nấm Đùi Gà Thố Đất</strong>
                      <small>Món kho · 25 phút · 310 kcal</small>
                    </div>
                    <span className="landing-preview-saved-tag">Đã lưu</span>
                  </div>
                </div>
              </div>
              <figcaption>Công thức đã lưu · Mâm Xanh</figcaption>
            </figure>
          </div>

          <div className="landing-discovery-details">
            <div data-reveal="up">
              <Search size={24} aria-hidden="true" />
              <h3>Tìm theo nhu cầu</h3>
              <p>
                Từ một nguyên liệu quen thuộc đến một món ăn mới, bắt đầu với
                điều bạn đang tìm.
              </p>
            </div>
            <div
              data-reveal="up"
              style={{ "--reveal-delay": "90ms" } as React.CSSProperties}
            >
              <ChefHat size={24} aria-hidden="true" />
              <h3>Biết trước khi nấu</h3>
              <p>
                Nguyên liệu, định lượng, khẩu phần và hướng dẫn ở cùng một nơi.
              </p>
            </div>
            <div
              data-reveal="up"
              style={{ "--reveal-delay": "180ms" } as React.CSSProperties}
            >
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
            <div className="landing-section-heading" data-reveal="left">
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

            <figure
              className="landing-preview"
              data-reveal="right"
              aria-hidden="true"
            >
              <div className="landing-preview-canvas">
                <div className="landing-preview-bar">
                  <span className="landing-preview-title-sm">
                    <CalendarDays size={14} /> Kế hoạch thực đơn tuần
                  </span>
                  <span className="landing-preview-pill">1.280 kcal / ngày</span>
                </div>
                <div className="landing-preview-saved-list">
                  <div className="landing-preview-saved-item">
                    <img src="/assets/8ef48.png" alt="" loading="lazy" />
                    <div>
                      <span className="landing-preview-meal-slot">
                        Bữa sáng · 15 phút
                      </span>
                      <strong>Bánh Mì Nguyên Cám Đậu Hũ Nghệ</strong>
                      <small>2 khẩu phần · 340 kcal</small>
                    </div>
                  </div>
                  <div className="landing-preview-saved-item">
                    <img src="/assets/cddd4.png" alt="" loading="lazy" />
                    <div>
                      <span className="landing-preview-meal-slot">
                        Bữa trưa · 20 phút
                      </span>
                      <strong>Tô Hạt Diêm Mạch & Bơ Đậu Gà</strong>
                      <small>2 khẩu phần · 460 kcal</small>
                    </div>
                  </div>
                  <div className="landing-preview-saved-item">
                    <img src="/assets/95427.png" alt="" loading="lazy" />
                    <div>
                      <span className="landing-preview-meal-slot">
                        Bữa tối · 30 phút
                      </span>
                      <strong>Đậu Hũ Kho Nấm Đùi Gà & Canh Sen</strong>
                      <small>3 khẩu phần · 480 kcal</small>
                    </div>
                  </div>
                </div>
              </div>
              <figcaption>Lịch ăn tuần · Mâm Xanh</figcaption>
            </figure>
          </div>
        </section>

        <section
          id="di-cho"
          className="landing-section landing-container landing-split landing-split-reverse"
          aria-labelledby="shopping-title"
        >
          <figure
            className="landing-preview"
            data-reveal="left"
            aria-hidden="true"
          >
            <div className="landing-preview-canvas">
              <div className="landing-preview-bar">
                <span className="landing-preview-title-sm">
                  <ShoppingBasket size={14} /> Danh sách nguyên liệu cần mua
                </span>
                <span className="landing-preview-pill">Đã mua 4/5 mục</span>
              </div>
              <div className="landing-preview-checklist">
                {[
                  {
                    name: "Đậu hũ non hữu cơ",
                    group: "Đậu & Chế phẩm",
                    qty: "400 g",
                    done: true,
                  },
                  {
                    name: "Nấm đùi gà tươi",
                    group: "Rau củ & Nấm",
                    qty: "250 g",
                    done: true,
                  },
                  {
                    name: "Hạt sen tươi Huế",
                    group: "Các loại hạt",
                    qty: "150 g",
                    done: true,
                  },
                  {
                    name: "Quả bơ sáp chín",
                    group: "Trái cây tươi",
                    qty: "2 quả",
                    done: true,
                  },
                  {
                    name: "Rau cải bó xôi non",
                    group: "Rau củ & Nấm",
                    qty: "300 g",
                    done: false,
                  },
                ].map((item) => (
                  <div
                    key={item.name}
                    className={`landing-preview-check-row${item.done ? " is-done" : ""}`}
                  >
                    <span className="landing-preview-check-box">
                      {item.done && <Check size={12} />}
                    </span>
                    <div className="landing-preview-check-text">
                      <strong>{item.name}</strong>
                      <small>{item.group}</small>
                    </div>
                    <span className="landing-preview-check-qty">{item.qty}</span>
                  </div>
                ))}
              </div>
            </div>
            <figcaption>Danh sách mua sắm · Mâm Xanh</figcaption>
          </figure>

          <div className="landing-section-heading" data-reveal="right">
            <p className="landing-eyebrow">TỪ CĂN BẾP ĐẾN GIỎ HÀNG</p>
            <h2 id="shopping-title">
              Mua sắm nguyên liệu cùng
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
            <div className="landing-section-heading" data-reveal="left">
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

            <figure
              className="landing-preview"
              data-reveal="right"
              aria-hidden="true"
            >
              <div className="landing-preview-canvas">
                <div className="landing-preview-bar">
                  <span className="landing-preview-title-sm">
                    <Leaf size={14} /> Dinh dưỡng mỗi khẩu phần
                  </span>
                  <span className="landing-preview-pill">9 chỉ tiêu</span>
                </div>
                <div className="landing-preview-kcal-banner">
                  <div>
                    <span>Năng lượng ước tính</span>
                    <strong>≈ 420 kcal / khẩu phần</strong>
                  </div>
                  <span className="landing-preview-kcal-sub">
                    Chuẩn USDA / NIH
                  </span>
                </div>
                <div className="landing-preview-nutri-grid">
                  <div className="landing-preview-nutri-cell">
                    <span>Chất đạm</span>
                    <strong>18.4 g</strong>
                  </div>
                  <div className="landing-preview-nutri-cell">
                    <span>Tinh bột</span>
                    <strong>46.0 g</strong>
                  </div>
                  <div className="landing-preview-nutri-cell">
                    <span>Chất xơ</span>
                    <strong>9.5 g</strong>
                  </div>
                  <div className="landing-preview-nutri-cell">
                    <span>Chất béo</span>
                    <strong>14.2 g</strong>
                  </div>
                </div>
                <div className="landing-preview-micro-row">
                  <span>Canxi ≈ 160 mg</span>
                  <span>Sắt ≈ 4.8 mg</span>
                  <span>Kẽm ≈ 3.1 mg</span>
                  <span>Vitamin B12 ≈ 0.8 µg</span>
                </div>
              </div>
              <figcaption>Dinh dưỡng công thức · Mâm Xanh</figcaption>
            </figure>
          </div>
        </section>

        <section
          id="tro-ly-ai"
          className="landing-section landing-container landing-split landing-split-reverse"
          aria-labelledby="ai-title"
        >
          <figure
            className="landing-preview"
            data-reveal="left"
            aria-hidden="true"
          >
            <div className="landing-preview-canvas">
              <div className="landing-preview-bar">
                <span className="landing-preview-title-sm">
                  <Sparkles size={14} /> Trợ lý ẩm thực Mâm Xanh AI
                </span>
                <span className="landing-preview-pill">Gợi ý thông minh</span>
              </div>
              <div className="landing-preview-ai-box">
                <div className="landing-preview-ai-prompt">
                  “Mình đang có đậu hũ non và nấm đùi gà, muốn nấu bữa tối thuần
                  chay trong 25 phút giàu đạm.”
                </div>
                <div className="landing-preview-ai-reply">
                  <div className="landing-preview-ai-head">
                    <Sparkles size={13} /> Gợi ý phù hợp cho căn bếp của bạn
                  </div>
                  <p>
                    Gợi ý món <strong>Đậu Hũ Kho Nấm Đùi Gà Thố Đất</strong> —
                    thời gian chuẩn bị và nấu 25 phút, cung cấp khoảng 16g đạm
                    thực vật mỗi khẩu phần.
                  </p>
                  <div className="landing-preview-ai-recipe">
                    <img src="/assets/95427.png" alt="" loading="lazy" />
                    <div>
                      <strong>Đậu Hũ Kho Nấm Đùi Gà</strong>
                      <small>
                        <Clock3 size={11} /> 25 phút · Thuần chay · Dễ nấu
                      </small>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <figcaption>Hỗ trợ soạn công thức · Mâm Xanh</figcaption>
          </figure>

          <div className="landing-section-heading" data-reveal="right">
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
          <div className="landing-container" data-reveal="up">
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
          <div className="landing-container" data-reveal="up">
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
