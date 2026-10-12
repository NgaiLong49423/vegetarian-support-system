const COOKIE_NAME = 'mamxanh.recipe-viewer';
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** First-party anonymous identity; never contains an account, token, or IP address. */
export function anonymousRecipeViewer(): string {
  const existing = document.cookie.split(';').map((part) => part.trim())
    .find((part) => part.startsWith(`${COOKIE_NAME}=`))?.slice(COOKIE_NAME.length + 1);
  if (existing && UUID.test(existing)) return existing;
  const viewer = crypto.randomUUID();
  document.cookie = `${COOKIE_NAME}=${viewer}; Path=/; SameSite=Lax${location.protocol === 'https:' ? '; Secure' : ''}`;
  if (!document.cookie.split(';').some((part) => part.trim() === `${COOKIE_NAME}=${viewer}`)) {
    throw new Error('Không thể giữ định danh lượt xem. Vui lòng cho phép cookie của trang và thử lại.');
  }
  return viewer;
}
