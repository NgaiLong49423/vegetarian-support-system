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
  Clock3,
  LoaderCircle,
  Send,
  ShieldAlert,
  UserRound,
  X,
} from "lucide-react"
import { useAuth } from "../components/AuthContext"
import { useAiChat } from "../components/AiChatContext"
import { recipesApi, type AiChatTurn } from "../api/recipes"
import { asApiError } from "../lib/apiClient"

interface ChatMessage extends AiChatTurn {
  id: number
  recommendations?: Awaited<ReturnType<typeof recipesApi.chat>>["recommendations"]
}

export function AiChatPage() {
  const { account, isAuthenticated } = useAuth()
  const { isOpen, recipeId, recipeTitle, recipeArticle, closeChat } =
    useAiChat()
  const hasRecipeContext = recipeId !== null
  const activeRecipeTitle =
    recipeTitle?.trim() || (recipeId === null ? "" : `công thức #${recipeId}`)

  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [question, setQuestion] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [retryQuestion, setRetryQuestion] = useState("")
  const [retryAt, setRetryAt] = useState(0)
  const [retrySeconds, setRetrySeconds] = useState(0)
  const contextVersion = useRef(0)
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
    contextVersion.current += 1
    setMessages([])
    setQuestion("")
    setError("")
    setRetryQuestion("")
    setSequence(0)
    setLoading(false)
    setRetryAt(0)
    setRetrySeconds(0)
    return () => {
      contextVersion.current += 1
    }
  }, [recipeId, account?.id])

  useEffect(() => {
    if (!retryAt) return
    const tick = () =>
      setRetrySeconds(Math.max(0, Math.ceil((retryAt - Date.now()) / 1000)))
    tick()
    const timer = window.setInterval(tick, 1000)
    return () => window.clearInterval(timer)
  }, [retryAt])

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
    if (!normalized || loading || retrySeconds > 0) return

    const version = contextVersion.current
    setLoading(true)
    setError("")
    setRetryQuestion(normalized)

    const currentId = sequence + 1
    setSequence(currentId)
    if (addQuestion) {
      setMessages((current) => [
        ...current,
        { id: currentId, speaker: "USER", content: normalized },
      ])
    }

    try {
      const response = await recipesApi.chat({
        contextType,
        ...(hasRecipeContext ? { recipeId } : {}),
        question: normalized,
        history: previous
          .slice(-8)
          .map(({ speaker, content }) => ({ speaker, content })),
      })

      if (version !== contextVersion.current) return

      setMessages((current) => [
        ...current,
        {
          id: currentId + 1,
          speaker: "ASSISTANT",
          content: response.answer,
          recommendations: response.recommendations ?? [],
        },
      ])
      setSequence(currentId + 1)
      setRetryQuestion("")
    } catch (cause) {
      if (version !== contextVersion.current) return

      const apiError = asApiError(cause)
      if (apiError.status === 429) {
        const wait = Math.ceil(apiError.retryAfter ?? 60)
        setRetryAt(Date.now() + wait * 1000)
        setRetrySeconds(wait)
      }

      setError(
        apiError.status === 429
          ? "Bạn đã gửi nhiều câu hỏi trong thời gian ngắn. Vui lòng tạm dừng và thử lại sau."
          : apiError.code === "AI_PROVIDER_UNAVAILABLE" ||
              apiError.code === "AI_RESPONSE_INVALID" ||
              apiError.status === 0
            ? "Dịch vụ trả lời đang gặp sự cố. Vui lòng thử lại sau."
            : apiError.message,
      )
    } finally {
      if (version === contextVersion.current) {
        setLoading(false)
      }
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalized = question.trim()
    if (!normalized || loading || retrySeconds > 0) return
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

        <p className="flex gap-2 border-b border-brand-100 bg-brand-50/70 px-4 py-2.5 text-[11px] leading-4 text-ink-soft">
          <ShieldAlert
            className="mt-0.5 h-3.5 w-3.5 shrink-0 text-brand-700"
            aria-hidden="true"
          />
          Câu trả lời do AI tạo và chỉ mang tính tham khảo. Câu hỏi được chuyển
          tới Google Gemini để xử lý. Hội thoại chỉ giữ trong phiên này.
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
              {retrySeconds > 0 && <p>Thử lại sau {retrySeconds} giây.</p>}
              {retryQuestion && (
                <button
                  type="button"
                  onClick={() =>
                    void send(retryQuestion, messages.slice(0, -1), false)
                  }
                  disabled={loading || retrySeconds > 0}
                  className="ml-2 font-bold underline disabled:opacity-50"
                >
                  Thử lại
                </button>
              )}
            </div>
          )}

          {!isAuthenticated &&
            (messages.some((message) => message.speaker === "ASSISTANT") ||
              Boolean(error)) && (
              <p className="rounded-xl bg-brand-50 p-3 text-sm text-ink-soft">
                <Link
                  to="/dang-ky"
                  className="font-bold text-brand-700 underline"
                >
                  Đăng ký tài khoản
                </Link>{" "}
                để lưu công thức và quản lý thực đơn của bạn. Hội thoại AI không
                được lưu theo tài khoản.
              </p>
            )}
        </div>

        <form
          onSubmit={submit}
          className="border-t border-brand-100 bg-paper p-3 sm:p-4"
        >
          <label htmlFor="ai-chat-question" className="sr-only">
            Câu hỏi của bạn
          </label>
          <div className="flex items-end gap-2 rounded-2xl border border-brand-200 bg-cream-soft p-2 focus-within:ring-2 focus-within:ring-brand-300">
            <textarea
              id="ai-chat-question"
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              onKeyDown={(event) => {
                if (
                  event.key !== "Enter" ||
                  event.shiftKey ||
                  event.nativeEvent.isComposing
                )
                  return

                event.preventDefault()

                event.currentTarget.form?.requestSubmit()
              }}
              maxLength={1000}
              rows={2}
              placeholder="Nhập câu hỏi của bạn…"
              className="max-h-36 min-h-12 flex-1 resize-y bg-transparent px-2 py-2 text-sm text-ink outline-none placeholder:text-ink-muted"
              disabled={loading}
            />
            <button
              type="submit"
              aria-label="Gửi câu hỏi"
              disabled={loading || retrySeconds > 0 || !question.trim()}
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-600 text-on-brand enabled:hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Send className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
          <p className="mt-1 text-right text-[11px] text-ink-muted">
            {question.length}/1.000 ký tự
          </p>
        </form>
      </section>
    </div>
  )
}
