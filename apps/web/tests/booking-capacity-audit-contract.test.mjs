import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
const source=p=>readFile(new URL("../"+p,import.meta.url),"utf8");

test("admin booking capacity audit is read-only and not restricted by sport filter",async()=>{
 const page=await source("app/admin/bookings/page.tsx");
 const css=await source("app/admin/bookings/bookings.module.css");
 assert.match(page,/aria-label="Slot capacity audit"/);
 assert.match(page,/Saved sport:/);
 assert.match(page,/facilities\.map\(f=><option/);
 assert.match(page,/\/api\/v1\/admin\/bookings\/capacity-audit\?/);
 assert.match(page,/Active holds:/);
 assert.match(page,/Pending payment/);
 assert.match(page,/countersMismatch/);
 assert.match(page,/This is read-only/);
 assert.match(css,/\.auditTable\{[^}]*overflow-x:auto/);
});
