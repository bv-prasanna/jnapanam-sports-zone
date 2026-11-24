import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=path=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('mobile billing displays refund amounts and retained deduction descriptions',async()=>{
 const screen=await source('src/app/billing.tsx');
 const en=await source('src/i18n/locales/en.ts');
 const kn=await source('src/i18n/locales/kn.ts');
 assert.match(screen,/refundHistory/);
 assert.match(screen,/deductionPaise/);
 assert.match(screen,/deductionDescription/);
 for(const locale of [en,kn]){
  for(const key of ['refundHistory:','refundToCustomer:','retainedCharges:','deductionDescription:'])
   assert.ok(locale.includes(key),key);
 }
});
