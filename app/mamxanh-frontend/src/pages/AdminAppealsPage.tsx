import { useCallback, useEffect, useState, type FormEvent } from "react"
import { AlertCircle, CheckCircle2, LoaderCircle, RefreshCw } from "lucide-react"
import { AdminPageHeader } from "../components/AdminPageHeader"
import { Modal } from "../components/Modal"
import { Button, Card } from "../components/ui"
import { asApiError } from "../lib/apiClient"
import { formatSubscriptionDateTime as formatDateTime } from "../utils/subscriptionDate"
import {
  adminAppealsApi,
  type AdminAppeal,
  type AppealDecision,
  type AppealStatus,
} from "../api/appeals"

const REASON_MIN = 10
const REASON_MAX = 500

const filters: Array<{ label: string; value: AppealStatus[] }> = [
  { label: "Chờ xử lý", value: ["PENDING", "RESUBMITTED"] },
  { label: "Đang cho sửa", value: ["EDIT_ALLOWED"] },
  { label: "Đã mở lại", value: ["RESTORED"] },
  { label: "Giữ ẩn", value: ["KEPT_HIDDEN"] },
  { label: "Tất cả", value: [] },
]

const statusLabels: Record<AppealStatus, string> = {
  PENDING: "Chờ xử lý",
  EDIT_ALLOWED: "Đang cho sửa",
  RESUBMITTED: "Đã gửi duyệt lại",
  RESTORED: "Đã mở lại",
  KEPT_HIDDEN: "Giữ ẩn",
}

const decisionLabels: Record<AppealDecision, string> = {
  RESTORE: "Mở lại bài",
  KEEP_HIDDEN: "Giữ ẩn",
  ALLOW_EDIT: "Cho phép sửa",
}

function doneNotice(decision: AppealDecision, title: string) {
  if (decision === "RESTORE") return `Đã mở lại bài “${title}”.`
  if (decision === "KEEP_HIDDEN") return `Đã giữ ẩn bài “${title}”.`
  return `Đã cho phép tác giả sửa “${title}”. Bài vẫn đang ẩn.`
}

