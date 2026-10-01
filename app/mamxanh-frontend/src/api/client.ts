export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly code?: string, readonly errors: ApiFieldError[] = []) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface ApiFieldError {
  field: string;
  message: string;
}

type AccessTokenProvider = () => string | null | undefined;
let accessTokenProvider: AccessTokenProvider = () => null;

declare global {
  interface Window {
    /** Set only by Playwright on the instrumented coverage build; never used by production builds. */
    __nutritionE2eAccessToken?: string;
  }
}

if (import.meta.env.VITE_COVERAGE === 'true' && typeof window !== 'undefined') {
  accessTokenProvider = () => window.__nutritionE2eAccessToken ?? null;
}

/** Auth integration point; the Auth slice can register its verified token provider when available. */
export function setAccessTokenProvider(provider: AccessTokenProvider) {
  accessTokenProvider = provider;
}

export function hasAccessToken() {
  return Boolean(accessTokenProvider());
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = accessTokenProvider();
  if (path.startsWith('/nutrition/') && !token) {
    throw new ApiError('Đăng nhập để truy cập hồ sơ dinh dưỡng cá nhân.', 401, 'AUTHENTICATION_REQUIRED');
  }
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; code?: string; errors?: ApiFieldError[] } | null;
    throw new ApiError(body?.detail || 'Không thể kết nối dịch vụ. Vui lòng thử lại.', response.status, body?.code, body?.errors);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
