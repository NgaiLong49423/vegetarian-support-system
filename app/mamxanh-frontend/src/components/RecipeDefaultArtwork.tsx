import { Leaf } from "lucide-react"

export type VegetarianCode = "VEGAN" | "LACTO" | "OVO" | "LACTO_OVO"

const defaultStyles: Record<
  VegetarianCode,
  { className: string; fallbackLabel: string }
> = {
  VEGAN: {
    className: "from-leaf-100 to-emerald-50 text-leaf-800",
    fallbackLabel: "Thuần chay",
  },
  LACTO: {
    className: "from-amber-100 to-orange-50 text-amber-900",
    fallbackLabel: "Có sữa",
  },
  OVO: {
    className: "from-yellow-100 to-lime-50 text-lime-900",
    fallbackLabel: "Có trứng",
  },
  LACTO_OVO: {
    className: "from-brand-100 to-yellow-50 text-brand-800",
    fallbackLabel: "Có trứng và sữa",
  },
}

export function RecipeDefaultArtwork({
  vegetarianType,
  label,
  compact = false,
}: {
  vegetarianType: VegetarianCode
  label?: string
  compact?: boolean
}) {
  const style = defaultStyles[vegetarianType]
  const resolvedLabel =
    label && label !== vegetarianType ? label : style.fallbackLabel
  return (
    <div
      role="img"
      aria-label={`Ảnh mặc định cho món ${resolvedLabel}`}
      data-testid={`recipe-default-artwork-${vegetarianType}`}
      className={`flex h-full w-full flex-col items-center justify-center bg-gradient-to-br px-3 text-center ${style.className}`}
    >
      <Leaf className={compact ? "h-8 w-8" : "h-14 w-14"} />
      <span
        className={compact ? "mt-2 text-xs font-bold" : "mt-3 text-sm font-bold"}
      >
        Ảnh mặc định · {resolvedLabel}
      </span>
      <span className="sr-only">Chưa có ảnh công thức</span>
    </div>
  )
}
