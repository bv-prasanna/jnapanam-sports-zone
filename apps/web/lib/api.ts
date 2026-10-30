export const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080").replace(/\/$/, "");

export type TokenResponse = { tokenType: string; accessToken: string; accessTokenExpiresAt: string; refreshToken: string };
export type User = { id: string; fullName: string; displayName?: string; profilePhotoUrl?: string; email: string; mobile?: string; addressLine?: string; city?: string; state?: string; postalCode?: string; status: string; roles: string[]; requiresUpfrontPayment: boolean };

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


// Coordinates are best-effort and consent based; never block booking/payment if denied.
async function attachEventLocation(headers: Headers, method: string) {
  if (!["POST","PUT","PATCH","DELETE"].includes(method.toUpperCase()) || typeof navigator === "undefined") return;
  if (!navigator.geolocation || !window.isSecureContext) { headers.set("X-Location-Status","UNAVAILABLE"); return; }
  try {
    const permission = await navigator.permissions?.query({name:"geolocation" as PermissionName});
    if (!permission || permission.state !== "granted") {
      headers.set("X-Location-Status",permission?.state === "denied"?"DENIED":"NOT_REQUESTED");
      return;
    }
    const point = await new Promise<GeolocationPosition>((resolve,reject)=>
      navigator.geolocation.getCurrentPosition(resolve,reject,{enableHighAccuracy:false,timeout:2500,maximumAge:60000}));
    headers.set("X-Client-Latitude",point.coords.latitude.toFixed(7));
    headers.set("X-Client-Longitude",point.coords.longitude.toFixed(7));
    headers.set("X-Location-Status","CAPTURED");
  } catch { headers.set("X-Location-Status","UNAVAILABLE"); }
}

export async function api<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  const multipart = typeof FormData !== "undefined" && init.body instanceof FormData;
  if (!headers.has("Content-Type") && init.body && !multipart) headers.set("Content-Type", "application/json");
  // Public auth endpoints must never send stale JWTs. Spring's bearer filter
  // validates a supplied token even when the endpoint itself is permitAll().
  const publicAuthPath = ["/api/v1/auth/login", "/api/v1/auth/register"].includes(path)
    || path.startsWith("/api/v1/auth/otp/") || path.startsWith("/api/v1/auth/password/");
  if (publicAuthPath) {
    headers.delete("Authorization");
  } else if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  await attachEventLocation(headers, init.method || "GET");
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

export async function requestEmailRegistrationOtp(email:string){return api<void>("/api/v1/auth/otp/registration/email/request",{method:"POST",body:JSON.stringify({email})},false);}
export async function registerWithEmailOtp(payload:{fullName:string;email:string;mobile:string;password:string;otp:string}){
 const tokens=await api<TokenResponse>("/api/v1/auth/otp/registration/email/verify",{method:"POST",body:JSON.stringify(payload)},false);
 saveTokens(tokens);return api<User>("/api/v1/profile/me");
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


export async function apiDocument(path:string):Promise<Blob>{
  const request=async(token:string|null)=>fetch(`${API_BASE_URL}${path}`,{headers:token?{Authorization:`Bearer ${token}`}:{}});
  let response=await request(accessToken);
  if(response.status===401){const token=await refreshAccess();if(token)response=await request(token);}
  if(!response.ok)throw new ApiError(response.status,"Unable to retrieve verification document");
  return response.blob();
}
