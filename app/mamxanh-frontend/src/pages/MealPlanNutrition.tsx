import { useEffect, useState } from "react"
import { Button } from "../components/ui"
import {
  recipesApi,
  type MealPlanWeek,
  type RecipeDetail,
} from "../api/recipes"

const nutrients = [
  { key: "ENERGY_KCAL", label: "Năng lượng", unit: "kcal" },
  { key: "PROTEIN_G", label: "Chất đạm", unit: "g" },
  { key: "CARBOHYDRATE_G", label: "Tinh bột", unit: "g" },
  { key: "TOTAL_FAT_G", label: "Chất béo", unit: "g" },
  { key: "FIBER_G", label: "Chất xơ", unit: "g" },
  { key: "CALCIUM_MG", label: "Canxi", unit: "mg" },
  { key: "IRON_MG", label: "Sắt", unit: "mg" },
  { key: "VITAMIN_B12_MCG", label: "Vitamin B12", unit: "mcg" },
  { key: "ZINC_MG", label: "Kẽm", unit: "mg" },
] as const

type Entry = MealPlanWeek["entries"][number]
type RecipeData = Record<number, RecipeDetail["nutrition"] | null>

/** Each scheduled dish contributes one serving for one person, regardless of cooking quantity. */
function sum(entries: Entry[], data: RecipeData, key: string) {
  if (!entries.length) return null
  let total = 0
  for (const entry of entries) {
    const nutrition = data[entry.recipeId]
    const value = nutrition?.perServing?.[key]
    if (
      entry.recipeDeleted ||
      entry.unavailableMessage ||
      !entry.recipeTitle ||
      !nutrition?.complete ||
      value == null ||
      !Number.isFinite(value)
    )
      return null
    total += value
  }
  return total
}

function format(value: number | null, unit: string) {
  return value === null
    ? "Chưa đủ dữ liệu"
    : `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(value)} ${unit}`
}

