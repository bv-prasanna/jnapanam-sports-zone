import { createContext, PropsWithChildren, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import {
  api, ApiError, clearMobileSession, onSessionExpired, REFRESH_TOKEN_KEY,
  restoreAccessToken, setAccessToken, TokenResponse, User
} from '@/lib/api';
import { getStoredValue, removeStoredValue, setStoredValue } from '@/lib/storage';
import { getDeviceIdentity } from '@/lib/device-identity';

const CACHED_USER_KEY = 'jnapanam.cachedUser';

type RegisterInput = { fullName: string; email: string; mobile: string; password: string };
type AuthValue = {
  user: User | null;
  initializing: boolean;
  login(email: string, password: string, mfaCode?: string): Promise<void>;
  requestRegistrationOtp(input: RegisterInput): Promise<void>;
  verifyRegistrationOtp(input:RegisterInput,otp:string): Promise<void>;
  logout(): Promise<void>;
  refreshProfile(): Promise<void>;
  updateUser(user: User): void;
};

const AuthContext = createContext<AuthValue | null>(null);

async function storeSession(tokens: TokenResponse) {
  await setStoredValue(REFRESH_TOKEN_KEY, tokens.refreshToken);
  setAccessToken(tokens.accessToken);
}
async function loadMe() {
  return api<User>('/api/v1/profile/me');
}
async function cacheUser(profile:User) {
  await setStoredValue(CACHED_USER_KEY,JSON.stringify(profile));
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);

  const clearSession = useCallback(async () => {
    setUser(null);
    await clearMobileSession();
    await removeStoredValue(CACHED_USER_KEY);
  }, []);

  // Only confirmed invalid/revoked refresh tokens invalidate an installation.
  // A network outage must never erase credentials or force another OTP.
  useEffect(() => {
    onSessionExpired(() => {
      setUser(null);
      void removeStoredValue(CACHED_USER_KEY);
    });
    return () => onSessionExpired(null);
  }, []);

  useEffect(() => {
    let active=true;
    (async () => {
      try {
        const refreshToken = await getStoredValue(REFRESH_TOKEN_KEY);
        if (!refreshToken) return;
        const savedProfile=await getStoredValue(CACHED_USER_KEY);
        if(savedProfile && active) {
          try{setUser(JSON.parse(savedProfile) as User);}catch{/* Ignore damaged cache. */}
        }
        if(!(await restoreAccessToken())){
          if(active)await clearSession();
          return;
        }
        const profile=await loadMe();
        await cacheUser(profile);
        if(active)setUser(profile);
      } catch(reason) {
        if(reason instanceof ApiError && (reason.status===401||reason.status===403)) {
          if(active)await clearSession();
        }
        // Do not clear refresh token or cached profile for offline/5xx failures.
        // The next authenticated API request will retry the single-flight refresh.
      } finally {
        if(active)setInitializing(false);
      }
    })();
    return ()=>{active=false;};
  }, [clearSession]);

  const login = useCallback(async (email: string, password: string, mfaCode?: string) => {
    const device=await getDeviceIdentity();
    const tokens = await api<TokenResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: email.trim().toLowerCase(), password,mfaCode,...device }),
    });
    await storeSession(tokens);
    const profile=await loadMe();
    await cacheUser(profile);
    setUser(profile);
  }, []);

  const requestRegistrationOtp=useCallback(async(input:RegisterInput)=>{await api<void>('/api/v1/auth/otp/registration/email/request',{method:'POST',body:JSON.stringify({email:input.email.trim().toLowerCase()})});},[]);
  const verifyRegistrationOtp=useCallback(async(input:RegisterInput,otp:string)=>{const device=await getDeviceIdentity();const tokens=await api<TokenResponse>('/api/v1/auth/otp/registration/email/verify',{method:'POST',body:JSON.stringify({...input,fullName:input.fullName.trim(),email:input.email.trim().toLowerCase(),mobile:input.mobile.trim(),otp,...device})});await storeSession(tokens);const profile=await loadMe();await cacheUser(profile);setUser(profile);},[]);

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

  const refreshProfile=useCallback(async()=>{const updated=await loadMe();await cacheUser(updated);setUser(updated);},[]);
  const updateUser=useCallback((updated:User)=>{setUser(updated);void cacheUser(updated);},[]);
  const value = useMemo(() => ({ user, initializing, login, requestRegistrationOtp, verifyRegistrationOtp, logout, refreshProfile, updateUser }), [user, initializing, login, requestRegistrationOtp, verifyRegistrationOtp, logout, refreshProfile, updateUser]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
