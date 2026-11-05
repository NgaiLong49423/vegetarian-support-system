import { useId, useState, type FormEvent } from "react"
import { Check, CircleHelp, LoaderCircle, Pencil, Send } from "lucide-react"
import type { PersistedAiChatMessage } from "../lib/aiChatSession"

interface Props {
  message: PersistedAiChatMessage
  busy: boolean
  onDecision: (approved: boolean) => Promise<void>
  onReply: (text: string) => Promise<void>
}

/** A user decision is explicit: free text never grants consent or approves a write. */
export function AiHumanInputCard({ message, busy, onDecision, onReply }: Props) {
  const id = useId()
  const [selection, setSelection] = useState<"approve" | "reject" | null>(null)
  const [reply, setReply] = useState("")
  const consent = message.consentRequest
  const pending = message.pendingConfirmation
  const title = consent ? "Cho phép dùng dữ liệu cá nhân?" : "Xem lại trước khi thực hiện"
  const options = consent
    ? ["Đồng ý và tiếp tục", "Không đồng ý"]
    : ["Xác nhận thay đổi", "Hủy"]
  const categories = consent ? Array.from(new Set(consent.scopes.map(scope => ({
    HEALTH_PROFILE: "hồ sơ sức khỏe",
    DIETARY_PREFERENCES: "tùy chọn ăn uống",
    SHOPPING_LIST: "danh sách mua sắm (nội dung yêu cầu, tên, ID và kết quả danh sách — mục đích tìm hướng dẫn và điều phối)",
    BASIC_PROFILE: "hồ sơ cơ bản",
  })[scope.category]))).join(", ") : ""

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    if (reply.trim()) await onReply(reply.trim())
    else if (selection) await onDecision(selection === "approve")
  }

  return (
    <form onSubmit={event => void submit(event)} aria-labelledby={`${id}-title`}
      onKeyDown={event => {
        if (busy || event.altKey || event.ctrlKey || event.metaKey || event.target instanceof HTMLTextAreaElement) return
        if (event.key === "1" || event.key === "2") {
          event.preventDefault(); setSelection(event.key === "1" ? "approve" : "reject"); setReply("")
        }
      }}
      className="overflow-hidden rounded-2xl border border-ink/10 bg-raised text-ink shadow-sm">
      <div className="max-h-[45dvh] overflow-y-auto px-4 pt-4 sm:px-5">
        <div className="mb-3 flex items-center gap-2 text-xs font-medium text-ink-muted">
          <CircleHelp className="h-4 w-4" aria-hidden="true" />
          <span>Cần bạn phản hồi</span>
        </div>
        <h2 id={`${id}-title`} className="text-sm font-bold leading-6">{title}</h2>
        <p className="mt-1 text-xs leading-5 text-ink-muted">
          {consent ? <>Chỉ khi đồng ý, nội dung liên quan mới được gửi tới {consent.recipient}. Phạm vi: {categories}.</>
            : <>{pending?.summary}. Các thay đổi chưa được áp dụng.</>}
        </p>
        {pending?.proposedListName && <p className="mt-2 text-sm font-semibold leading-6">Tên danh sách dự kiến: “{pending.proposedListName}”</p>}
        <fieldset disabled={busy} className="mt-3 space-y-1 pb-3">
          <legend className="sr-only">Chọn một phương án</legend>
          {options.map((label, index) => {
            const value = index === 0 ? "approve" : "reject"
            const selected = selection === value
            return (
              <label key={value} className={`group relative flex cursor-pointer items-center gap-3 rounded-xl border px-2.5 py-2.5 transition focus-within:ring-2 focus-within:ring-brand-300 ${selected ? "border-brand-300 bg-paper" : "border-transparent hover:bg-paper/70"} ${busy ? "cursor-wait opacity-60" : ""}`}>
                <input type="radio" name={`${id}-decision`} value={value} checked={selected} className="absolute inset-0 z-10 h-full w-full cursor-pointer opacity-0 disabled:cursor-wait"
                  onChange={() => { setSelection(value); setReply("") }} />
                <span aria-hidden="true" className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md border text-xs font-semibold ${selected ? "border-brand-300 bg-brand-50 text-brand-700" : "border-ink/10 bg-paper/60 text-ink-muted"}`}>{index + 1}</span>
                <span className="flex-1 text-sm font-semibold">{label}</span>
                {selected && <Check className="h-4 w-4 text-brand-700" aria-hidden="true" />}
              </label>
            )
          })}
        </fieldset>
      </div>
      <div className="border-t border-ink/10 px-4 py-3 sm:px-5">
        <label htmlFor={`${id}-reply`} className="mb-1 flex items-center gap-2 text-xs text-ink-muted">
          <Pencil className="h-3.5 w-3.5" aria-hidden="true" /> Hoặc viết yêu cầu khác
        </label>
        <textarea id={`${id}-reply`} value={reply} disabled={busy} maxLength={1000} rows={1}
          onChange={event => { setReply(event.target.value); setSelection(null) }}
          onKeyDown={event => {
            if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
              event.preventDefault(); event.currentTarget.form?.requestSubmit()
            }
          }}
          placeholder="Nhập phản hồi của bạn…"
          className="block max-h-24 min-h-9 w-full resize-y rounded-lg bg-transparent py-1.5 text-sm outline-none placeholder:text-ink-muted/70 focus-visible:ring-2 focus-visible:ring-brand-300" />
        <div className="mt-2 flex items-end justify-between gap-3">
          <p className="max-w-64 text-[11px] leading-4 text-ink-muted">{reply.trim() ? "Yêu cầu mới không đồng nghĩa với xác nhận hoặc cấp quyền dữ liệu." : "Chọn một phương án, rồi nhấn Gửi."}</p>
          <div className="flex shrink-0 items-center gap-2">
            <button type="button" disabled={busy} onClick={() => void onDecision(false)}
              className="rounded-full border border-ink/15 px-3 py-1.5 text-xs font-semibold transition hover:bg-paper focus-visible:outline-2 focus-visible:outline-brand-600 disabled:opacity-50">Bỏ qua</button>
            <button type="submit" disabled={busy || (!selection && !reply.trim())}
              className="inline-flex items-center gap-1.5 rounded-full bg-brand-600 px-3.5 py-1.5 text-xs font-semibold text-on-brand transition hover:bg-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:cursor-not-allowed disabled:opacity-40">
              {busy ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" aria-hidden="true" /> : <Send className="h-3.5 w-3.5" aria-hidden="true" />} Gửi
            </button>
          </div>
        </div>
      </div>
    </form>
  )
}
