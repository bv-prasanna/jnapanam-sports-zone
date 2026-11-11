import test from "node:test";
import assert from "node:assert/strict";
import {readFile,readdir} from "node:fs/promises";
const source=path=>readFile(new URL("../"+path,import.meta.url),"utf8");
const root=new URL("../",import.meta.url);
async function enumerate(path,ending){
 const files=await readdir(new URL(path,root),{withFileTypes:true});
 const out=[];
 for(const f of files){
  const current=path+f.name;
  if(f.isDirectory())out.push(...await enumerate(current+"/",ending));
  else if(current.endsWith(ending))out.push(current);
 }
 return out;
}
test("audit all route pages for out-of-brand legacy blue styling",async()=>{
 const pages=await enumerate("app/","/page.tsx");
 assert.ok(pages.length>=35,"Unexpected missing public/admin routes");
 for(const page of pages){
  const c=await source(page);
  assert.doesNotMatch(c,/#(?:063b63|0877b9|17354a|102a3a|f4f8fa|dce6eb)\b/i,page);
 }
});
test("all CSS modules and global theme avoid the obsolete navy blue identity",async()=>{
 const modules=await enumerate("app/",".module.css");
 modules.push(...await enumerate("components/",".module.css"));
 const cssPaths=["app/globals.css",...modules];
 assert.ok(cssPaths.length>=25,"Audit should cover the global stylesheet plus CSS modules");
 for(const file of cssPaths){
  const css=await source(file);
  assert.doesNotMatch(css,/#(?:063b63|0877b9|17354a|102a3a|f4f8fa|dce6eb)\b/i,file);
 }
});
test("account, operations and billing pages share left navigation with mobile toggle",async()=>{
 const nav=await source("components/portal-side-navigation.tsx");
 const sidebar=await source("components/portal-side-navigation.module.css");
 const shell=await source("components/portal-shell.module.css");
 for(const text of ['aria-controls="jnapanam-portal-sidebar"','aria-expanded={menuOpen}','styles.mobileToggle','styles.sidebarOpen','key:"front-desk"','key:"shop-counter"'])
  assert.ok(nav.includes(text),text);
 assert.ok(sidebar.includes("@media(max-width:760px)"));
 assert.ok(shell.includes("grid-template-columns:215px minmax(0,1fr)"));
 for(const path of ["app/profile/page.tsx","app/account-services/page.tsx","app/operations/page.tsx",
  "app/operations/front-desk/page.tsx","app/shop-counter/page.tsx","app/billing-profile/page.tsx","app/subscriptions/page.tsx"]){
   const page=await source(path);
   assert.ok(page.includes("<PortalSideNavigation"),path+" requires portal sidebar");
   assert.ok(page.includes("shell.shell"),path+" requires responsive shell");
 }
 const admin=await source("app/admin/layout.tsx");
 assert.ok(admin.includes('aria-label="Admin modules"'));
 assert.ok(admin.includes('id="jnapanam-admin-navigation"'));
});
test("forms, tables and document fields have proper width and overflow constraints",async()=>{
 const global=await source("app/globals.css");
 const profile=await source("app/profile/profile.module.css");
 const billing=await source("app/billing-profile/billing-profile.module.css");
 const ops=await source("app/operations/front-desk/front-desk.module.css");
 const receipt=await source("app/billing-documents/[id]/billing.module.css");
 assert.ok(global.includes('overflow-wrap:anywhere'));
 assert.ok(global.includes('overflow-x:auto'));
 assert.ok(profile.includes(".proofFields input[type=file]"));
 for(const sheet of [billing,ops,receipt]){
  assert.ok(sheet.includes("min-width:0"),"Missing responsive width");
  assert.ok(sheet.includes("overflow-wrap:anywhere"),"Missing long-text wrap");
 }
 assert.ok(ops.includes(".tableScroll"));
});