/** FR-28 / BR-27: Administrator queue of appeals on hidden Recipe Posts. */
export function AdminAppealsPage() {
  const [filter, setFilter] = useState(0)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [appeals, setAppeals] = useState<AdminAppeal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [notice, setNotice] = useState("")
  const [selected, setSelected] = useState<AdminAppeal | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const result = await adminAppealsApi.list(filters[filter].value, page)
      // A decision can empty the last page: step back to the new last page instead of showing nothing.
      if (result.content.length === 0 && page > 0) {
        setPage(Math.max(0, result.totalPages - 1))
        return
      }
      setAppeals(result.content)
      setTotalPages(result.totalPages)
    } catch (cause) {
      setError(asApiError(cause).message)
    } finally {
      setLoading(false)
    }
  }, [filter, page])

  useEffect(() => {
    void load()
  }, [load])

  async function open(item: AdminAppeal) {
    setError("")
    try {
      setSelected(await adminAppealsApi.detail(item.appeal.reportId))
    } catch (cause) {
      setError(asApiError(cause).message)
    }
  }

  function decided(decision: AppealDecision, updated: AdminAppeal) {
    setSelected(null)
    setNotice(doneNotice(decision, updated.appeal.recipeTitle))
    void load()
  }

  return (
    <div className="space-y-5 py-2">
      <AdminPageHeader
        title="Khiếu nại bài bị ẩn"
        description="Chuyên gia gửi khiếu nại khi bài bị ẩn. Xem nội dung bài và lời khiếu nại, rồi mở lại bài, giữ ẩn hoặc cho phép tác giả sửa trước khi duyệt lại."
        actions={
          <Button variant="outline" size="sm" disabled={loading} onClick={() => { setError(""); void load() }}>
            <RefreshCw className="h-3.5 w-3.5" aria-hidden="true" /> Làm mới
          </Button>
        }
      />

      {error && !selected && (
        <div role="alert" className="flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50/90 p-4 text-sm text-rose-800">
          <AlertCircle className="h-5 w-5 shrink-0 text-rose-600" aria-hidden="true" />
          <p className="flex-1 font-medium">{error}</p>
        </div>
      )}
      {notice && (
        <div role="status" className="flex items-center gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/90 p-4 text-sm text-leaf-800">
          <CheckCircle2 className="h-5 w-5 shrink-0 text-leaf-600" aria-hidden="true" />
          <p className="flex-1 font-medium">{notice}</p>
        </div>
      )}

      <Card className="flex flex-wrap items-center gap-2 p-3.5 sm:px-5" role="group" aria-label="Lọc theo trạng thái khiếu nại">
        {filters.map((item, index) => (
          <button
            key={item.label}
            type="button"
            aria-pressed={filter === index}
            onClick={() => { setError(""); setNotice(""); setFilter(index); setPage(0) }}
            className={`rounded-xl border px-3.5 py-2 text-sm font-bold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400/70 ${
              filter === index
                ? "border-brand-600 bg-brand-600 text-on-brand"
                : "border-brand-200 bg-paper text-ink-soft hover:border-brand-300 hover:text-brand-600"
            }`}
          >
            {item.label}
          </button>
        ))}
      </Card>

      {loading ? (
        <Card className="p-8 text-center text-sm text-ink-muted">
          <LoaderCircle className="mx-auto mb-2 h-6 w-6 animate-spin" aria-hidden="true" />Đang tải khiếu nại…
        </Card>
      ) : appeals.length === 0 ? (
        <Card className="p-8 text-center text-sm text-ink-muted">Không có khiếu nại nào ở trạng thái này.</Card>
      ) : (
        <div className="space-y-3">
          {appeals.map((item) => (
            <Card key={item.appeal.reportId} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
              <div className="min-w-0">
                <p className="text-xs font-semibold text-ink-muted">
                  Khiếu nại #{item.appeal.reportId} · {item.author.displayName} · {formatDateTime(item.appeal.resubmittedAt ?? item.appeal.appealedAt ?? item.appeal.hiddenAt)}
                </p>
                <h3 className="mt-1 truncate text-base font-bold text-ink">{item.appeal.recipeTitle}</h3>
                <p className="mt-1 line-clamp-2 text-sm text-ink-soft">{item.appeal.appealMessage}</p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                {item.appeal.appealStatus && (
                  <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-bold text-amber-900">
                    {statusLabels[item.appeal.appealStatus]}
                  </span>
                )}
                <Button size="sm" aria-label={`Xem khiếu nại ${item.appeal.recipeTitle}`} onClick={() => void open(item)}>
                  Xem
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>
            Trang trước
          </Button>
          <span className="text-sm font-medium text-ink-muted">Trang {page + 1}/{totalPages}</span>
          <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>
            Trang sau
          </Button>
        </div>
      )}

      <Modal
        open={selected !== null}
        onClose={() => { setSelected(null); setError("") }}
        title={selected ? `Khiếu nại #${selected.appeal.reportId}` : "Khiếu nại"}
        size="lg"
      >
        {selected && <AppealDetail item={selected} onDecided={decided} />}
      </Modal>
    </div>
  )
}

function AppealDetail({ item, onDecided }: { item: AdminAppeal; onDecided: (decision: AppealDecision, updated: AdminAppeal) => void }) {
  const { appeal, author, recipe } = item
  const open = appeal.appealStatus === "PENDING" || appeal.appealStatus === "RESUBMITTED"
  const choices: AppealDecision[] = appeal.appealStatus === "PENDING"
    ? ["RESTORE", "KEEP_HIDDEN", "ALLOW_EDIT"]
    : ["RESTORE", "KEEP_HIDDEN"]
  const [decision, setDecision] = useState<AppealDecision | "">("")
  const [reason, setReason] = useState("")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const trimmed = reason.trim().length

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!decision) return
    setBusy(true)
    setError("")
    try {
      onDecided(decision, await adminAppealsApi.resolve(appeal.reportId, decision, reason.trim()))
    } catch (cause) {
      setError(asApiError(cause).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-4 text-sm">
      <section className="rounded-xl bg-amber-50 p-3 text-amber-950">
        <p><strong>Tác giả:</strong> {author.displayName} ({author.email})</p>
        <p className="mt-1"><strong>Lý do ẩn ban đầu:</strong> {appeal.hideReason}</p>
        <p className="mt-1"><strong>Lời khiếu nại:</strong> {appeal.appealMessage}</p>
        {appeal.appealStatus === "RESUBMITTED" && <p className="mt-1 font-semibold">Tác giả đã sửa bài và gửi duyệt lại.</p>}
        {appeal.adminReason && <p className="mt-1"><strong>Kết luận gần nhất:</strong> {appeal.adminReason}</p>}
      </section>

      {recipe && (
        <section className="rounded-xl border border-brand-100 p-3">
          <h3 className="text-base font-bold text-ink">{recipe.title}</h3>
          {recipe.description && <p className="mt-1 text-ink-muted">{recipe.description}</p>}
          <p className="mt-2 whitespace-pre-line text-ink-soft">{recipe.instructions}</p>
          <ul className="mt-2 list-inside list-disc text-ink-soft">
            {recipe.ingredients.map((ingredient, index) => (
              <li key={`${ingredient.ingredientId}-${index}`}>{ingredient.name} — {ingredient.quantity} {ingredient.unitName}</li>
            ))}
          </ul>
          {recipe.media.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-2">
              {recipe.media.map((media) => <img key={media.displayOrder} src={media.url} alt="Ảnh công thức" className="h-16 w-16 rounded-lg object-cover" />)}
            </div>
          )}
        </section>
      )}

      {open ? (
        <form onSubmit={submit} className="space-y-3">
          <fieldset className="flex flex-wrap gap-3">
            <legend className="mb-1 font-semibold text-ink">Kết luận</legend>
            {choices.map((choice) => (
              <label key={choice} className="inline-flex items-center gap-2 rounded-xl border border-brand-200 px-3 py-2">
                <input type="radio" name="appeal-decision" value={choice} checked={decision === choice} onChange={() => setDecision(choice)} />
                {decisionLabels[choice]}
              </label>
            ))}
          </fieldset>
          <label className="block font-semibold text-ink" htmlFor="appeal-reason">Lý do kết luận</label>
          <textarea
            id="appeal-reason"
            className="w-full rounded-xl border border-brand-200 bg-paper p-3 text-sm text-ink outline-none focus:border-brand-500"
            rows={3}
            maxLength={REASON_MAX}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          <p className="text-xs text-ink-muted">{trimmed}/{REASON_MAX} · từ {REASON_MIN} ký tự, tác giả sẽ thấy lý do này.</p>
          {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-800">{error}</p>}
          <Button type="submit" disabled={busy || !decision || trimmed < REASON_MIN}>
            {busy && <LoaderCircle className="h-4 w-4 animate-spin" />}Xác nhận kết luận
          </Button>
        </form>
      ) : (
        <p className="rounded-xl bg-brand-50 p-3 text-ink-soft">
          Trạng thái: {appeal.appealStatus ? statusLabels[appeal.appealStatus] : "—"}. Không còn bước nào cần Admin quyết định.
        </p>
      )}
    </div>
  )
}
