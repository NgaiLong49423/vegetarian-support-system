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
      </section>
    )
  }

  async function send(
    text: string,
    history: ChatMessage[],
    clearInput = true,
  ) {
    const trimmed = text.trim()
    if (!trimmed || loading || retrySeconds > 0) return

    const userMessage: ChatMessage = {
      id: sequence + 1,
      speaker: "USER",
      content: trimmed,
    }
    const nextMessages = [...history, userMessage]
    const currentVersion = contextVersion.current

    setMessages(nextMessages)
    setSequence((current) => current + 1)
    if (clearInput) setQuestion("")
    setLoading(true)
    setError("")
    setRetryQuestion("")

    try {
      const response = await recipesApi.chat({
        contextType,
        ...(hasRecipeContext ? { recipeId } : {}),
        question: trimmed,
        history: history.map(({ speaker, content }) => ({ speaker, content })),
      })

      if (contextVersion.current !== currentVersion) return

      setMessages((current) => [
        ...current,
        {
          id: sequence + 2,
          speaker: "ASSISTANT",
          content: response.answer,
          recommendations: response.recommendations,
        },
      ])
      setSequence((current) => current + 2)
    } catch (cause) {
      if (contextVersion.current !== currentVersion) return

      const apiError = asApiError(cause)
      const status = apiError.status

      if (status === 429) {
        const retryHeader = (cause as { response?: { headers?: Record<string, string> } })?.response?.headers?.["retry-after"]
        const retrySecondsValue = retryHeader ? parseInt(retryHeader, 10) : 60
        const delaySeconds = Number.isFinite(retrySecondsValue) && retrySecondsValue > 0 ? retrySecondsValue : 60
        setRetryAt(Date.now() + delaySeconds * 1000)
        setError(`Bạn đang thao tác quá nhanh. Vui lòng chờ ${delaySeconds} giây trước khi gửi tiếp.`)
      } else {
        const message =
          apiError.message ||
          "Không thể nhận phản hồi từ AI. Vui lòng thử lại sau."
        setError(message)
        setRetryQuestion(trimmed)
      }
    } finally {
      if (contextVersion.current === currentVersion) {
        setLoading(false)
      }
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    void send(question, messages, true)
  }

  return (
    <div
      role="dialog"
      aria-modal="false"
      aria-label="Khung chat hỗ trợ AI"
      style={chatWindowStyle}
      className="fixed bottom-4 right-4 z-50 flex flex-col overflow-hidden rounded-3xl border border-brand-200/80 bg-paper shadow-2xl shadow-brand-950/20 ring-1 ring-black/5"
    >
      <header className="relative flex shrink-0 items-center justify-between border-b border-brand-100 bg-linear-to-r from-brand-50 to-paper px-4 py-3">
        {resizeHandle}
        <div className="flex min-w-0 items-center gap-2 pl-4">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 text-on-brand shadow-xs">
            <Bot className="h-4 w-4" aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <h2 className="truncate text-sm font-bold text-ink">Trợ lý AI</h2>
            <p className="truncate text-xs text-ink-muted">
              {contextType === "RECIPE"
                ? `Hỏi đáp về: ${activeRecipeTitle}`
                : "Hỗ trợ ẩm thực & công thức chay"}
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={closeChat}
          aria-label="Đóng khung chat"
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-ink-muted hover:bg-brand-100/60 hover:text-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      </header>

      <section className="flex min-h-0 flex-1 flex-col">
        <div className="flex-1 space-y-4 overflow-y-auto p-4">
          {messages.length === 0 && (
            <div className="rounded-2xl bg-cream-soft p-4 text-sm leading-6 text-ink-soft">
              <p className="font-semibold text-brand-800">
                Xin chào! Tôi có thể giúp gì cho bạn?
              </p>
              <p className="mt-1 text-xs text-ink-muted">
                {contextType === "RECIPE"
                  ? `Bạn có thể hỏi tôi về nguyên liệu, thời gian nấu hoặc cách thực hiện của ${activeRecipeTitle}.`
                  : "Hỏi tôi về các công thức chay, cách kết hợp nguyên liệu hoặc gợi ý món ăn dinh dưỡng."}
              </p>
            </div>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              className={`flex items-start gap-2.5 ${
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
                          className="flex min-w-0 gap-3 rounded-xl border border-brand-200 bg-paper p-2.5 text-ink transition hover:border-brand-400 hover:shadow-xs focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-600"
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
              <p>{error}</p>
              {retrySeconds > 0 && (
                <p className="mt-1 text-xs text-red-700">
                  Thử lại sau {retrySeconds} giây.
                </p>
              )}
              {retryQuestion && retrySeconds === 0 && (
                <button
                  type="button"
                  onClick={() =>
                    void send(retryQuestion, messages.slice(0, -1), false)
                  }
                  disabled={loading}
                  className="mt-1 font-bold underline"
                >
                  Thử lại
                </button>
              )}
              {error.includes("đăng ký") && (
                <Link to="/dang-ky" className="ml-2 font-bold underline">
                  Đăng ký
                </Link>
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
              disabled={loading || retrySeconds > 0}
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
