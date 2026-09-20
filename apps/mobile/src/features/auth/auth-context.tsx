import { createContext, PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import { api, setAccessToken, TokenResponse, User } from '@/lib/api';
import { getStoredValue, removeStoredValue, setStoredValue } from '@/lib/storage';
import { getDeviceIdentity } from '@/lib/device-identity';

const REFRESH_TOKEN_KEY = 'jnapanam.refreshToken';

type RegisterInput = { fullName: string; email: string; mobile?: string; password: string };
type AuthValue = {
  user: User | null;
  initializing: boolean;
  login(email: string, password: string): Promise<void>;
  requestRegistrationOtp(input: RegisterInput): Promise<void>;
  verifyRegistrationOtp(mobile:string,otp:string): Promise<void>;
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
        const device=await getDeviceIdentity();
        const tokens = await api<TokenResponse>('/api/v1/auth/refresh', {
          method: 'POST',
          body: JSON.stringify({ refreshToken,deviceSignature:device.deviceSignature }),
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
    const device=await getDeviceIdentity();
    const tokens = await api<TokenResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: email.trim().toLowerCase(), password,...device }),
    });
    await storeSession(tokens);
    setUser(await loadMe());
  }, []);

  const requestRegistrationOtp=useCallback(async(input:RegisterInput)=>{await api<void>('/api/v1/auth/otp/registration/request',{method:'POST',body:JSON.stringify({...input,fullName:input.fullName.trim(),email:input.email.trim().toLowerCase(),mobile:input.mobile?.trim()||null})});},[]);
  const verifyRegistrationOtp=useCallback(async(mobile:string,otp:string)=>{const tokens=await api<TokenResponse>('/api/v1/auth/otp/registration/verify',{method:'POST',body:JSON.stringify({mobile,otp})});await storeSession(tokens);setUser(await loadMe());},[]);

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

  const value = useMemo(() => ({ user, initializing, login, requestRegistrationOtp, verifyRegistrationOtp, logout }), [user, initializing, login, requestRegistrationOtp, verifyRegistrationOtp, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
