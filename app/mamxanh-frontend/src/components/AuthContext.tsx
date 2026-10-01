import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { SESSION_ENDED_EVENT, type SessionEndReason } from '../lib/apiClient';
import { SESSION_STORAGE_KEY, clearSession, loadSession, saveSession, type StoredSession } from '../lib/authStorage';
import type { AccountSummary, AuthResponse } from '../services/authApi';

/** Shown on the login page after the app ends a session by itself. */
export type SessionNotice = SessionEndReason | 'timeout';

type AuthContextValue = {
  /** The account of a real, server-issued session; null for Guests and the demo preview. */
  account: AccountSummary | null;
  isAuthenticated: boolean;
  /** UI preview with mock data (Lan Anh/FREE); never a verified identity or server session. */
  demoActive: boolean;
  /** Member-only UI is visible for a real session or the demo preview. */
  memberView: boolean;
  signIn: (response: AuthResponse) => void;
  signOut: () => void;
  enterDemo: () => void;
  exitDemo: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(() => loadSession());
  const [demoActive, setDemoActive] = useState(false);
  const navigate = useNavigate();

  const endSession = useCallback((notice: SessionNotice) => {
    clearSession();
    setSession(null);
    navigate('/dang-nhap', { state: { sessionNotice: notice } });
  }, [navigate]);

  // The backend rejected the token (expired, invalid or account locked).
  useEffect(() => {
    const onSessionEnded = (event: Event) => endSession((event as CustomEvent<SessionEndReason>).detail);
    window.addEventListener(SESSION_ENDED_EVENT, onSessionEnded);
    return () => window.removeEventListener(SESSION_ENDED_EVENT, onSessionEnded);
  }, [endSession]);

  // Without refresh tokens the session ends exactly when the access token expires.
  useEffect(() => {
    if (!session) return;
    const timer = window.setTimeout(() => endSession('timeout'), Math.max(0, session.expiresAt - Date.now()));
    return () => window.clearTimeout(timer);
  }, [session, endSession]);

  // Keep tabs in sync: logging in or out in one tab applies to the others.
  useEffect(() => {
    const onStorage = (event: StorageEvent) => {
      if (event.key === SESSION_STORAGE_KEY || event.key === null) setSession(loadSession());
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    account: session?.account ?? null,
    isAuthenticated: !!session,
    demoActive,
    memberView: !!session || demoActive,
    signIn: (response) => {
      setDemoActive(false);
      setSession(saveSession(response));
    },
    signOut: () => {
      clearSession();
      setSession(null);
    },
    enterDemo: () => setDemoActive(true),
    exitDemo: () => setDemoActive(false),
  }), [session, demoActive]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
