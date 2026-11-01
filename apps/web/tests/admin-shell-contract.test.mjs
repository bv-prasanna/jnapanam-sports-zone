import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import {fileURLToPath} from "node:url";
import test from "node:test";
const root=fileURLToPath(new URL("../",import.meta.url));
async function source(name){return readFile(new URL("../"+name,import.meta.url),"utf8");}

test("admin has exactly one ownership point for the sidebar and top navigation",async()=>{
  const layout=await source("app/admin/layout.tsx");
  const dashboard=await source("app/admin/page.tsx");
  assert.match(layout,/aria-label="Admin modules"/);
  assert.match(layout,/id="jnapanam-admin-navigation"/);
  assert.match(layout,/admin-foundation\.css/);
  assert.match(layout,/Skip to main content/);
  assert.match(layout,/aria-expanded=\{menuOpen\}/);
  assert.doesNotMatch(dashboard,/<aside\b/,"legacy dashboard must not embed its own sidebar");
  assert.doesNotMatch(dashboard,/admin-shell/,"dashboard must not create an alternate admin shell");
});

test("shared sidebar contains every primary admin module exactly once",async()=>{
  const layout=await source("app/admin/layout.tsx");
  const routes=[
    "/admin/bookings","/admin/users","/admin/verification","/admin/commerce",
    "/admin/pricing","/admin/billing","/admin/operator-assignments",
    "/admin/release-readiness","/admin/subscriptions","/admin/notification-providers",
    "/admin/uat-otp","/admin?view=sports","/admin?view=facilities",
    "/admin?view=memberships","/admin?view=training","/admin?view=tournaments",
  ];
  for(const route of routes){
    const escaped=route.replace(/[.*+?^${}()|[\]\\]/g,"\\$&");
    const count=(layout.match(new RegExp('href:"'+escaped+'"',"g"))||[]).length;
    assert.equal(count,1,`nav route ${route} must be owned once by the shared shell`);
  }
});

test("admin design tokens and mobile sidebar are shared without styling public pages",async()=>{
  const styles=await source("app/admin/admin-foundation.css");
  const shell=await source("app/admin/admin-layout.module.css");
  const page=await source("app/admin/page.tsx");
  assert.match(styles,/\.adminExperience/);
  assert.match(styles,/--admin-primary/);
  assert.match(styles,/focus-visible/);
  assert.match(shell,/\.sidebarOpen/);
  assert.match(shell,/@media\(max-width:760px\)/);
  assert.match(page,/useSearchParams/);
  assert.match(page,/router\.push\(target==="dashboard"/);
});
