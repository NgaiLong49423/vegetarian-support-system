import axios, { isAxiosError } from 'axios';
import { clearSession, loadSession } from './authStorage';
import { toProblem } from './problem';

/** Base URL of the Mâm Xanh REST API (docs/api/API.md). Set VITE_API_BASE_URL per environment. */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

/** Dispatched when Backend rejects the stored token; AuthProvider ends the local session. */
export const SESSION_ENDED_EVENT = 'mamxanh:session-ended';
export type SessionEndReason = 'expired' | 'locked';

export type AccessTokenProvider = () => string | null | undefined;
let accessTokenProvider: AccessTokenProvider | null = null;

/** Optional token injection boundary for callers that own token storage. */
export function setAccessTokenProvider(provider: AccessTokenProvider | null): void {
  accessTokenProvider = provider;
}

export interface ApiFieldError { field: string; message: string }

export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly code?: string, readonly errors: ApiFieldError[] = []) {
    super(message);
    this.name = 'ApiError';
  }
}

declare global {
  interface Window { __nutritionE2eAccessToken?: string }
}

function coverageTestToken() {
  return import.meta.env.VITE_COVERAGE === 'true' && typeof window !== 'undefined'
    ? window.__nutritionE2eAccessToken ?? null
    : null;
}

export function hasAccessToken() {
  return Boolean(accessTokenProvider?.() ?? loadSession()?.accessToken ?? coverageTestToken());
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json, application/problem+json' },
});

const isPublicAuthRequest = (url?: string) => !!url && url.startsWith('/auth/');

apiClient.interceptors.request.use((config) => {
  const token = isPublicAuthRequest(config.url)
    ? null
    : accessTokenProvider?.() ?? loadSession()?.accessToken ?? (config.url?.startsWith('/nutrition/') ? coverageTestToken() : null);
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  else config.headers.delete('Authorization');
  return config;
});

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

export function asApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  const problem = toProblem(error);
  if (problem) return new ApiError(problem.detail || 'Yêu cầu không thành công. Vui lòng thử lại.', problem.status, problem.code, problem.errors ?? []);
  return new ApiError('Không thể kết nối dịch vụ. Vui lòng thử lại.', axios.isAxiosError(error) ? error.response?.status ?? 0 : 0);
}
