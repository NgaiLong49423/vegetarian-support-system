import type { MealPlanExport } from "../api/recipes"

const meals = ["BREAKFAST", "LUNCH", "DINNER"] as const
const labels = { BREAKFAST: "Bữa sáng", LUNCH: "Bữa trưa", DINNER: "Bữa tối" }
const number = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 })

function clean(value: string) {
  return value
    .normalize("NFC")
    .replace(/[\u0000-\u001f\u007f]/g, " ")
    .trim()
}

function days(week: string) {
  return Array.from({ length: 7 }, (_, offset) => {
    const date = new Date(`${week}T12:00:00`)
    date.setDate(date.getDate() + offset)
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
    return {
      key,
      label: date.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      }),
    }
  })
}

function entryText(entry: MealPlanExport["entries"][number]) {
  if (!entry.available) return "Công thức không còn khả dụng"
  const energy =
    entry.energyKcal === null
      ? "Chưa đủ dữ liệu dinh dưỡng"
      : `${number.format(entry.energyKcal)} kcal`
  return `${clean(entry.title)} · ${number.format(entry.plannedServings!)} khẩu phần · ${energy}`
}

export function mealPlanText(plan: MealPlanExport) {
  const lines = [
    "MÂM XANH — LỊCH ĂN TUẦN",
    `Tuần bắt đầu ${plan.weekStartDate}`,
    "Năng lượng ước tính theo khẩu phần đã lưu.",
    "",
  ]
  for (const day of days(plan.weekStartDate)) {
    lines.push(day.label)
    for (const meal of meals) {
      lines.push(`  ${labels[meal]}`)
      const entries = plan.entries.filter(
        (entry) => entry.mealDate === day.key && entry.mealType === meal,
      )
      lines.push(
        ...(entries.length
          ? entries.map((entry) => `    - ${entryText(entry)}`)
          : ["    Chưa có món"]),
      )
    }
    lines.push("")
  }
  return lines.join("\n")
}

export async function mealPlanFile(
  plan: MealPlanExport,
  format: "pdf" | "txt",
): Promise<Blob> {
  if (format === "txt")
    return new Blob([mealPlanText(plan)], { type: "text/plain;charset=utf-8" })
  const [{ jsPDF }, response] = await Promise.all([
    import("jspdf"),
    fetch(`${import.meta.env.BASE_URL}fonts/NotoSans-Regular.ttf`),
  ])
  if (!response.ok) throw new Error("Không tải được phông chữ PDF.")
  const bytes = new Uint8Array(await response.arrayBuffer())
  let binary = ""
  for (let i = 0; i < bytes.length; i += 8192)
    binary += String.fromCharCode(...bytes.subarray(i, i + 8192))
  const pdf = new jsPDF({ format: "a4", unit: "mm", compress: true })
  pdf.addFileToVFS("NotoSans-Regular.ttf", btoa(binary))
  pdf.addFont("NotoSans-Regular.ttf", "NotoSans", "normal")
  pdf.setFont("NotoSans")
  pdf.setProperties({
    title: `Lịch ăn tuần ${plan.weekStartDate}`,
    author: "Mâm Xanh",
  })
  let y = 20
  const ensure = (height: number) => {
    if (y + height > 277) {
      pdf.addPage("a4")
      y = 20
    }
  }
  const write = (text: string, size = 10, indent = 0) => {
    pdf.setFontSize(size)
    const lines: string[] = pdf.splitTextToSize(text, 178 - indent)
    for (const line of lines) {
      ensure(6)
      pdf.text(line, 16 + indent, y)
      y += 6
    }
  }
  write("MÂM XANH — LỊCH ĂN TUẦN", 16)
  write(`Tuần bắt đầu ${plan.weekStartDate}`)
  write("Năng lượng ước tính theo khẩu phần đã lưu.", 9)
  y += 4
  for (const day of days(plan.weekStartDate)) {
    ensure(24)
    pdf.setTextColor(35, 95, 63)
    write(day.label, 12)
    pdf.setTextColor(30, 40, 35)
    for (const meal of meals) {
      ensure(14)
      write(labels[meal], 10, 3)
      const entries = plan.entries.filter(
        (entry) => entry.mealDate === day.key && entry.mealType === meal,
      )
      for (const entry of entries) write(entryText(entry), 10, 7)
      if (!entries.length) write("Chưa có món", 9, 7)
      y += 2
    }
    y += 4
  }
  const count = pdf.getNumberOfPages()
  for (let page = 1; page <= count; page++) {
    pdf.setPage(page)
    pdf.setFontSize(9)
    pdf.setTextColor(90, 90, 90)
    pdf.text(`Mâm Xanh · ${page} / ${count}`, 194, 287, { align: "right" })
  }
  return pdf.output("blob")
}

export function downloadMealPlan(
  blob: Blob,
  week: string,
  format: "pdf" | "txt",
) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = `lich-an-${week}.${format}`
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
