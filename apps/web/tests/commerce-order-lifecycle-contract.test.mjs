import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const source=path=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('admin commerce exposes only valid next shop order states',async()=>{
 const page=await source('app/admin/commerce/page.tsx');
 assert.match(page,/function orderStatusOptions\(order:Order\):string\[\]/);
 assert.match(page,/if\(current==="PLACED"\)next=\["CONFIRMED"\]/);
 assert.match(page,/else if\(current==="CONFIRMED"\)next=\["PACKED"\]/);
 assert.match(page,/order\.fulfilment_type==="PICKUP"\)next=\["READY_FOR_PICKUP"\]/);
 assert.match(page,/else if\(order\.payment_status==="PAID"\)next=\["SHIPPED"\]/);
 assert.match(page,/\["PLACED","CONFIRMED"\]\.includes\(current\)&&order\.payment_status!=="PAID"\)next\.push\("CANCELLED"\)/);
 assert.match(page,/order\.payment_status==="PAID"\?\["DELIVERED"\]:\[\]/);
});

test('shop counter actions follow packing, readiness, payment and handover states',async()=>{
 const page=await source('app/shop-counter/page.tsx');
 assert.match(page,/advanceOrder\(o,"PACKED"/);
 assert.match(page,/advanceOrder\(o,"READY_FOR_PICKUP"/);
 assert.match(page,/advanceOrder\(o,"SHIPPED"/);
 assert.match(page,/o\.status==="READY_FOR_PICKUP"&&o\.payment_status==="PAID"/);
 assert.match(page,/o\.status==="SHIPPED"&&o\.payment_status==="PAID"/);
 assert.match(page,/!\["CANCELLED","DELIVERED"\]\.includes\(String\(o\.status\)\)/);
});
