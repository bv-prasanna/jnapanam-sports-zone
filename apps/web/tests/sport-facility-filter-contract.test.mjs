import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');
test('Admin booking sport filter uses stable catalogue codes and displays facility parent sport',async()=>{
 const page=await source('app/admin/bookings/page.tsx');
 assert.match(page,/value=\{s\.code\}>\{s\.name\}/);
 assert.match(page,/f\.sportCode\?\.toLowerCase\(\)===sport\.toLowerCase\(\)/);
 assert.match(page,/f\.sportName\|\|f\.sportCode\|\|"Unknown sport"/);
 assert.match(page,/p\.set\("sport",sport\)/);
});
test('Admin facility editor keeps selected sport in controlled state after async option load',async()=>{
 const page=await source('app/admin/page.tsx');
 assert.match(page,/const \[sportCode,setSportCode\]=useState\(String\(row\?\.sportCode\|\|""\)\)/);
 assert.match(page,/name="sportCode" value=\{sportCode\} onChange=\{e=>setSportCode\(e\.target\.value\)\}/);
 assert.match(page,/The selected sport determines where this facility appears in booking and reporting/);
});
