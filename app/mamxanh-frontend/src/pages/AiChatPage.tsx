import { aiRequiredTier, aiUpgradeMessage, type AiPaidTier } from "../lib/aiUpgrade"
import {
  FormEvent,
  useEffect,
  useRef,
  useState,
  type KeyboardEvent,
  type PointerEvent,
} from "react"

import { Link } from "react-router-dom"

import {
  Bot,
  Check,
  ChevronDown,
  Clock3,
  Gauge,
  LoaderCircle,
  Send,
  ShieldAlert,
  Sparkles,
  UserRound,
  X,
} from "lucide-react"

import { useAuth } from "../components/AuthContext"

import { useAiChat } from "../components/AiChatContext"

import { recipesApi, type AiChatTurn } from "../api/recipes"
import { subscriptionApi, type AiChatSettingsResponse } from "../api/subscription"

import { asApiError } from "../lib/apiClient"
import { anonymousRecipeViewer } from "../lib/recipeViewer"

interface ChatMessage extends AiChatTurn {
  id: number

  recommendations?: Awaited<ReturnType<typeof recipesApi.chat>>["recommendations"]
  workflowUrl?: string | null
}

export function AiChatPage() {
  const { account, isAuthenticated } = useAuth()

  const { isOpen, recipeId, recipeTitle, recipeArticle, closeChat } =
    useAiChat()

  const hasRecipeContext = recipeId !== null
  const requestVersion = useRef(0)
  const activeRecipeTitle =
    recipeTitle?.trim() || (recipeId === null ? "" : `công thức #${recipeId}`)

  const [messages, setMessages] = useState<ChatMessage[]>([])

  const [question, setQuestion] = useState("")

  const [loading, setLoading] = useState(false)

  const [error, setError] = useState("")
  const [upgradeTier, setUpgradeTier] = useState<AiPaidTier>("PLUS")
  const [errorAction, setErrorAction] = useState<"plans" | "login" | null>(null)
  const [retryQuestion, setRetryQuestion] = useState("")
  const [chatSettings, setChatSettings] = useState<AiChatSettingsResponse | null>(null)
  const [settingsLoading, setSettingsLoading] = useState(false)
  const [settingsError, setSettingsError] = useState("")
  const [modelSaving, setModelSaving] = useState(false)
  const [modelMenuOpen, setModelMenuOpen] = useState(false)
  const [usageOpen, setUsageOpen] = useState(false)
  const modelMenuRef = useRef<HTMLDivElement>(null)

  const [sequence, setSequence] = useState(0)

  const [windowSize, setWindowSize] = useState({ width: 420, height: 620 })

  const resizeStart = useRef<{
    x: number
    y: number
    width: number
    height: number
  } | null>(null)

  const contextType = hasRecipeContext ? "RECIPE" : "GENERAL"

  const chatWindowStyle = {
    width: `min(${windowSize.width}px, calc(100vw - 1rem))`,

    height: `min(${windowSize.height}px, calc(100dvh - 1rem))`,
  }

  function startResize(event: PointerEvent<HTMLButtonElement>) {
    event.preventDefault()

    event.currentTarget.setPointerCapture(event.pointerId)

    resizeStart.current = {
      x: event.clientX,

      y: event.clientY,

      width: windowSize.width,

      height: windowSize.height,
    }
  }

  function resizeWindow(event: PointerEvent<HTMLButtonElement>) {
    const start = resizeStart.current

    if (!start) return

    const maxWidth = Math.max(320, window.innerWidth - 40)

    const maxHeight = Math.max(360, window.innerHeight - 40)

    setWindowSize({
      width: Math.min(
        maxWidth,
        Math.max(320, start.width + start.x - event.clientX),
      ),

      height: Math.min(
        maxHeight,
        Math.max(360, start.height + start.y - event.clientY),
      ),
    })
  }

  function stopResize() {
    resizeStart.current = null
  }

  function resizeWithKeyboard(event: KeyboardEvent<HTMLButtonElement>) {
    const step = event.shiftKey ? 48 : 24

    if (
      !["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(event.key)
    )
      return

    event.preventDefault()

    setWindowSize((current) => {
      const maxWidth = Math.max(320, window.innerWidth - 40)

      const maxHeight = Math.max(360, window.innerHeight - 40)

      return {
        width: Math.min(
          maxWidth,
          Math.max(
            320,
            current.width +
              (event.key === "ArrowLeft"
                ? step
                : event.key === "ArrowRight"
                  ? -step
                  : 0),
          ),
        ),

        height: Math.min(
          maxHeight,
          Math.max(
            360,
            current.height +
              (event.key === "ArrowUp"
                ? step
                : event.key === "ArrowDown"
                  ? -step
                  : 0),
          ),
        ),
      }
    })
  }

  const resizeHandle = (
    <button
      type="button"
      aria-label="Thay đổi kích thước khung chat"
      title="Kéo để thay đổi kích thước · Dùng phím mũi tên để chỉnh"
      onPointerDown={startResize}
      onPointerMove={resizeWindow}
      onPointerUp={stopResize}
      onPointerCancel={stopResize}
      onKeyDown={resizeWithKeyboard}
      className="absolute left-1 top-1 z-10 flex h-5 w-5 touch-none cursor-nwse-resize items-center justify-center rounded text-ink-muted/70 hover:bg-brand-50 hover:text-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600"
    >
      <span
        aria-hidden="true"
        className="h-2.5 w-2.5 border-l-2 border-t-2 border-current"
      />
    </button>
  )

  useEffect(() => {
    requestVersion.current += 1
    setLoading(false)
    setMessages([])

    setQuestion("")

    setError("")
    setErrorAction(null)
    setRetryQuestion("")

    setSequence(0)
  }, [recipeId, account?.id, isAuthenticated])

  useEffect(() => {
    if (!isOpen || account?.role === "ADMIN") return
    let active = true
    setSettingsLoading(true)
    setSettingsError("")
    const anonymousViewerId = isAuthenticated ? undefined : anonymousRecipeViewer()
    subscriptionApi.getChatSettings(anonymousViewerId)
      .then((settings) => { if (active) setChatSettings(settings) })
      .catch((cause) => { if (active) setSettingsError(asApiError(cause).message) })
      .finally(() => { if (active) setSettingsLoading(false) })
    return () => { active = false }
  }, [isOpen, account?.id, account?.role, isAuthenticated])

  useEffect(() => {
    if (!modelMenuOpen) return
    const dismiss = (event: Event) => {
      if (event.target instanceof Node && !modelMenuRef.current?.contains(event.target)) {
        setModelMenuOpen(false)
      }
    }
    const onKeyDown = (event: globalThis.KeyboardEvent) => {
      if (event.key === "Escape") setModelMenuOpen(false)
    }
    document.addEventListener("pointerdown", dismiss)
    document.addEventListener("keydown", onKeyDown)
    return () => {
      document.removeEventListener("pointerdown", dismiss)
      document.removeEventListener("keydown", onKeyDown)
    }
  }, [modelMenuOpen])

  if (!isOpen) return null

  if (account?.role === "ADMIN") {
    return (
      <section className="mx-auto my-12 max-w-xl rounded-2xl border border-brand-100 bg-paper p-6 text-center shadow-sm">
        <h1 className="text-2xl font-extrabold text-ink">
          Chatbot không dành cho tài khoản quản trị
        </h1>
        <p className="mt-3 text-sm leading-6 text-ink-muted">
          Bạn có thể tiếp tục sử dụng các chức năng quản trị của hệ thống.
        </p>
        <Link
          to="/"
          className="mt-5 inline-flex rounded-full bg-brand-600 px-5 py-2.5 text-sm font-semibold text-on-brand"
        >
          Về trang chủ
        </Link>
      </section>
    )
  }

  async function send(
    questionText: string,
    previous: ChatMessage[],
    addQuestion: boolean,
  ) {
    const normalized = questionText.trim()

    if (!normalized || loading) return
    const version = requestVersion.current
    setLoading(true)

    setError("")
    setErrorAction(null)
    setRetryQuestion(normalized)

    const currentId = sequence + 1

    setSequence(currentId)

    if (addQuestion)
      setMessages((current) => [
        ...current,
        { id: currentId, speaker: "USER", content: normalized },
      ])

    try {
      const response = await recipesApi.chat({
        contextType,
        ...(!isAuthenticated
          ? { anonymousViewerId: anonymousRecipeViewer() }
          : {}),
        ...(hasRecipeContext ? { recipeId } : {}),

        question: normalized,

        history: previous
          .slice(-8)
          .map(({ speaker, content, recommendations }) => ({
            speaker,
            content,
            ...(speaker === "ASSISTANT" && recommendations?.length
              ? {
                  recommendedRecipeIds: recommendations
                    .slice(0, 4)
                    .map((recipe) => recipe.id),
                }
              : {}),
          })),
      })
      if (version !== requestVersion.current) return
      setMessages((current) => [
        ...current,
        {
          id: currentId + 1,

          speaker: "ASSISTANT",

          content: response.answer,

          recommendations: response.recommendations ?? [],
          workflowUrl: response.workflowUrl,
        },
      ])

      setSequence(currentId + 1)

      setRetryQuestion("")
      void subscriptionApi.getChatSettings(isAuthenticated ? undefined : anonymousRecipeViewer())
        .then(setChatSettings)
        .catch(() => undefined)
    } catch (cause) {
      if (version !== requestVersion.current) return
      const apiError = asApiError(cause)
      setUpgradeTier(aiRequiredTier(apiError, "PLUS"))
      setErrorAction(
        apiError.code === "AI_SUBSCRIPTION_REQUIRED"
          ? "plans"
          : apiError.status === 401
            ? "login"
            : null,
      )
      if (apiError.code === "AI_TOKEN_QUOTA_EXCEEDED") setRetryQuestion("")
      if (apiError.code === "AI_TOKEN_QUOTA_EXCEEDED") {
        void subscriptionApi.getChatSettings(isAuthenticated ? undefined : anonymousRecipeViewer())
          .then(setChatSettings)
          .catch(() => undefined)
      }
      if (apiError.code === "AI_MODEL_SELECTION_REQUIRED") {
        setError("Model đang chọn không còn được gói của bạn hỗ trợ. Hãy chọn model khác rồi gửi lại câu hỏi.")
        setModelMenuOpen(true)
        setUsageOpen(false)
        void subscriptionApi.getChatSettings(isAuthenticated ? undefined : anonymousRecipeViewer())
          .then(setChatSettings)
          .catch(() => undefined)
      }
      if (apiError.status === 401 || apiError.status === 403)
        setRetryQuestion("")
      if (apiError.code !== "AI_MODEL_SELECTION_REQUIRED") setError(
        apiError.code === "AI_SUBSCRIPTION_REQUIRED"
          ? aiUpgradeMessage(apiError, "PLUS", "Tính năng AI bạn yêu cầu")
          : apiError.code === "AI_TOKEN_QUOTA_EXCEEDED" && !isAuthenticated
          ? "Bạn đã dùng hết 20.000 token dùng thử trong phiên này. Đăng ký tài khoản để tiếp tục sử dụng AI."
          : apiError.status === 429
          ? "Bạn đã gửi nhiều câu hỏi trong thời gian ngắn. Hãy thử lại sau; bạn cũng có thể đăng ký tài khoản để tiếp tục."
          : apiError.status === 0
            ? "Dịch vụ trả lời đang gặp sự cố. Vui lòng thử lại sau."
            : apiError.message,
      )
    } finally {
      if (version === requestVersion.current) setLoading(false)
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const normalized = question.trim()

    if (!normalized) return

    const previous = messages

    setQuestion("")

    void send(normalized, previous, true)
  }

  return (
    <div className="pointer-events-none fixed inset-2 z-50 flex items-end justify-end sm:inset-0 sm:p-5">
      <section
        role="dialog"
        aria-modal="false"
        style={chatWindowStyle}
        className="pointer-events-auto relative flex shrink-0 flex-col overflow-hidden rounded-2xl border border-brand-100 bg-paper shadow-2xl sm:max-h-[calc(100dvh-2.5rem)]"
        aria-labelledby="chat-heading"
      >
        {resizeHandle}
        <header className="flex items-center justify-between border-b border-brand-100 bg-cream-soft py-3 pl-8 pr-4">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-50 text-brand-700">
              <Bot className="h-5 w-5" aria-hidden="true" />
            </span>
            <div>
              <h1 id="chat-heading" className="font-extrabold text-ink">
                Bạn cần AI hỗ trợ gì?
              </h1>
              <p
                className="max-w-64 truncate text-xs text-ink-muted"
                title={hasRecipeContext ? activeRecipeTitle : undefined}
              >
                {hasRecipeContext
                  ? `Đang hỏi về: ${activeRecipeTitle}`
                  : "Trợ lý ẩm thực Mâm Xanh"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={closeChat}
            aria-label="Đóng khung chat"
            className="rounded-full p-2 text-ink-muted hover:bg-paper hover:text-ink"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        {!isAuthenticated && (
          <p className="border-b border-brand-100 px-4 py-2 text-xs text-ink-muted">
            Bạn đang trò chuyện với tư cách khách.{" "}
            <Link
              to="/dang-ky"
              className="font-semibold text-brand-700 underline"
            >
              Đăng ký
            </Link>{" "}
            để dùng các tính năng dành cho thành viên.
          </p>
        )}
        <p className="flex gap-2 border-b border-brand-100 bg-brand-50/70 px-4 py-2.5 text-[11px] leading-4 text-ink-soft">
          <ShieldAlert
            className="mt-0.5 h-3.5 w-3.5 shrink-0 text-brand-700"
            aria-hidden="true"
          />
          Câu trả lời do AI tạo và chỉ mang tính tham khảo. Hội thoại chỉ giữ
          trong phiên này.
        </p>

        <div
          className="flex-1 space-y-4 overflow-y-auto p-4"
          aria-live="polite"
          aria-label="Cuộc trò chuyện"
        >
          {messages.length === 0 &&
            (hasRecipeContext ? (
              <div className="flex min-h-full flex-col justify-center py-8">
                <div className="flex items-start gap-3">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700">
                    <Bot className="h-4 w-4" aria-hidden="true" />
                  </span>
                  <div className="max-w-[88%] rounded-2xl bg-raised px-4 py-3 text-sm leading-6 text-ink">
                    <p className="mb-1 text-[11px] font-bold opacity-75">
                      Trợ lý Mâm Xanh
                    </p>
                    <p>
                      Bạn muốn hỏi về công thức “{activeRecipeTitle}” phải
                      không?
                    </p>
                    <p className="mt-2 text-ink-muted">
                      Bạn có thể chọn một câu hỏi gợi ý hoặc nhập câu hỏi bên
                      dưới.
                    </p>
                  </div>
                </div>
                {recipeArticle && (
                  <article
                    aria-label={`Bài công thức: ${recipeArticle.title}`}
                    className="mt-4 overflow-hidden rounded-xl border border-brand-200 bg-paper text-ink shadow-xs"
                  >
                    <div className="flex gap-3 border-b border-brand-100 p-3">
                      {recipeArticle.coverUrl ? (
                        <img
                          src={recipeArticle.coverUrl}
                          alt={`Ảnh ${recipeArticle.title}`}
                          className="h-20 w-20 shrink-0 rounded-lg object-cover"
                        />
                      ) : (
                        <span className="flex h-20 w-20 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
                          <Bot className="h-7 w-7" aria-hidden="true" />
                        </span>
                      )}
                      <div className="min-w-0 flex-1">
                        <p className="text-[10px] font-bold uppercase tracking-wide text-brand-700">
                          Bài công thức đang xem
                        </p>
                        <h2 className="mt-0.5 font-bold leading-5">
                          {recipeArticle.title}
                        </h2>
                        {recipeArticle.description && (
                          <p className="mt-1 line-clamp-2 text-xs leading-4 text-ink-muted">
                            {recipeArticle.description}
                          </p>
                        )}
                        <div className="mt-2 flex min-w-0 items-center gap-1.5 text-[11px] text-ink-muted">
                          {recipeArticle.authorAvatarUrl ? (
                            <img
                              src={recipeArticle.authorAvatarUrl}
                              alt=""
                              className="h-5 w-5 shrink-0 rounded-full object-cover"
                            />
                          ) : (
                            <UserRound
                              className="h-3.5 w-3.5 shrink-0"
                              aria-hidden="true"
                            />
                          )}
                          <span className="truncate">
                            {recipeArticle.authorName}
                          </span>
                        </div>
                      </div>
                    </div>
                    <div className="space-y-3 p-3 text-xs leading-5">
                      <div className="flex flex-wrap gap-1.5">
                        {[
                          recipeArticle.vegetarianTypeLabel,
                          recipeArticle.dishCategoryLabel,
                          recipeArticle.difficultyLabel,
                        ]
                          .filter(Boolean)
                          .map((label) => (
                            <span
                              key={label}
                              className="rounded-full bg-brand-50 px-2 py-0.5 text-[10px] font-semibold text-brand-700"
                            >
                              {label}
                            </span>
                          ))}
                        <span className="inline-flex items-center gap-1 rounded-full bg-cream-soft px-2 py-0.5 text-[10px] text-ink-muted">
                          <Clock3 className="h-3 w-3" aria-hidden="true" />
                          {recipeArticle.prepTimeMinutes +
                            recipeArticle.cookTimeMinutes}{" "}
                          phút
                        </span>
                        <span className="rounded-full bg-cream-soft px-2 py-0.5 text-[10px] text-ink-muted">
                          {recipeArticle.servings} khẩu phần
                        </span>
                      </div>
                      <section>
                        <h3 className="font-bold text-ink">Nguyên liệu</h3>
                        {recipeArticle.ingredients.length ? (
                          <ul className="mt-1 list-inside list-disc space-y-0.5 text-ink-muted">
                            {recipeArticle.ingredients.map(
                              (ingredient, index) => (
                                <li key={`${ingredient.name}-${index}`}>
                                  {ingredient.name} — {ingredient.quantity}{" "}
                                  {ingredient.unitName}
                                </li>
                              ),
                            )}
                          </ul>
                        ) : (
                          <p className="text-ink-muted">
                            Bài viết chưa ghi thành phần.
                          </p>
                        )}
                      </section>
                      <section>
                        <h3 className="font-bold text-ink">Cách chế biến</h3>
                        <p className="mt-1 whitespace-pre-wrap text-ink-muted">
                          {recipeArticle.instructions ||
                            "Bài viết chưa ghi cách chế biến."}
                        </p>
                      </section>
                    </div>
                  </article>
                )}
                <div className="mt-5 flex flex-wrap justify-center gap-2">
                  {[
                    "Công thức này gồm những nguyên liệu nào?",

                    "Hướng dẫn mình từng bước chế biến món này.",

                    "Có thể thay nguyên liệu nào?",

                    "Công thức này nấu trong bao lâu?",
                  ].map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      onClick={() => setQuestion(suggestion)}
                      className="rounded-full border border-brand-200 bg-paper px-3 py-2 text-xs font-medium text-brand-700 hover:bg-brand-50"
                    >
                      {suggestion}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="flex min-h-full flex-col items-center justify-center py-8 text-center">
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-50 text-brand-700">
                  <Bot className="h-6 w-6" aria-hidden="true" />
                </span>
                <p className="mt-3 font-semibold text-ink">
                  Bạn cần AI hỗ trợ gì?
                </p>
                <p className="mt-1 max-w-xs text-sm text-ink-muted">
                  Hỏi về nguyên liệu, cách nấu hoặc lựa chọn món chay.
                </p>
                <div className="mt-5 flex max-w-sm flex-wrap justify-center gap-2">
                  {[
                    "Gợi ý món chay dễ nấu",
                    "Thay thế nguyên liệu",
                    "Cách bảo quản món ăn",
                  ].map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      onClick={() => setQuestion(suggestion)}
                      className="rounded-full border border-brand-200 bg-paper px-3 py-2 text-xs font-medium text-brand-700 hover:bg-brand-50"
                    >
                      {suggestion}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          {messages.map((message) => (
            <div
              key={message.id}
              className={`flex gap-3 ${
                message.speaker === "USER" ? "justify-end" : "justify-start"
              }`}
            >
              {message.speaker === "ASSISTANT" && (
                <span className="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-700">
                  <Bot className="h-4 w-4" aria-hidden="true" />
                </span>
              )}
              <div
                className={`max-w-[88%] min-w-0 rounded-2xl px-4 py-3 text-sm leading-6 ${
                  message.speaker === "USER"
                    ? "bg-brand-600 text-on-brand"
                    : "bg-raised text-ink"
                }`}
              >
                <p className="mb-1 text-[11px] font-bold opacity-75">
                  {message.speaker === "USER" ? "Bạn" : "AI"}
                </p>
                <p className="whitespace-pre-wrap">{message.content}</p>
                {message.speaker === "ASSISTANT" &&
                  Boolean(message.recommendations?.length) && (
                    <div
                      className="mt-3 space-y-2"
                      aria-label="Công thức được gợi ý"
                    >
                      {message.recommendations?.map((recipe) => (
                        <Link
                          key={recipe.id}
                          to={`/cong-thuc/id/${recipe.id}`}
                          onClick={closeChat}
                          className="flex min-w-0 gap-3 rounded-xl border border-brand-200 bg-paper p-2.5 text-ink transition hover:border-brand-400 hover:shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600"
                        >
                          {recipe.coverUrl ? (
                            <img
                              src={recipe.coverUrl}
                              alt=""
                              className="h-16 w-16 shrink-0 rounded-lg object-cover"
                            />
                          ) : (
                            <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
                              <Bot className="h-6 w-6" aria-hidden="true" />
                            </span>
                          )}
                          <span className="min-w-0 flex-1">
                            <span className="block truncate font-bold">
                              {recipe.title}
                            </span>
                            {recipe.description && (
                              <span className="mt-0.5 block line-clamp-1 text-xs text-ink-muted">
                                {recipe.description}
                              </span>
                            )}
                            <span className="mt-0.5 flex flex-wrap items-center gap-x-2 text-xs text-ink-muted">
                              <span>
                                {recipe.difficulty === "EASY"
                                  ? "Dễ"
                                  : recipe.difficulty === "MEDIUM"
                                    ? "Trung bình"
                                    : recipe.difficulty === "HARD"
                                      ? "Khó"
                                      : recipe.difficulty}
                              </span>
                              <span className="inline-flex items-center gap-1">
                                <Clock3
                                  className="h-3 w-3"
                                  aria-hidden="true"
                                />
                                {recipe.totalTimeMinutes} phút
                              </span>
                            </span>
                            <span className="mt-1 block line-clamp-2 text-xs leading-4 text-ink-soft">
                              {recipe.reason}
                            </span>
                          </span>
                        </Link>
                      ))}
                    </div>
                  )}
                {message.workflowUrl &&
                  ["/ke-hoach?view=for-you", "/dang-cong-thuc"].includes(
                    message.workflowUrl,
                  ) && (
                    <Link
                      to={message.workflowUrl}
                      onClick={closeChat}
                      className="mt-3 inline-flex font-semibold text-brand-700 underline"
                    >
                      {message.workflowUrl === "/dang-cong-thuc"
                        ? "Mở biểu mẫu soạn công thức"
                        : "Mở chức năng lập thực đơn"}
                    </Link>
                  )}
              </div>
              {message.speaker === "USER" && (
                <span className="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-stone-100 text-ink-muted">
                  <UserRound className="h-4 w-4" aria-hidden="true" />
                </span>
              )}
            </div>
          ))}
          {loading && (
            <p
              role="status"
              className="flex items-center gap-2 text-sm text-ink-muted"
            >
              <LoaderCircle className="h-4 w-4 animate-spin" />
              Đang trả lời…
            </p>
          )}
          {error && (
            <div
              role="alert"
              className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-900"
            >
              {error}
              {retryQuestion && (
                <button
                  type="button"
                  onClick={() =>
                    void send(retryQuestion, messages.slice(0, -1), false)
                  }
                  disabled={loading}
                  className="ml-2 font-bold underline"
                >
                  Thử lại
                </button>
              )}
              {error.includes("đăng ký") && (
                <Link to="/dang-ky" className="ml-2 font-bold underline">
                  Đăng ký
                </Link>
              )}
              {errorAction === "plans" && (
                <Link
                  to="/goi-ai"
                  onClick={closeChat}
                  className="ml-2 font-bold underline"
                >
                  Nâng cấp gói AI {upgradeTier}
                </Link>
              )}
              {errorAction === "login" && (
                <Link
                  to="/dang-nhap"
                  onClick={closeChat}
                  className="ml-2 font-bold underline"
                >
                  Đăng nhập
                </Link>
              )}
            </div>
          )}
        </div>

        <form
          onSubmit={submit}
          className="border-t border-brand-100 bg-paper p-3 sm:p-4"
        >
          <label htmlFor="ai-chat-question" className="sr-only">
            Câu hỏi của bạn
          </label>
          <div ref={modelMenuRef} className="relative rounded-2xl border border-brand-200 bg-paper shadow-sm transition focus-within:border-brand-300 focus-within:ring-2 focus-within:ring-brand-100">
            {modelMenuOpen && (
              <section
                id="chat-model-popover"
                aria-label="Model Gemini và hạn mức sử dụng"
                className="absolute bottom-[calc(100%+0.65rem)] left-0 z-30 w-[min(22rem,calc(100vw-3rem))] overflow-hidden rounded-2xl border border-brand-100 bg-paper text-ink shadow-xl"
              >
                <div className="border-b border-brand-50 px-4 py-3">
                  <p className="text-sm font-bold">Model Gemini</p>
                  <p className="mt-0.5 text-xs text-ink-muted">
                    {chatSettings ? `Các model khả dụng trong gói ${chatSettings.tier === "GUEST" ? "dùng thử" : chatSettings.tier}` : "Model được quản lý theo gói AI của bạn"}
                  </p>
                </div>
                {settingsLoading ? (
                  <div className="flex items-center gap-2 px-4 py-4 text-xs text-ink-muted" role="status">
                    <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" /> Đang tải model…
                  </div>
                ) : settingsError && !chatSettings ? (
                  <div className="px-4 py-3">
                    <p className="text-xs text-red-700">Không tải được danh sách model hoặc hạn mức.</p>
                    <button
                      type="button"
                      className="mt-2 text-xs font-semibold text-brand-700 underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600"
                      onClick={() => {
                        setSettingsLoading(true)
                        setSettingsError("")
                        subscriptionApi.getChatSettings(isAuthenticated ? undefined : anonymousRecipeViewer())
                          .then(setChatSettings)
                          .catch((cause) => setSettingsError(asApiError(cause).message))
                          .finally(() => setSettingsLoading(false))
                      }}
                    >Thử tải lại</button>
                  </div>
                ) : chatSettings ? (
                  <>
                    <div id="chat-model-menu" role="listbox" aria-label="Model được gói hỗ trợ" className="p-2">
                      {chatSettings.models.map((model) => {
                        const selected = chatSettings.selectedModelId === model.modelId
                        return (
                          <button
                            key={model.modelId}
                            type="button"
                            role="option"
                            aria-selected={selected}
                            disabled={modelSaving || loading}
                            onClick={() => {
                              if (selected) return
                              setModelSaving(true)
                              setSettingsError("")
                              subscriptionApi.selectChatModel(model.modelId)
                                .then((settings) => {
                                  setChatSettings(settings)
                                  setModelMenuOpen(false)
                                  setUsageOpen(false)
                                })
                                .catch((cause) => {
                                  setSettingsError(asApiError(cause).message)
                                  void subscriptionApi.getChatSettings(isAuthenticated ? undefined : anonymousRecipeViewer())
                                    .then(setChatSettings)
                                    .catch(() => undefined)
                                })
                                .finally(() => setModelSaving(false))
                            }}
                            className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition hover:bg-cream-soft focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600 disabled:opacity-60"
                          >
                            <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${selected ? "bg-brand-50 text-brand-700" : "bg-stone-50 text-ink-muted"}`}>
                              <Sparkles className="h-4 w-4" aria-hidden="true" />
                            </span>
                            <span className="min-w-0 flex-1">
                              <span className="block truncate text-sm font-semibold">{model.displayName}</span>
                              <span className="mt-0.5 block truncate text-[11px] text-ink-muted">{model.modelId}</span>
                            </span>
                            {model.isDefault && <span className="rounded-full bg-brand-50 px-2 py-1 text-[10px] font-semibold text-brand-700">Mặc định</span>}
                            {selected && <Check className="h-4 w-4 shrink-0 text-brand-700" aria-label="Đang chọn" />}
                          </button>
                        )
                      })}
                    </div>
                    {settingsError && <p className="mx-3 mb-2 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700" role="alert">{settingsError}</p>}
                    <div className="border-t border-brand-50 p-2">
                      <button
                        type="button"
                        aria-expanded={usageOpen}
                        aria-controls="chat-ai-usage"
                        onClick={() => setUsageOpen((open) => !open)}
                        className="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-left text-sm font-semibold text-ink-muted hover:bg-cream-soft hover:text-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600"
                      >
                        <Gauge className="h-4 w-4" aria-hidden="true" />
                        <span className="flex-1">Xem hạn mức sử dụng</span>
                        <ChevronDown className={`h-4 w-4 transition-transform ${usageOpen ? "rotate-180" : ""}`} aria-hidden="true" />
                      </button>
                      {usageOpen && (
                        <div id="chat-ai-usage" className="mt-1 rounded-xl bg-cream-soft p-3">
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <p className="text-xs font-semibold text-ink">{chatSettings.tier === "GUEST" ? "Hạn mức dùng thử" : `Gói ${chatSettings.tier}`}</p>
                              <p className="mt-1 text-[11px] text-ink-muted">Đã dùng {chatSettings.consumedTokens.toLocaleString("vi-VN")} / {chatSettings.quotaTokens.toLocaleString("vi-VN")} token</p>
                            </div>
                            <p className="shrink-0 text-right text-xs font-bold text-brand-700">Còn<br />{chatSettings.remainingTokens.toLocaleString("vi-VN")}</p>
                          </div>
                          <div className="mt-3 h-2 overflow-hidden rounded-full bg-paper" role="progressbar" aria-label="Mức sử dụng token trong kỳ hiện tại" aria-valuemin={0} aria-valuemax={chatSettings.quotaTokens} aria-valuenow={chatSettings.consumedTokens}>
                            <div className="h-full rounded-full bg-brand-600 transition-[width]" style={{ width: `${chatSettings.quotaTokens ? Math.min(100, chatSettings.consumedTokens / chatSettings.quotaTokens * 100) : 0}%` }} />
                          </div>
                          <div className="mt-2 flex justify-between gap-2 text-[10px] text-ink-muted">
                            <span>{chatSettings.tier === "GUEST" ? "Tính theo phiên khách" : "Hạn mức theo tháng"}</span>
                            {chatSettings.resetsAt && <span>Cấp lại {new Date(chatSettings.resetsAt).toLocaleDateString("vi-VN")}</span>}
                          </div>
                          {chatSettings.tier === "GUEST" && <Link to="/dang-ky" onClick={closeChat} className="mt-3 inline-flex text-xs font-semibold text-brand-700 underline">Đăng ký để có hạn mức tháng</Link>}
                        </div>
                      )}
                    </div>
                  </>
                ) : null}
              </section>
            )}
            <textarea
              id="ai-chat-question"
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              onKeyDown={(event) => {
                if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) return
                event.preventDefault()
                event.currentTarget.form?.requestSubmit()
              }}
              maxLength={1000}
              rows={2}
              placeholder="Nhập câu hỏi của bạn…"
              className="block max-h-36 min-h-20 w-full resize-y bg-transparent px-4 py-3 text-sm text-ink outline-none placeholder:text-ink-muted"
              disabled={loading}
            />
            <div className="flex items-center justify-between gap-2 border-t border-brand-50 px-2 py-2">
              <div className="flex min-w-0 items-center gap-2">
                <button
                  type="button"
                  aria-haspopup="dialog"
                  aria-expanded={modelMenuOpen}
                  aria-controls="chat-model-menu"
                  onClick={() => setModelMenuOpen((open) => !open)}
                  disabled={settingsLoading}
                  className="flex min-w-0 items-center gap-2 rounded-xl px-2.5 py-2 text-left text-xs text-ink-muted transition hover:bg-cream-soft hover:text-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600 disabled:opacity-60"
                >
                  <Sparkles className="h-4 w-4 shrink-0 text-brand-700" aria-hidden="true" />
                  <span className="min-w-0 truncate font-semibold text-ink">{chatSettings?.models.find((model) => model.modelId === chatSettings.selectedModelId)?.displayName ?? (settingsLoading ? "Đang tải model" : "Chọn model")}</span>
                  <span className="hidden shrink-0 sm:inline">· {chatSettings?.tier === "GUEST" ? "Guest" : chatSettings?.tier ?? "AI"}</span>
                  <ChevronDown className={`h-3.5 w-3.5 shrink-0 transition-transform ${modelMenuOpen ? "rotate-180" : ""}`} aria-hidden="true" />
                </button>
                <span className="text-[10px] text-ink-muted">{question.length}/1.000</span>
              </div>
              <button
                type="submit"
                aria-label="Gửi câu hỏi"
                disabled={loading || !question.trim()}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-600 text-on-brand enabled:hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
              >
                {loading ? <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" /> : <Send className="h-4 w-4" aria-hidden="true" />}
              </button>
            </div>
          </div>
        </form>
      </section>
    </div>
  )
}
