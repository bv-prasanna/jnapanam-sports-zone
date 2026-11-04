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

test("expired web sessions clear the protected admin navigation and redirect to login",()=>{
 const provider=readFileSync(new URL("../components/app-provider.tsx",import.meta.url),"utf8");
 const login=readFileSync(new URL("../app/login/page.tsx",import.meta.url),"utf8");
 const theme=readFileSync(new URL("../app/admin/admin-foundation.css",import.meta.url),"utf8");
 assert.match(api,/const SESSION_EXPIRED_EVENT = "jnapanam:session-expired"/);
 assert.match(api,/window\.dispatchEvent\(new Event\(SESSION_EXPIRED_EVENT\)\)/);
 assert.match(api,/response\.status === 401 && !publicAuthPath/);
 assert.match(api,/expireSession\(\);\s*throw new ApiError\(401, "Session expired/);
 assert.match(provider,/window\.addEventListener\("jnapanam:session-expired",onExpired\)/);
 assert.match(provider,/setUser\(null\);setReady\(true\)/);
 assert.match(provider,/router\.replace\("\/login\?reason=session-expired"\)/);
 assert.match(login,/Your session has expired\. Please sign in again\./);
 assert.match(theme,/--admin-primary:var\(--olive/);
 assert.match(theme,/--admin-accent:var\(--saffron/);
 assert.match(theme,/max-height:calc\(100dvh - 40px\);overflow-y:auto/);
});
test("invalid password and deliberate logout are not interpreted as timeout",()=>{
 assert.match(api,/\["\/api\/v1\/auth\/login", "\/api\/v1\/auth\/register", "\/api\/v1\/auth\/logout"\]/);
 assert.match(api,/if \(response\.status === 401 && !publicAuthPath/);
});
