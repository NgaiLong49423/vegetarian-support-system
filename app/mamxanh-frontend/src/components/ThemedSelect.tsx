import { useEffect, useRef, useState } from "react"
import { Check, ChevronDown } from "lucide-react"

export interface ThemedSelectOption {
  value: string
  label: string
}

export interface ThemedSelectProps {
  id?: string
  name?: string
  value: string
  onChange: (event: { target: { value: string; name?: string } }) => void
  options: ThemedSelectOption[]
  "aria-label"?: string
  "aria-labelledby"?: string
  className?: string
  disabled?: boolean
}

export function ThemedSelect({
  value,
  onChange,
  options,
  className = "",
  disabled = false,
  "aria-label": ariaLabel,
  "aria-labelledby": ariaLabelledBy,
  id,
  name,
}: ThemedSelectProps) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const selectRef = useRef<HTMLSelectElement>(null)

  // Close when clicking outside or pressing Escape
  useEffect(() => {
    if (!open) return
    const handleClickOutside = (e: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setOpen(false)
      }
    }
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false)
    }
    document.addEventListener("mousedown", handleClickOutside)
    document.addEventListener("keydown", handleKeyDown)
    return () => {
      document.removeEventListener("mousedown", handleClickOutside)
      document.removeEventListener("keydown", handleKeyDown)
    }
  }, [open])

  const selectedOption =
    options.find((opt) => opt.value === value) ?? options[0]

  const handleSelect = (val: string) => {
    if (disabled) return
    onChange({ target: { value: val, name } })
    setOpen(false)
  }

  return (
    <div ref={containerRef} className="relative inline-block text-left">
      {/* 
        Native Select: Rendered in the DOM for automated tests (Playwright .selectOption()) 
        and accessibility. We intercept mousedown so a real user never sees the ugly OS popup.
      */}
      <select
        ref={selectRef}
        id={id}
        name={name}
        aria-label={ariaLabel}
        aria-labelledby={ariaLabelledBy}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e)}
        onMouseDown={(e) => {
          e.preventDefault()
          if (!disabled) setOpen((prev) => !prev)
        }}
        onKeyDown={(e) => {
          if (e.key === " " || e.key === "Enter" || e.key === "ArrowDown") {
            e.preventDefault()
            if (!disabled) setOpen((prev) => !prev)
          }
        }}
        className={`cursor-pointer appearance-none transition-all ${className}`}
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>

      {/* 
        Custom Dropdown Popup:
        - Absolute floating position with high z-index
        - Beautiful rounded-2xl with border-brand-200/90 and paper background
        - Top accent micro gradient line
        - Soft shadow-2xl with backdrop-blur
        - Smooth fade-in & scale animation
        - Clean hover and active state with Mâm Xanh brand orange, checkmark icon
      */}
      {open && (
        <div
          role="listbox"
          className="modal-panel-enter absolute left-0 z-50 mt-1.5 min-w-full w-max max-h-60 overflow-y-auto overflow-x-hidden rounded-2xl border border-brand-200/90 bg-paper p-1.5 shadow-2xl shadow-brand-950/25 ring-1 ring-brand-900/5 backdrop-blur-md"
        >
          <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-brand-500 via-leaf-400 to-amber-400" />
          <div className="space-y-0.5 pt-1">
            {options.map((opt) => {
              const isSelected = opt.value === value
              return (
                <button
                  key={opt.value}
                  type="button"
                  role="option"
                  aria-selected={isSelected}
                  onClick={() => handleSelect(opt.value)}
                  className={`group flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2 text-xs font-semibold transition-all ${
                    isSelected
                      ? "bg-brand-500/15 text-brand-600 dark:text-brand-400 font-bold"
                      : "text-ink-soft hover:bg-brand-50 hover:text-brand-700 dark:hover:bg-brand-500/10"
                  }`}
                >
                  <span className="truncate">{opt.label}</span>
                  {isSelected && (
                    <Check className="h-3.5 w-3.5 shrink-0 text-brand-600 dark:text-brand-400" />
                  )}
                </button>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
