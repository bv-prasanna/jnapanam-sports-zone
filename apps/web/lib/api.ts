export const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080").replace(/\/$/, "");

export type TokenResponse = { tokenType: string; accessToken: string; accessTokenExpiresAt: string; refreshToken: string };
export type User = { id: string; fullName: string; displayName?: string; profilePhotoUrl?: string; email: string; mobile?: string; status: string; roles: string[]; requiresUpfrontPayment: boolean };

const REFRESH_KEY = "jnapanam.refresh-token";
let accessToken: string | null = null;
let refreshPromise: Promise<string | null> | null = null;

export class ApiError extends Error {
  constructor(public status: number, message: string, public details?: unknown) { super(message); }
}

function saveTokens(tokens: TokenResponse) {
  accessToken = tokens.accessToken;
  if (typeof window !== "undefined") localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
}

export function clearSession() {
  accessToken = null;
  if (typeof window !== "undefined") localStorage.removeItem(REFRESH_KEY);
}

async function refreshAccess(): Promise<string | null> {
  if (typeof window === "undefined") return null;
  const refreshToken = localStorage.getItem(REFRESH_KEY);
  if (!refreshToken) return null;
  if (!refreshPromise) refreshPromise = fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken }),
  }).then(async response => {
    if (!response.ok) { clearSession(); return null; }
    const tokens = await response.json() as TokenResponse; saveTokens(tokens); return tokens.accessToken;
  }).finally(() => { refreshPromise = null; });
  return refreshPromise;
}

export async function api<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  const multipart = typeof FormData !== "undefined" && init.body instanceof FormData;
  if (!headers.has("Content-Type") && init.body && !multipart) headers.set("Content-Type", "application/json");
  // Public auth endpoints must never send stale JWTs. Spring's bearer filter
  // validates a supplied token even when the endpoint itself is permitAll().
  const publicAuthPath = /^\\/api\\/v1\\/auth\\/(?:login|register|otp\\/|password\\/)/.test(path);
  if (publicAuthPath) {
    headers.delete("Authorization");
  } else if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  let response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  if (response.status === 401 && retry && !publicAuthPath && await refreshAccess()) {
    headers.set("Authorization", `Bearer ${accessToken}`); response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const fieldErrors=body?.errors&&typeof body.errors==="object"?Object.entries(body.errors).map(([field,value])=>`${field.replace(/([A-Z])/g," $1").replace(/^./,letter=>letter.toUpperCase())}: ${String(value)}`).join(" · "):"";
    throw new ApiError(response.status, fieldErrors || body?.detail || body?.message || body?.error || `Request failed (${response.status})`, body);
  }
  // OTP request endpoints reply 202 Accepted with an empty body. Do not parse it as JSON.
  if (response.status === 204 || response.headers.get("Content-Length") === "0") return undefined as T;
  const raw = await response.text();
  if (!raw.trim()) return undefined as T;
  return JSON.parse(raw) as T;
}

export async function login(email: string, password: string, mfaCode?: string) {
  const tokens = await api<TokenResponse>("/api/v1/auth/login", { method: "POST", body: JSON.stringify({ email, password, mfaCode }) }, false);
  saveTokens(tokens); return api<User>("/api/v1/profile/me");
}

export async function requestRegistrationOtp(mobile:string){return api<void>("/api/v1/auth/otp/registration/request",{method:"POST",body:JSON.stringify({mobile})},false);}
export async function register(payload: { fullName: string; email: string; mobile: string; password: string; otp:string }) {
  const tokens = await api<TokenResponse>("/api/v1/auth/otp/registration/verify", { method: "POST", body: JSON.stringify(payload) }, false);
  saveTokens(tokens); return api<User>("/api/v1/profile/me");
}

export async function restoreSession() { return (await refreshAccess()) ? api<User>("/api/v1/profile/me") : null; }
export async function logout() {
  const refreshToken = typeof window === "undefined" ? null : localStorage.getItem(REFRESH_KEY);
  if (refreshToken) await api<void>("/api/v1/auth/logout", { method: "POST", body: JSON.stringify({ refreshToken }) }).catch(() => undefined);
  clearSession();
}
