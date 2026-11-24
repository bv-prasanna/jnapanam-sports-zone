import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=path=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('billing receipt API exposes refund deductions and descriptions',async()=>{
 const service=await readFile(new URL('../../backend/src/main/java/com/jnapanam/sportszone/pricing/VenueBillingService.java',import.meta.url),'utf8');
 assert.match(service,/deduction_paise as "deductionPaise"/);
 assert.match(service,/deduction_description as "deductionDescription"/);
 assert.match(service,/status,processed_at as "processedAt"/);
 assert.doesNotMatch(service,/deduction_description as "deductionDescription",reason/);
});

test('customer billing receipt separates refunds from retained charges',async()=>{
 const page=await source('app/billing-documents/[id]/page.tsx');
 assert.match(page,/Processed refunds to customer/);
 assert.match(page,/Processed retained charges/);
 assert.match(page,/r\.deductionDescription/);
 assert.match(page,/Refund history/);
});

test('admin GST billing receipt includes retained deduction detail',async()=>{
 const page=await source('app/admin/billing/page.tsx');
 assert.match(page,/deductionPaise:number/);
 assert.match(page,/Retained:/);
 assert.match(page,/deductionDescription/);
});
