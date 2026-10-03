import axios from 'axios';
import { toProblem } from './problem';

/** Base URL of the Mâm Xanh REST API (docs/api/API.md). Set VITE_API_BASE_URL per environment. */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export interface ApiFieldError {
  field: string;
  message: string;
}

export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly code?: string, readonly errors: ApiFieldError[] = []) {
    super(message);
    this.name = 'ApiError';
  }
}

declare global {
  interface Window {
    /** Set only by Playwright on the instrumented coverage build; never used by production builds. */
    __nutritionE2eAccessToken?: string;
  }
}

function coverageTestToken() {
  return import.meta.env.VITE_COVERAGE === 'true' && typeof window !== 'undefined'
    ? window.__nutritionE2eAccessToken ?? null
    : null;
}

export function hasAccessToken() {
  return Boolean(coverageTestToken());
}

/**
 * Shared HTTP client. Authentication uses `Authorization: Bearer` (stateless JWT), so no cookies
 * are sent (`withCredentials` stays false). The Bearer interceptor is added with login (#6).
 */
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json, application/problem+json' },
});

apiClient.interceptors.request.use((config) => {
  const token = config.url?.startsWith('/nutrition/') ? coverageTestToken() : null;
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  return config;
});

export function asApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  const problem = toProblem(error);
  if (problem) {
    return new ApiError(problem.detail || 'Yêu cầu không thành công. Vui lòng thử lại.', problem.status, problem.code, problem.errors ?? []);
  }
  return new ApiError('Không thể kết nối dịch vụ. Vui lòng thử lại.', axios.isAxiosError(error) ? error.response?.status ?? 0 : 0);
}
