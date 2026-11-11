import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
const source=p=>readFile(new URL("../"+p,import.meta.url),"utf8");

test("web transport safely classifies auth, permission, missing data, conflict, limits and server failures",async()=>{
 const api=await source("lib/api.ts");
 for(const status of [401,403,404,408,409,422,429,426])
   assert.match(api,new RegExp("status==="+status));
 assert.match(api,/status>=500/);
 assert.match(api,/Reference: /);
 assert.match(api,/fetchSafe/);
 assert.match(api,/MFA code sent/);
 assert.match(api,/apiResponseError\(response,path\)/);
});
test("admin error screen and navigation use dedicated authenticated API",async()=>{
 const page=await source("app/admin/errors/page.tsx");
 const nav=await source("app/admin/admin-navigation.ts");
 assert.match(page,/api\/v1\/admin\/errors/);
 assert.match(page,/SUPER_ADMIN/);
 assert.match(page,/correlationId/);
 assert.match(nav,/href:"\/admin\/errors"/);
});
