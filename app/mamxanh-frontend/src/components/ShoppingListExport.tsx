import { useEffect, useRef, useState } from "react";
import { ClipboardCopy, Download } from "lucide-react";
import { Button } from "./ui";
import { Modal } from "./Modal";
import {
  downloadShoppingListTxt,
  formatShoppingListText,
  type ShoppingExportItem,
} from "../lib/shoppingListExport";

export function ShoppingListExport({
  items,
  listName,
  disabled,
  onNotice,
  compact = false,
}: {
  items: ShoppingExportItem[];
  listName: string;
  disabled: boolean;
  onNotice: (message: string) => void;
  compact?: boolean;
}) {
  const [fallback, setFallback] = useState<string | null>(null);
  const [copying, setCopying] = useState(false);
  const pending = useRef(false);
  const alive = useRef(true);
  const trigger = useRef<HTMLElement | null>(null);
  const text = useRef<HTMLTextAreaElement>(null);
  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
    };
  }, []);
  useEffect(() => {
    if (fallback !== null) text.current?.focus();
  }, [fallback]);
  const blocked = disabled || !items.length || copying;
  function close() {
    setFallback(null);
    requestAnimationFrame(() => {
      if (trigger.current?.isConnected) trigger.current.focus();
    });
  }
  async function copy() {
    if (disabled || !items.length || pending.current) return;
    trigger.current = document.activeElement as HTMLElement;
    const content = formatShoppingListText(items, new Date(), listName);
    pending.current = true;
    setCopying(true);
    onNotice("");
    try {
      await navigator.clipboard.writeText(content);
      if (alive.current) onNotice("Đã sao chép danh sách vào khay nhớ tạm.");
    } catch {
      if (alive.current) setFallback(content);
    } finally {
      pending.current = false;
      if (alive.current) setCopying(false);
    }
  }
  return (
    <>
      <div className={compact ? "flex flex-col gap-1" : "flex gap-2"}>
        <Button
          variant={compact ? "ghost" : "outline"}
          size="sm"
          className={compact ? "justify-start" : undefined}
          onClick={() => void copy()}
          disabled={blocked}
          title={!items.length ? "Danh sách trống" : "Sao chép vào Clipboard"}
        >
          <ClipboardCopy className="h-4 w-4" />
          {copying ? "Đang sao chép…" : "Sao chép"}
        </Button>
        <Button
          variant={compact ? "ghost" : "outline"}
          size="sm"
          className={compact ? "justify-start" : undefined}
          disabled={blocked}
          title={!items.length ? "Danh sách trống" : "Xuất file .txt"}
          onClick={() => {
            if (blocked) return;
            try {
              downloadShoppingListTxt(items, new Date(), listName);
              onNotice("Đã xuất danh sách thành tệp .txt.");
            } catch {
              onNotice(
                "Chưa thể tạo tệp TXT. Vui lòng thử lại hoặc sao chép danh sách.",
              );
            }
          }}
        >
          <Download className="h-4 w-4" />
          Xuất TXT
        </Button>
      </div>
      <Modal
        open={fallback !== null}
        onClose={close}
        title="Sao chép danh sách mua sắm"
        trapFocus
      >
        <p className="mb-3 text-sm text-ink-muted">
          Trình duyệt không cho phép sao chép tự động. Chọn tất cả rồi dùng thao
          tác sao chép của thiết bị (Ctrl+C trên máy tính).
        </p>
        <textarea
          ref={text}
          aria-label="Nội dung danh sách mua sắm"
          readOnly
          value={fallback ?? ""}
          className="h-48 w-full rounded-xl border border-brand-200 bg-paper p-3 font-mono text-xs text-ink focus-visible:outline-2 focus-visible:outline-brand-400"
        />
        <div className="mt-4 flex justify-end gap-2">
          <Button
            variant="outline"
            onClick={() => {
              text.current?.focus();
              text.current?.select();
            }}
          >
            Chọn tất cả
          </Button>
          <Button onClick={close}>Đóng</Button>
        </div>
      </Modal>
    </>
  );
}
