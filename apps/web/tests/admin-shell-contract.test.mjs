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
  const registry=await source("app/admin/admin-navigation.ts");
  assert.match(layout,/adminModules\.filter/,"sidebar must use the shared module registry");
  const routes=[
    "/admin/bookings","/admin/users","/admin/verification","/admin/commerce",
    "/admin/pricing?tab=pricing-rules","/admin/pricing?tab=taxes",
    "/admin?view=promotions","/admin/billing","/admin/operator-assignments",
    "/admin/release-readiness","/admin/subscriptions","/admin/notification-providers",
    "/admin?view=sports","/admin?view=facilities",
    "/admin?view=memberships","/admin?view=training","/admin?view=tournaments",
  ];
  for(const route of routes){
    const escaped=route.replace(/[.*+?^${}()|[\]\\]/g,"\\$&");
    const count=(registry.match(new RegExp('href:"'+escaped+'"',"g"))||[]).length;
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
    "player-growth/page.tsx"
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
    "subscriptions/admin-subscriptions.module.css"
  ];
  for(const moduleName of modules){
    const css=await source("app/admin/"+moduleName);
    assert.match(css,/\.page\{width:100%;max-width:none;min-width:0;min-height:0/,
      moduleName+" should not establish a competing layout");
    assert.match(css,/var\(--admin-ink\)/,
      moduleName+" should use common text tokens");
  }
});

test("dashboard exposes direct working links to pricing, taxes, offers, billing and operator assignments",async()=>{
  const dashboard=await source("app/admin/page.tsx");
  const hub=await source("app/admin/admin-dashboard-links.tsx");
  const registry=await source("app/admin/admin-navigation.ts");
  const pricing=await source("app/admin/pricing/page.tsx");
  assert.match(dashboard,/view==="dashboard"&&<AdminDashboardLinks/,"dashboard hub must remain visible even when metrics API fails");
  assert.match(hub,/visibleAdminModules/);
  assert.match(hub,/href=\{module\.href\}/);
  for(const name of ["Dynamic Pricing","Tax Management","Offers & Coupons","Operator Assignments","GST Billing & Refunds"]){
    assert.ok(registry.includes('label:"'+name+'"'),name+" must be present in shared module registry");
  }
  assert.match(pricing,/requestedTab==="taxes"/,"tax nav must open the tax management tab");
  assert.match(pricing,/router\.replace\(`\/admin\/pricing\?tab=\$\{next\}`\)/);
});

