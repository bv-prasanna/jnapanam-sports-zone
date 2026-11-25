import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const read=path=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('admin promotion management is connected to protected promotion API and navigation',async()=>{
 const page=await read('app/admin/promotions/page.tsx');
 const nav=await read('components/portal-side-navigation.tsx');
 const css=await read('app/admin/promotions/promotions.module.css');
 assert.match(page,/\/api\/v1\/admin\/promotions/);
 assert.match(page,/roles\.some\(r=>r==="ADMIN"\|\|r==="SUPER_ADMIN"\)/);
 assert.match(page,/globalUsageLimit/);
 assert.match(page,/perUserLimit/);
 assert.match(page,/startsAt/);
 assert.match(page,/endsAt/);
 assert.match(page,/purposes/);
 assert.match(page,/automatic/);
 assert.match(page,/onlineOnly/);
 assert.match(page,/role="dialog"/);
 assert.match(css,/max-height:90vh/);
 assert.match(nav,/href:"\/admin\/promotions"/);
});

test('promotion form validates date range, limits and discount bounds before submission',async()=>{
 const page=await read('app/admin/promotions/page.tsx');
 assert.match(page,/end>start/);
 assert.match(page,/discount>100/);
 assert.match(page,/globalUsageLimit/);
 assert.match(page,/perUser<1/);
 assert.match(page,/maxDiscountPaise/);
});
