export const API_BASE_URL = (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080").replace(/\/$/, "");

export type TokenResponse = { tokenType: string; accessToken: string; accessTokenExpiresAt: string; refreshToken: string };
export type User = { id: string; fullName: string; displayName?: string; profilePhotoUrl?: string; email: string; mobile?: string; addressLine?: string; city?: string; state?: string; postalCode?: string; status: string; roles: string[]; requiresUpfrontPayment: boolean };

const REFRESH_KEY = "jnapanam.refresh-token";
let accessToken: string | null = null;
let refreshPromise: Promise<string | null> | null = null;
const SESSION_EXPIRED_EVENT = "jnapanam:session-expired";
let sessionExpiryPublished = false;
/** End an invalid session once across all protected pages and file downloads. */
function expireSession() {
  if (sessionExpiryPublished) return;
  sessionExpiryPublished = true;
  clearSession();
  if (typeof window !== "undefined") window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}

export class ApiError extends Error {
  constructor(public status: number, message: string, public details?: unknown) { super(message); }
}

/** Consistent, safe messages for users on every API-backed web page. */
async function fetchSafe(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  try { return await fetch(input, init); }
  catch { throw new ApiError(0, "Cannot connect right now. Check your internet connection and try again."); }
}
function supportReference(response: Response) {
  const id=response.headers.get("X-Correlation-ID");
  return id && /^[A-Za-z0-9_-]{8,80}$/.test(id) ? " Reference: "+id : "";
}
function safeDetail(body: Record<string, unknown> | null): string | null {
  const value=body?.detail || body?.message;
  if(typeof value!=="string" || value.length>180 ||
      /(?:<|>|exception|stacktrace|java[.]|org[.]|sql|jdbc|password[=:]|token[=:]|traceback)/i.test(value)) return null;
  return value;
}
async function apiResponseError(response: Response,path:string):Promise<ApiError> {
  const status=response.status;
  const body=await response.json().catch(()=>null) as Record<string,unknown>|null;
  const detail=safeDetail(body);
  if(status===401 && path==="/api/v1/auth/login") {
    if(detail?.includes("MFA code sent"))
      return new ApiError(status,"MFA code sent to your registered email. Enter the 6-digit verification code.");
    return new ApiError(status,"Invalid login details or verification code. Please try again.");
  }
  if(status===401)return new ApiError(status,"Your session has expired. Please sign in again.");
  if(status===403)return new ApiError(status,"You do not have permission to perform this action.");
  if(status===404)return new ApiError(status,"The requested information could not be found. Please refresh and try again.");
  if(status===408||status===504)return new ApiError(status,"The request took too long. Please try again.");
  if(status===400||status===409||status===422)
    return new ApiError(status,detail||"Please check your details and try again.",body);
  if(status===429)return new ApiError(status,"Too many attempts. Please wait a moment and try again.");
  if(status===426)return new ApiError(status,"An app update is required to continue.");
  if(status>=500)return new ApiError(status,"The service is temporarily unavailable. Please try again."+supportReference(response));
  return new ApiError(status,detail||"Unable to complete this request. Please try again.");
}

function saveTokens(tokens: TokenResponse) {
  accessToken = tokens.accessToken;
  sessionExpiryPublished = false;
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
  if (!refreshPromise) refreshPromise = fetchSafe(`${API_BASE_URL}/api/v1/auth/refresh`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken }),
  }).then(async response => {
    if (response.status === 401 || response.status === 403) { expireSession(); return null; }
    if (!response.ok) throw new ApiError(response.status, "Sign-in service temporarily unavailable. Please retry.");
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
  const publicAuthPath = ["/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/logout"].includes(path)
    || path.startsWith("/api/v1/auth/otp/") || path.startsWith("/api/v1/auth/password/");
  if (publicAuthPath) {
    headers.delete("Authorization");
  } else if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  await attachEventLocation(headers, init.method || "GET");
  let response = await fetchSafe(`${API_BASE_URL}${path}`, { ...init, headers });
  if (response.status === 401 && retry && !publicAuthPath) {
    const renewed = await refreshAccess();
    if (renewed) {
      headers.set("Authorization", `Bearer ${renewed}`);
      response = await fetchSafe(`${API_BASE_URL}${path}`, { ...init, headers });
    }
  }
  // A 403 is a permissions issue; only a 401 after attempted refresh expires login.
  if (response.status === 401 && !publicAuthPath && !path.startsWith("/api/v1/public/")) {
    expireSession();
    throw new ApiError(401, "Session expired. Please sign in again.");
  }
  if (!response.ok) throw await apiResponseError(response,path);
  // OTP request endpoints reply 202 Accepted with an empty body. Do not parse it as JSON.
  if (response.status === 204 || response.headers.get("Content-Length") === "0") return undefined as T;
  const raw = await response.text();
  if (!raw.trim()) return undefined as T;
  try { return JSON.parse(raw) as T; }
  catch { throw new ApiError(502,"The server returned an unexpected response."+supportReference(response)); }
}

/** Download authenticated binary files; standard api() expects JSON. */
export async function apiBlob(path:string):Promise<Blob>{
  let jwt=accessToken || await refreshAccess();
  const request=()=>fetchSafe(`${API_BASE_URL}${path}`,{headers:jwt?{Authorization:`Bearer ${jwt}`}:{}});
  let response=await request();
  if(response.status===401){jwt=await refreshAccess();if(jwt)response=await request();}
  if(response.status===401){expireSession();throw new ApiError(401,"Session expired. Please sign in again.");}
  if(!response.ok)throw await apiResponseError(response,path);
  return response.blob();
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
  const request=async(token:string|null)=>fetchSafe(`${API_BASE_URL}${path}`,{headers:token?{Authorization:`Bearer ${token}`}:{}});
  let response=await request(accessToken);
  if(response.status===401){const token=await refreshAccess();if(token)response=await request(token);}
  if(response.status===401){expireSession();throw new ApiError(401,"Session expired. Please sign in again.");}
  if(!response.ok)throw await apiResponseError(response,path);
  return response.blob();
}