test("admin theme follows the homepage palette, type and branded button states",async()=>{
 const home=await source("app/globals.css");
 const admin=await source("app/admin/admin-foundation.css");
 const shell=await source("app/admin/admin-layout.module.css");
 const hub=await source("app/admin/admin-dashboard-links.module.css");
 const features=await source("app/admin/features/page.tsx");
 assert.match(home,/--olive:#28330f;.*--saffron:#c83a0a;.*--ivory:#fff9ef/);
 assert.match(admin,/--admin-primary:var\(--olive/);
 assert.match(admin,/--admin-accent:var\(--saffron/);
 assert.match(admin,/--admin-cta:var\(--saffron/);
 assert.match(admin,/--admin-bg:var\(--ivory/);
 assert.match(shell,/--admin-primary:var\(--olive/);
 assert.match(shell,/\.navLink\.active\{background:var\(--admin-accent\)/);
 assert.match(admin,/Georgia/);
 assert.match(hub,/var\(--admin-accent\)/);
 assert.match(features,/className="admin-feature-card"/);
 for(const file of [admin,shell,hub]){
   assert.doesNotMatch(file,/#063b63|#0877b9|#17354a|#f4f8fa|#101d2a|#164b69|#52a8db/i);
 }
});

test("desktop admin and tablet customer sidebar remain visible; phone menus have labels",async()=>{
 const admin=await source("app/admin/layout.tsx");
 const css=await source("app/admin/admin-layout.module.css");
 const customer=await source("app/dashboard/page.tsx");
 const global=await source("app/globals.css");
 assert.match(admin,/\{menuOpen\?"✕ Close":"☰ Menu"\}/);
 assert.match(css,/@media\(min-width:761px\)/);
 assert.match(css,/\.sidebar\{display:block;position:sticky/);
 assert.match(css,/\.sidebar\.sidebarOpen\{visibility:visible/);
 assert.match(customer,/aria-controls="jnapanam-customer-nav"/);
 assert.match(customer,/portal-nav-toggle/);
 assert.match(global,/@media\(min-width:701px\) and \(max-width:900px\)/);
 assert.match(global,/\.portal-nav\.portal-nav-open\{display:flex\}/);
});

test("booking Pay Now remains clickable and displays an accessible terms message if unchecked",async()=>{
 const page=await source("app/dashboard/page.tsx");
 const css=await source("app/globals.css");
 assert.match(page,/setTermsError\(true\)/);
 assert.match(page,/Please accept the facility terms and conditions before continuing to payment/);
 assert.match(page,/document\.querySelector\("\.booking-terms"\)\?\.scrollIntoView/);
 assert.match(page,/termsError&&!termsAccepted&&<p className="booking-terms-error" role="alert"/);
 assert.doesNotMatch(page,/disabled=\{!s\.available\|\|!quotes\[s\.id\]\|\|!!processing\|\|\(!!facilities/);
 assert.match(css,/\.booking-terms-error/);
});
test("admin payment diagnostic view exposes Razorpay order and callback metadata",async()=>{
 const page=await source("app/admin/page.tsx");
 assert.match(page,/\/api\/v1\/admin\/payments\/webhook-events/);
 assert.match(page,/Razorpay gateway events/);
 assert.match(page,/Razorpay order ID/);
 assert.match(page,/Search payment orders/);
 assert.match(page,/Payment reconciliation required/);
});

test("numeric KPI cards precede module links with active users and pending payments",async()=>{
 const page=await source("app/admin/page.tsx");
 assert.ok(page.indexOf('<AdminData view={view}')<page.indexOf('view==="dashboard"&&<AdminDashboardLinks'));
 assert.match(page,/title:"Active users"/);
 assert.match(page,/title:"Bookings"/);
 assert.match(page,/title:"Active sports"/);
 assert.match(page,/title:"Pending payment orders"/);
 assert.match(page,/api\/v1\/admin\/reconciliation/);
});
test("payment-error message remains visible after booking or slot refresh",async()=>{
 const page=await source("app/dashboard/page.tsx");
 assert.match(page,/let pendingBookingNumber=""/);
 assert.match(page,/Payment not confirmed for booking/);
 assert.match(page,/Do not pay twice if money was debited/);
 assert.match(page,/setMessage\(warning\)/);
 assert.match(page,/finally\{setMessageKind\("success"\);setMessage\(result\);?\}/);
});

test("production admin does not expose a fixed-OTP management screen",async()=>{
 const registry=await source("app/admin/admin-navigation.ts");
 const old=await source("app/admin/uat-otp/page.tsx");
 assert.doesNotMatch(registry,/href:"\/admin\/uat-otp"/);
 assert.match(old,/notFound\(\)/);
});

test("booking Explorer shows all dates by default; active operations exclude expired reservations",async()=>{
 const explorer=await source("app/admin/bookings/page.tsx");
 const board=await source("components/operator-facility-board.tsx");
 assert.match(explorer,/useState<RangePreset>\("ALL"\)/);
 assert.match(explorer,/x==="ALL"\?"All dates"/);
 assert.match(board,/\["CANCELLED","NO_SHOW","PAYMENT_EXPIRED"\]/);
 assert.match(board,/>Active bookings<\/span>/);
});

test("long booking details scroll and features have explicit enable and disable controls",async()=>{
 const css=await source("app/globals.css");
 const features=await source("app/admin/features/page.tsx");
 assert.match(css,/\.portal-dialog\{max-height:calc\(100dvh - 2\.5rem\);overflow-y:auto/);
 assert.match(features,/Disable feature/);
 assert.match(features,/Enable feature/);
});