export function MealPlanNutrition({
  plan,
  initialDay,
  initialScope,
}: {
  plan: MealPlanWeek
  initialDay: string
  initialScope: "day" | "week"
}) {
  const [scope, setScope] = useState(initialScope)
  const [day, setDay] = useState(initialDay)
  const [data, setData] = useState<RecipeData>({})
  const [loading, setLoading] = useState(true)
  const [retry, setRetry] = useState(0)
  const [failed, setFailed] = useState<number[]>([])
  const entries =
    scope === "week"
      ? plan.entries
      : plan.entries.filter((entry) => entry.mealDate === day)
  // A stable unique ID key avoids repeated requests when the user switches the summary.
  const ids = [
    ...new Set(
      entries
        .filter(
          (entry) =>
            !entry.recipeDeleted &&
            !entry.unavailableMessage &&
            entry.recipeTitle,
        )
        .map((entry) => entry.recipeId),
    ),
  ]
    .sort((a, b) => a - b)
    .join(",")

  useEffect(() => {
    let active = true
    const recipeIds = ids ? ids.split(",").map(Number) : []
    setLoading(true)
    setFailed([])
    const result: RecipeData = {}
    const errors: number[] = []
    let index = 0
    async function worker() {
      while (active && index < recipeIds.length) {
        const id = recipeIds[index++]
        try {
          result[id] = (await recipesApi.getPublished(id)).nutrition
        } catch {
          result[id] = null
          errors.push(id)
        }
      }
    }
    void Promise.all(
      Array.from({ length: Math.min(4, recipeIds.length) }, worker),
    ).then(() => {
      if (active) {
        setData(result)
        setFailed(errors)
        setLoading(false)
      }
    })
    return () => {
      active = false
    }
  }, [ids, retry])

  const missing = entries.filter(
    (entry) =>
      entry.recipeDeleted ||
      entry.unavailableMessage ||
      !entry.recipeTitle ||
      !data[entry.recipeId]?.complete ||
      nutrients.some(
        ({ key }) => data[entry.recipeId]?.perServing?.[key] == null,
      ),
  )
  const days = Array.from({ length: 7 }, (_, offset) => {
    const date = new Date(`${plan.weekStartDate}T12:00:00`)
    date.setDate(date.getDate() + offset)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
    return {
      key,
      label: date.toLocaleDateString("vi-VN", {
        weekday: "short",
        day: "numeric",
        month: "numeric",
      }),
    }
  })

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2" aria-label="Phạm vi dinh dưỡng">
        <Button
          size="sm"
          variant={scope === "day" ? "secondary" : "ghost"}
          aria-pressed={scope === "day"}
          onClick={() => setScope("day")}
        >
          Theo ngày
        </Button>
        <Button
          size="sm"
          variant={scope === "week" ? "secondary" : "ghost"}
          aria-pressed={scope === "week"}
          onClick={() => setScope("week")}
        >
          Cả tuần
        </Button>
      </div>
      {scope === "day" ? (
        <label className="block text-sm text-ink-soft">
          Ngày xem
          <select
            value={day}
            onChange={(event) => setDay(event.target.value)}
            className="mt-2 block w-full rounded-lg border border-brand-200 bg-paper p-2 text-ink"
          >
            {days.map(({ key, label }) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </label>
      ) : (
        <p className="text-sm text-ink-muted">
          {days[0].label} – {days[6].label}
        </p>
      )}
      <div className="rounded-xl bg-raised p-3">
        <p className="text-sm font-semibold text-ink">
          Dinh dưỡng cho 1 người · mỗi món 1 phần ăn
        </p>
        <p className="mt-1 text-xs leading-5 text-ink-muted">
          Dinh dưỡng 1 phần = dinh dưỡng toàn công thức ÷ số phần ăn gốc của
          công thức. Số phần nấu cho cả nhà trong lịch không làm tăng số liệu
          của 1 người.
        </p>
        <p className="mt-2 text-xs leading-5 text-ink-muted">
          {scope === "day"
            ? `Tổng trong ngày: cộng 1 phần của từng món trong ${entries.length} món đã lên lịch.`
            : `Tổng trong tuần: cộng dinh dưỡng 1 người của các ngày đã lên lịch (${entries.length} món), không phải trung bình mỗi ngày.`}
        </p>
      </div>
      {loading ? (
        <p role="status" className="py-6 text-center text-sm text-ink-muted">
          Đang tính dinh dưỡng…
        </p>
      ) : !entries.length ? (
        <p className="rounded-xl bg-raised p-5 text-sm text-ink-muted">
          {scope === "day" ? "Ngày này" : "Tuần này"} chưa có món để tính dinh
          dưỡng.
        </p>
      ) : (
        <>
          {missing.length > 0 && (
            <div
              role="status"
              className="rounded-xl border border-brand-200 bg-raised p-3 text-sm text-ink-soft"
            >
              <p className="font-semibold">Tổng dinh dưỡng chưa đầy đủ</p>
              <p className="mt-1 text-xs leading-5">
                Món thiếu dữ liệu được ghi rõ bên dưới. Không dùng số 0 để thay
                cho dữ liệu chưa biết.
              </p>
            </div>
          )}
          {failed.length > 0 && (
            <div role="alert" className="text-sm text-ink-muted">
              <p>Chưa tải được dữ liệu của {failed.length} công thức.</p>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setRetry((value) => value + 1)}
              >
                Thử lại
              </Button>
            </div>
          )}
          <dl className="divide-y divide-brand-100 rounded-xl border border-brand-100 px-4">
            {nutrients.map(({ key, label, unit }) => (
              <div
                key={key}
                className="flex flex-wrap items-center justify-between gap-2 py-3"
              >
                <dt className="text-sm text-ink-soft">{label}</dt>
                <dd
                  className={`text-right text-sm font-semibold tabular-nums ${
                    key === "ENERGY_KCAL" ? "text-brand-600" : "text-ink"
                  }`}
                >
                  {format(sum(entries, data, key), unit)}
                </dd>
              </div>
            ))}
          </dl>
          {scope === "week" && (
            <section aria-label="Năng lượng theo từng ngày">
              <h4 className="mb-2 text-sm font-semibold text-ink">
                Năng lượng từng ngày · 1 người
              </h4>
              <dl className="divide-y divide-brand-100">
                {days.map(({ key, label }) => {
                  const items = plan.entries.filter(
                    (entry) => entry.mealDate === key,
                  )
                  return (
                    <div
                      key={key}
                      className="flex justify-between gap-3 py-2 text-xs"
                    >
                      <dt className="text-ink-muted">
                        {label} · {items.length} món
                      </dt>
                      <dd className="text-ink tabular-nums">
                        {items.length
                          ? format(sum(items, data, "ENERGY_KCAL"), "kcal")
                          : "Chưa lên lịch"}
                      </dd>
                    </div>
                  )
                })}
              </dl>
            </section>
          )}
          {missing.length > 0 && (
            <section aria-label="Món thiếu dữ liệu">
              <h4 className="mb-2 text-sm font-semibold text-ink">
                Món cần kiểm tra
              </h4>
              <ul className="space-y-2 text-xs leading-5 text-ink-muted">
                {missing.map((entry) => (
                  <li key={entry.entryId}>
                    <span className="font-semibold text-ink-soft">
                      {entry.recipeDeleted ||
                      entry.unavailableMessage ||
                      !entry.recipeTitle
                        ? "Công thức không còn khả dụng"
                        : entry.recipeTitle}
                    </span>{" "}
                    ·{" "}
                    {new Date(`${entry.mealDate}T12:00:00`).toLocaleDateString(
                      "vi-VN",
                    )}
                    <p>
                      {failed.includes(entry.recipeId)
                        ? "Chưa tải được dữ liệu."
                        : data[entry.recipeId]?.ingredientsMissingData?.length
                          ? `Thiếu dữ liệu: ${data[entry.recipeId]!.ingredientsMissingData.join(", ")}.`
                          : "Chưa đủ dữ liệu dinh dưỡng."}
                    </p>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}
      <p className="text-xs leading-5 text-ink-muted">
        Ước tính từ dữ liệu dinh dưỡng của công thức, không thay thế tư vấn y
        tế. Dữ liệu minh họa trong catalog demo có thể ảnh hưởng kết quả.
      </p>
    </div>
  )
}
