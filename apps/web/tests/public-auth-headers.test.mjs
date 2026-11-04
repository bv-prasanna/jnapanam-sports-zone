import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

// Guard against accidentally attaching stale JWTs to endpoints that are public.
// Spring Security authenticates *present* bearer tokens even for permitAll URLs.
const api = readFileSync(new URL("../lib/api.ts", import.meta.url), "utf8");
test("public login and registration OTP endpoints never receive cached bearer authorization", () => {
  assert.match(api, /const publicAuthPath = \[/);
  assert.match(api, /headers\.delete\("Authorization"\)/);
  assert.match(api, /if \(response\.status === 401 && retry && !publicAuthPath/);
});
test("registration request disables automatic token refresh", () => {
  assert.match(api, /requestRegistrationOtp\(mobile:string\).*false\);/);
});

test("web customer password sign-in explains and accepts email MFA code",()=>{
  const login=readFileSync(new URL("../app/login/page.tsx",import.meta.url),"utf8");
  assert.match(login,/6-digit code sent to your registered email address/);
  assert.match(login,/mfaRequired&&<label>Email verification code/);
  assert.match(api,/if \(response.status === 401 \|\| response.status === 403\) \{ expireSession\(\); return null; \}/);
});

test("a protected 401 after refresh invalidates the shared session, not just one page",()=>{
  const provider=readFileSync(new URL("../components/app-provider.tsx",import.meta.url),"utf8");
  const login=readFileSync(new URL("../app/login/page.tsx",import.meta.url),"utf8");
  assert.match(api,/const SESSION_EXPIRED_EVENT = "jnapanam:session-expired"/);
  assert.match(api,/window\.dispatchEvent\(new Event\(SESSION_EXPIRED_EVENT\)\)/);
  assert.match(api,/response\.status === 401 && !publicAuthPath/);
  assert.match(api,/expireSession\(\);\s*throw new ApiError\(401, "Session expired/);
  assert.match(provider,/window\.addEventListener\("jnapanam:session-expired",onExpired\)/);
  assert.match(provider,/setUser\(null\);setReady\(true\)/);
  assert.match(provider,/router\.replace\("\/login\?reason=session-expired"\)/);
  assert.match(login,/Your session has expired\. Please sign in again\./);
  assert.match(login,/new URLSearchParams\(window\.location\.search\)/);
});

test("ordinary logout and wrong-password login cannot trigger global expiry redirect",()=>{
  assert.match(api,/\["\/api\/v1\/auth\/login", "\/api\/v1\/auth\/register", "\/api\/v1\/auth\/logout"\]/);
  assert.match(api,/if \(response\.status === 401 && !publicAuthPath/);
});

test("admin controls inherit shared olive/saffron theme and accessible overflow",()=>{
 const foundation=readFileSync(new URL("../app/admin/admin-foundation.css",import.meta.url),"utf8");
 assert.match(foundation,/--admin-primary:var\(--olive/);
 assert.match(foundation,/--admin-accent:var\(--saffron/);
 assert.match(foundation,/--admin-field-height/);
 assert.match(foundation,/max-height:calc\(100dvh - 40px\);overflow-y:auto/);
});
