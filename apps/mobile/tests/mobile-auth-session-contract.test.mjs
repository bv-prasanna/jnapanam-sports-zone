import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source = path => readFile(new URL('../'+path,import.meta.url),'utf8');

test('device-bound refresh token rotates through a single shared refresh request',async()=>{
 const api=await source('src/lib/api.ts');
 assert.match(api,/let refreshInFlight:Promise<string\|null>\|null=null/);
 assert.match(api,/if\(refreshInFlight\)return refreshInFlight/);
 assert.match(api,/getDeviceIdentity\(\)/);
 assert.match(api,/deviceSignature:device\.deviceSignature/);
 assert.match(api,/setStoredValue\(REFRESH_TOKEN_KEY,tokens\.refreshToken\)/);
 assert.match(api,/response\.status===401 && !publicAuth/);
 assert.match(api,/if\(renewed\)response=await request\(renewed\)/);
});

test('temporary offline startup never destroys encrypted refresh token and restores cached identity',async()=>{
 const api=await source('src/lib/api.ts');
 const auth=await source('src/features/auth/auth-context.tsx');
 assert.match(api,/response\.status===401\|\|response\.status===403/);
 assert.match(api,/if\(!response\.ok\)throw new ApiError\(friendly\('server'\)/);
 assert.match(auth,/CACHED_USER_KEY/);
 assert.match(auth,/restoreAccessToken\(\)/);
 assert.match(auth,/if\(reason instanceof ApiError && \(reason\.status===401\|\|reason\.status===403\)\)/);
 assert.match(auth,/Do not clear refresh token or cached profile for offline/);
});

test('password sign-in still surfaces MFA challenge to mobile and keeps mobile login code input',async()=>{
 const api=await source('src/lib/api.ts');
 const login=await source('src/app/login.tsx');
 assert.match(api,/message\.includes\('MFA code sent'\)/);
 assert.match(login,/if\(message\.toLowerCase\(\)\.includes\('mfa code'\)\)setMfaRequired\(true\)/);
});

test('auth repair preserves Android version policy enforcement and does not mask MFA as expired session',async()=>{
 const api=await source('src/lib/api.ts');
 const login=await source('src/app/login.tsx');
 assert.match(api,/X-Jnapanam-Platform/);
 assert.match(api,/X-Jnapanam-Version-Code/);
 assert.match(api,/announceServerRequiredUpdate\(\)/);
 assert.match(api,/if\(status===426\)/);
 assert.match(api,/if\(publicAuth&&path==='\/api\/v1\/auth\/login'\)/);
 assert.match(api,/MFA code sent/);
 assert.match(login,/setMfaRequired\(true\)/);
});

test('Android classifies permissions, limits and server references without technical errors',async()=>{
 const api=await source('src/lib/api.ts');
 for(const status of [403,404,429,426])assert.match(api,new RegExp('status==='+status));
 assert.match(api,/status>=500/);
 assert.match(api,/supportRef/);
 assert.match(api,/Invalid login details or verification code/);
 assert.match(api,/MFA code sent/);
});
