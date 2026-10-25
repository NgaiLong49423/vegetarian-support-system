/**
 * Formats an ISO date string to DD/MM/YYYY (AC-13.7).
 */
export function formatDate(isoString: string | null | undefined): string {
  if (!isoString) return '';
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return '';
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

/**
 * Formats an ISO date string to DD/MM/YYYY HH:mm for transaction timestamps.
 */
export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) return '';
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return '';
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${day}/${month}/${year} ${hours}:${minutes}`;
}

/**
 * Formats a currency number in VNĐ (e.g. 49000 -> "49.000").
 */
export function formatVnd(amount: number): string {
  return new Intl.NumberFormat('vi-VN').format(amount);
}

export function localDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function mondayOf(date: Date) {
  const monday = new Date(date);
  monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));
  return localDateKey(monday);
}

export function shiftWeek(weekStart: string, offset: number) {
  const date = new Date(`${weekStart}T12:00:00`);
  date.setDate(date.getDate() + offset * 7);
  return localDateKey(date);
}

export function shiftDays(dateKey: string, days: number): string {
  const date = new Date(`${dateKey}T12:00:00`);
  date.setDate(date.getDate() + days);
  return localDateKey(date);
}

export function getWindowDays(startDateKey: string, count = 7): string[] {
  return Array.from({ length: count }, (_, i) => shiftDays(startDateKey, i));
}

export function isToday(dateKey: string): boolean {
  return dateKey === localDateKey(new Date());
}

export function isPastDate(dateKey: string): boolean {
  return dateKey < localDateKey(new Date());
}
