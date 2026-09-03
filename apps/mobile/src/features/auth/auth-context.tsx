import { createContext, PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { api, setAccessToken, TokenResponse, User } from '@/lib/api';
import { getStoredValue, removeStoredValue, setStoredValue } from '@/lib/storage';

const REFRESH_TOKEN_KEY = 'jnapanam.refreshToken';

type RegisterInput = { fullName: string; email: string; mobile?: string; password: string };
type AuthValue = {
  user: User | null;
  initializing: boolean;
  login(email: string, password: string): Promise<void>;
  register(input: RegisterInput): Promise<void>;
  logout(): Promise<void>;
};

const AuthContext = createContext<AuthValue | null>(null);

async function storeSession(tokens: TokenResponse) {
  setAccessToken(tokens.accessToken);
  await setStoredValue(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

async function loadMe() {
  return api<User>('/api/v1/profile/me');
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);

  const clearSession = useCallback(async () => {
    setAccessToken(null);
    setUser(null);
    await removeStoredValue(REFRESH_TOKEN_KEY);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const refreshToken = await getStoredValue(REFRESH_TOKEN_KEY);
        if (!refreshToken) return;
        const tokens = await api<TokenResponse>('/api/v1/auth/refresh', {
          method: 'POST',
          body: JSON.stringify({ refreshToken }),
        });
        await storeSession(tokens);
        setUser(await loadMe());
      } catch {
        await clearSession();
      } finally {
        setInitializing(false);
      }
    })();
  }, [clearSession]);

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await api<TokenResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: email.trim().toLowerCase(), password }),
    });
    await storeSession(tokens);
    setUser(await loadMe());
  }, []);

  const register = useCallback(async (input: RegisterInput) => {
    const tokens = await api<TokenResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        ...input,
        fullName: input.fullName.trim(),
        email: input.email.trim().toLowerCase(),
        mobile: input.mobile?.trim() || null,
      }),
    });
    await storeSession(tokens);
    setUser(await loadMe());
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = await getStoredValue(REFRESH_TOKEN_KEY);
    try {
      if (refreshToken) await api<void>('/api/v1/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      });
    } finally {
      await clearSession();
    }
  }, [clearSession]);

  const value = useMemo(() => ({ user, initializing, login, register, logout }), [user, initializing, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
