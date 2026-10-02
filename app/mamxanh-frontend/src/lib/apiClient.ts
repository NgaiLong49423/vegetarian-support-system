import axios from 'axios';

/** Base URL of the Mâm Xanh REST API (docs/api/API.md). Set VITE_API_BASE_URL per environment. */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1';

/**
 * Shared HTTP client. Authentication uses `Authorization: Bearer` (stateless JWT), so no cookies
 * are sent (`withCredentials` stays false). The Bearer interceptor is added with login (#6).
 */
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json, application/problem+json' },
});
