import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { SESSION_ENDED_EVENT, type SessionEndReason } from '../lib/apiClient';
import { clearSession, loadSession, saveSession, updateStoredAccountProfile, updateStoredAccountRole, type StoredSession } from '../lib/authStorage';
import type { AccountSummary, AuthResponse } from '../services/authApi';

export type SessionNotice = SessionEndReason | 'timeout';
type AuthContextValue = {
  account: AccountSummary | null;
  isAuthenticated: boolean;
  signIn: (response: AuthResponse) => void;
  signOut: () => void;
  updateAccountRole: (role: AccountSummary['role']) => void;
  updateAccountProfile: (displayName: string, avatarUrl: string | null) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(() => loadSession());
  const navigate = useNavigate();
  const endSession = useCallback((notice: SessionNotice) => {
    clearSession();
    setSession(null);
    if (!window.location.pathname.startsWith('/dang-cong-thuc')) {
      navigate('/dang-nhap', { state: { sessionNotice: notice } });
    }
  }, [navigate]);

  useEffect(() => {
    const onSessionEnded = (event: Event) => endSession((event as CustomEvent<SessionEndReason>).detail);
    window.addEventListener(SESSION_ENDED_EVENT, onSessionEnded);
    return () => window.removeEventListener(SESSION_ENDED_EVENT, onSessionEnded);
  }, [endSession]);

  useEffect(() => {
    if (!session) return;
    const timer = window.setTimeout(() => endSession('timeout'), Math.max(0, session.expiresAt - Date.now()));
    return () => window.clearTimeout(timer);
  }, [session, endSession]);

  const updateAccountRole = useCallback((role: AccountSummary['role']) => {
    setSession((current) => current?.account.role === role ? current : updateStoredAccountRole(role));
  }, []);

  const updateAccountProfile = useCallback((displayName: string, avatarUrl: string | null) => {
    setSession((current) => current ? updateStoredAccountProfile(displayName, avatarUrl) : current);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    account: session?.account ?? null,
    isAuthenticated: !!session,
    signIn: (response) => setSession(saveSession(response)),
    signOut: () => { clearSession(); setSession(null); },
    updateAccountRole,
    updateAccountProfile,
  }), [session, updateAccountRole, updateAccountProfile]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
