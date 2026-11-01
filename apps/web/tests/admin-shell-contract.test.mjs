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

test("admin staff routes reuse admin navigation without disrupting operator and shopkeeper views",async()=>{
  const operations=await source("app/operations/page.tsx");
  const shop=await source("app/shop-counter/page.tsx");
  for(const page of [operations,shop]){
    assert.match(page,/import AdminLayout from/);
    assert.match(page,/<AdminLayout>\{screen\}<\/AdminLayout>/);
  }
  assert.match(operations,/<PortalSideNavigation active="operations"\/>/);
  assert.match(shop,/return admin\?<AdminLayout>/);
});

test("every admin route uses the common layout without its own website header or second sidebar",async()=>{
  const pages=[
    "page.tsx","users/page.tsx","verification/page.tsx","bookings/page.tsx",
    "commerce/page.tsx","pricing/page.tsx","billing/page.tsx",
    "subscriptions/page.tsx","notification-providers/page.tsx",
    "operator-assignments/page.tsx","release-readiness/page.tsx",
    "uat-otp/page.tsx","player-growth/page.tsx"
  ];
  for(const page of pages){
    const contents=await source("app/admin/"+page);
    assert.doesNotMatch(contents,/import\s*\{\s*(?:SiteHeader|SiteFooter|PortalSideNavigation)/,
      page+" must not inject public or customer navigation into Admin");
    assert.doesNotMatch(contents,/<aside[^>]*className=/,page+" must not embed its own sidebar");
  }
});

test("all admin page-level CSS modules honor common geometry and typographic scale",async()=>{
  const modules=[
    "users/users.module.css","bookings/bookings.module.css","commerce/commerce.module.css",
    "pricing/pricing.module.css","release-readiness/release-readiness.module.css",
    "notification-providers/notification-providers.module.css",
    "operator-assignments/operator-assignments.module.css",
    "subscriptions/admin-subscriptions.module.css","uat-otp/uat-otp.module.css"
  ];
  for(const moduleName of modules){
    const css=await source("app/admin/"+moduleName);
    assert.match(css,/\.page\{width:100%;max-width:none;min-width:0;min-height:0/,
      moduleName+" should not establish a competing layout");
    assert.match(css,/var\(--admin-ink\)/,
      moduleName+" should use common text tokens");
  }
});
