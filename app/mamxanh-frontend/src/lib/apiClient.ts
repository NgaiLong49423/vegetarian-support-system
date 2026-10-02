import axios, { isAxiosError } from 'axios';
import { clearSession, loadSession } from './authStorage';

/** Base URL of the Mâm Xanh REST API (docs/api/API.md). Set VITE_API_BASE_URL per environment. */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

/** Dispatched on `window` when the backend rejects the stored token; AuthProvider listens to it. */
export const SESSION_ENDED_EVENT = 'mamxanh:session-ended';
export type SessionEndReason = 'expired' | 'locked';

/**
 * Shared HTTP client. Authentication uses `Authorization: Bearer` (stateless JWT), so no cookies
 * are sent (`withCredentials` stays false).
 */
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json, application/problem+json' },
});

// Public /auth/* endpoints never get the token: a stale token there would be rejected with 401
// before the request reaches the endpoint.
const isPublicAuthRequest = (url?: string) => !!url && url.startsWith('/auth/');

apiClient.interceptors.request.use((config) => {
  const session = isPublicAuthRequest(config.url) ? null : loadSession();
  if (session) config.headers.set('Authorization', `Bearer ${session.accessToken}`);
  return config;
});

// API.md section 3.1: 401 means the token is no longer valid; 403 ACCOUNT_LOCKED means the
// account was locked after login. Both end the local session.
apiClient.interceptors.response.use(undefined, (error: unknown) => {
  if (isAxiosError(error) && error.config?.headers?.has('Authorization') && error.response) {
    const { status, data } = error.response;
    const code = (data as { code?: unknown } | undefined)?.code;
    const reason: SessionEndReason | null = status === 401 ? 'expired'
      : status === 403 && code === 'ACCOUNT_LOCKED' ? 'locked' : null;
    if (reason) {
      clearSession();
      window.dispatchEvent(new CustomEvent<SessionEndReason>(SESSION_ENDED_EVENT, { detail: reason }));
    }
  }
  return Promise.reject(error);
});
