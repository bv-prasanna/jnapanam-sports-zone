import test from "node:test";
import assert from "node:assert/strict";
import {readFileSync} from "node:fs";
const png=new URL("../public/images/jnapanam-logo.png",import.meta.url);
test("uploaded Jnapanam PNG is a valid nonempty logo",()=>{
 const b=readFileSync(png);
 assert.ok(b.length>20_000);
 assert.deepEqual([...b.subarray(0,8)],[137,80,78,71,13,10,26,10]);
});
test("web header footer favicon and admin use the same uploaded logo",()=>{
 const css=readFileSync(new URL("../app/globals.css",import.meta.url),"utf8");
 const footer=readFileSync(new URL("../components/site-footer.tsx",import.meta.url),"utf8");
 const layout=readFileSync(new URL("../app/layout.tsx",import.meta.url),"utf8");
 const admin=readFileSync(new URL("../app/admin/layout.tsx",import.meta.url),"utf8");
 for(const source of [css,footer,layout,admin])assert.ok(source.includes("jnapanam-logo.png"));
});
