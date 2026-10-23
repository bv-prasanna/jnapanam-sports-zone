import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

// Guard against accidentally attaching stale JWTs to endpoints that are public.
// Spring Security authenticates *present* bearer tokens even for permitAll URLs.
const api = readFileSync(new URL("../lib/api.ts", import.meta.url), "utf8");
test("public login and registration OTP endpoints never receive cached bearer authorization", () => {
  assert.match(api, /publicAuthPath\s*=\s*\/\^/);
  assert.match(api, /headers\.delete\("Authorization"\)/);
  assert.match(api, /if \(response\.status === 401 && retry && !publicAuthPath/);
});
test("registration request disables automatic token refresh", () => {
  assert.match(api, /requestRegistrationOtp\(mobile:string\).*false\);/);
});
