export function scaleQuantity(quantity: string, originalServings: number, desiredServings: number): string {
  const match = quantity.trim().match(/^(\d+(?:[.,]\d+)?|\d+\/\d+)(\s+.+)$/);
  if (!match || originalServings <= 0) return quantity;

  const rawAmount = match[1];
  const amount = rawAmount.includes('/')
    ? rawAmount.split('/').map(Number).reduce((numerator, denominator) => numerator / denominator)
    : Number(rawAmount.replace(',', '.'));
  if (!Number.isFinite(amount)) return quantity;

  const scaled = amount / originalServings * desiredServings;
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(scaled)}${match[2]}`;
}
