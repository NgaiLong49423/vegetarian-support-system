// Subscription timestamps are UTC in storage and explicitly zoned in the API.
// Treat legacy zone-less responses as UTC; never infer the browser's local zone.
function vietnamParts(value: string | null | undefined) {
  if (!value) return null;
  const date = new Date(/(?:Z|[+-]\d{2}:\d{2})$/i.test(value) ? value : `${value}Z`);
  if (Number.isNaN(date.getTime())) return null;
  return Object.fromEntries(new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh', day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).formatToParts(date).map(part => [part.type, part.value]));
}

export function formatSubscriptionDate(value: string | null | undefined) {
  const p = vietnamParts(value);
  return p ? `${p.day}/${p.month}/${p.year}` : '';
}

export function formatSubscriptionDateTime(value: string | null | undefined) {
  const p = vietnamParts(value);
  return p ? `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}` : '';
}
