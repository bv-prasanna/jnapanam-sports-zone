import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
const source=path=>readFile(new URL("../"+path,import.meta.url),"utf8");

test("profile follows unified olive saffron and ivory palette without legacy navy",async()=>{
 const css=await source("app/profile/profile.module.css");
 const shell=await source("components/portal-shell.module.css");
 const global=await source("app/globals.css");
 for(const token of ["--olive","--saffron","--ivory","--line"])assert.ok(css.includes("var("+token),token);
 assert.match(global,/--olive:#28330f;.*--saffron:#c83a0a;.*--ivory:#fff9ef/);
 assert.match(css,/\\.actions button\\{[^}]*background:var\\(--profile-saffron\\)/);
 assert.match(shell,/background:var\\(--ivory/);
 assert.doesNotMatch(css,/#063b63|#0877b9|#102a3a|#f4f8fa|#61727d|#dce6eb/i);
});
test("address proof has independent select and file styles and responsive wrap",async()=>{
 const page=await source("app/profile/page.tsx");
 const css=await source("app/profile/profile.module.css");
 assert.match(page,/className=\\{styles\\.proofPanel\\}/);
 assert.match(page,/className=\\{styles\\.proofFields\\}/);
 assert.match(page,/className=\\{styles\\.proofStatus\\}/);
 assert.match(page,/aria-label="Upload address proof \\(JPG, PNG or PDF\\)"/);
 assert.match(page,/maxLength=\\{10\\}/);
 assert.doesNotMatch(page,/Mobile \\(not verified\\)/);
 assert.match(css,/\\.form input:not\\(\\[type=file\\]\\),\\.form select/);
 assert.match(css,/\\.proofFields input\\[type=file\\]::file-selector-button/);
 assert.match(css,/@media\\(max-width:800px\\)[\\s\\S]*\\.proofFields\\{grid-template-columns:minmax\\(0,1fr\\)\\}/);
 assert.match(css,/overflow-wrap:anywhere/);
});
