import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
const source=path=>readFile(new URL("../"+path,import.meta.url),"utf8");

test("profile reuses olive, saffron, ivory, and shared focus tokens",async()=>{
 const css=await source("app/profile/profile.module.css");
 const shell=await source("components/portal-shell.module.css");
 for(const token of ["--olive","--saffron","--ivory","--line"])assert.ok(css.includes("var("+token),token);
 assert.ok(css.includes("background:var(--profile-saffron)"));
 assert.ok(shell.includes("background:var(--ivory"));
 for(const old of ["#063b63","#0877b9","#102a3a","#f4f8fa","#dce6eb"])
  assert.ok(!css.toLowerCase().includes(old),"legacy blue: "+old);
});
test("address proof controls have responsive grid, file input and wrapping",async()=>{
 const page=await source("app/profile/page.tsx");
 const css=await source("app/profile/profile.module.css");
 for(const label of ["styles.proofPanel","styles.proofFields","styles.proofStatus","Upload address proof (JPG, PNG or PDF)"])
  assert.ok(page.includes(label),label);
 assert.ok(!page.includes("Mobile (not verified)"));
 for(const selector of [".form input:not([type=file]),.form select",".proofFields input[type=file]::file-selector-button","@media(max-width:800px)",".proofFields{grid-template-columns:minmax(0,1fr)}","overflow-wrap:anywhere"])
  assert.ok(css.includes(selector),selector);
});
