import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');
test('mobile default sport has a published facility when available',async()=>{
 const page=await source('src/app/bookings.tsx');
 assert.match(page,/availableFacilities\.some\(item => item\.sportCode === current\)/);
 assert.match(page,/availableFacilities\[0\]\?\.sportCode \|\| availableSports\[0\]\?\.code/);
 assert.match(page,/facility\.sportCode === sportCode/);
});
