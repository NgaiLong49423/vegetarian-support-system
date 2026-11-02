import { ApiError } from './apiClient';

const COOKIE_NAME = 'mamxanh.guest-chat';
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** First-party session cookie forwarded explicitly; API cross-site cookies are unnecessary. */
export function guestChatIdentity(): string {
  const existing = document.cookie.split(';').map((part) => part.trim())
    .find((part) => part.startsWith(`${COOKIE_NAME}=`))?.slice(COOKIE_NAME.length + 1);
  if (existing && UUID.test(existing)) return existing;
  const id = crypto.randomUUID();
  document.cookie = `${COOKIE_NAME}=${id}; Path=/; SameSite=Lax${location.protocol === 'https:' ? '; Secure' : ''}`;
  if (!document.cookie.split(';').some((part) => part.trim() === `${COOKIE_NAME}=${id}`)) {
    throw new ApiError('Vui lòng cho phép cookie của trang để dùng thử Chatbot.', 400, 'GUEST_CHAT_COOKIE_REQUIRED');
  }
  return id;
}
