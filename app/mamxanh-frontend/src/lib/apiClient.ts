import axios from 'axios';

/** Base URL of the Mâm Xanh REST API (docs/api/API.md). Set VITE_API_BASE_URL per environment. */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

export type AccessTokenProvider = () => string | null | undefined;

let accessTokenProvider: AccessTokenProvider | null = null;

/** The login flow supplies a short-lived access token without coupling API calls to token storage. */
export function setAccessTokenProvider(provider: AccessTokenProvider | null): void {
  accessTokenProvider = provider;
}

/**
 * Shared HTTP client. Authentication uses Authorization: Bearer (stateless JWT), so no cookies
 * are sent (withCredentials stays false).
 */
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json, application/problem+json' },
});

apiClient.interceptors.request.use((config) => {
  const token = accessTokenProvider?.();
  if (token) config.headers.Authorization = 'Bearer ' + token;
  else delete config.headers.Authorization;
  return config;
});