import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');

test('mobile booking shows verified payment confirmation after slot refresh',async()=>{
 const bookings=await source('src/app/bookings.tsx');
 const payments=await source('src/lib/payments.ts');
 assert.match(bookings,/await completePayment\('BOOKING', booking\.id\)/);
 assert.ok(bookings.indexOf('await Promise.all([load(),findSlots()])')<bookings.indexOf('setMessage(confirmation)'));
 assert.match(payments,/receipt\.status !== 'PAID'/);
});

test('venue check-in displays success, warning or error as actually returned',async()=>{
 const screen=await source('src/app/checkin.tsx');
 assert.match(screen,/const refreshed=await load\(false\)/);
 assert.match(screen,/Check-in confirmed for/);
 assert.match(screen,/booking list could not refresh/);
 assert.match(screen,/messageKind==='success'/);
 assert.match(screen,/messageKind==='error'/);
});

test('operator action confirmations keep pending refund distinct and translate to Kannada',async()=>{
 const ops=await source('src/app/operations.tsx');
 const en=await source('src/i18n/locales/en.ts');
 const kn=await source('src/i18n/locales/kn.ts');
 assert.match(ops,/setNotice\(message\+\(refreshed/);
 assert.match(ops,/mobileOperations\.refundSubmitted/);
 assert.match(ops,/mobileOperations\.paymentRecorded/);
 for(const key of ['checkinRecorded:','checkoutRecorded:','noShowRecorded:','cancelRecorded:','refundSubmitted:','paymentRecorded:','refreshWarning:']){
   assert.ok(en.includes(key),'English '+key);
   assert.ok(kn.includes(key),'Kannada '+key);
 }
});

test('shop pickup order remains unpaid and counter actions acknowledge only persisted state',async()=>{
 const shop=await source('src/app/shop.tsx');
 const counter=await source('src/app/shop-counter.tsx');
 const en=await source('src/i18n/locales/en.ts');
 const kn=await source('src/i18n/locales/kn.ts');
 assert.match(shop,/const placed=await api<Order>/);
 assert.match(shop,/orderRefreshWarning/);
 assert.match(shop,/cartTotalsWarning/);
 assert.match(counter,/saleRefreshWarning/);
 assert.match(counter,/orderStatusRecorded/);
 for(const key of ['orderSubmitted:','orderRefreshWarning:','saleRecorded:','orderStatusRecorded:'])
   assert.ok(en.includes(key)&&kn.includes(key),'Translated status '+key);
});
