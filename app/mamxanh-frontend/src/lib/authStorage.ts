import type { AccountSummary, AuthResponse } from '../services/authApi';

/** The signed-in session kept on this device. Never log `accessToken`. */
export type StoredSession = {
  accessToken: string;
  /** Epoch milliseconds after which the access token is no longer accepted. */
  expiresAt: number;
  account: AccountSummary;
};

const STORAGE_KEY = 'mamxanh.auth';

/**
 * Single place that decides where the access token lives (API.md section 6, decision pending as
 * Q33). localStorage keeps the session across reloads and tabs; switching storage only changes
 * this function.
 */
function storage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function isStoredSession(value: unknown): value is StoredSession {
  const session = value as Partial<StoredSession> | null;
  return !!session
    && typeof session.accessToken === 'string' && session.accessToken.length > 0
    && typeof session.expiresAt === 'number'
    && typeof session.account?.id === 'number'
    && typeof session.account.displayName === 'string';
}

/** Returns the stored session, dropping it when it is malformed or already expired. */
export function loadSession(now = Date.now()): StoredSession | null {
  const raw = storage()?.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const session: unknown = JSON.parse(raw);
    if (isStoredSession(session) && session.expiresAt > now) return session;
  } catch {
    // Corrupted value: treat as signed out.
  }
  clearSession();
  return null;
}

export function saveSession(response: AuthResponse, now = Date.now()): StoredSession {
  const session: StoredSession = {
    accessToken: response.accessToken,
    expiresAt: now + response.expiresInSeconds * 1000,
    account: response.account,
  };
  storage()?.setItem(STORAGE_KEY, JSON.stringify(session));
  return session;
}

/** Client-side logout (AC-03.13): the backend keeps no session, so removing the token is enough. */
export function clearSession(): void {
  storage()?.removeItem(STORAGE_KEY);
}

export const SESSION_STORAGE_KEY = STORAGE_KEY;
