import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');
test('booking page distinguishes payment failure from successful payment explicitly',async()=>{
 const page=await source('app/dashboard/page.tsx');
 const css=await source('app/globals.css');
 assert.match(page,/const \[messageKind,setMessageKind\]=useState<"error"\|"success"\|"info">/);
 assert.match(page,/setMessageKind\("error"\);setMessage\(warning\)/);
 assert.match(page,/setMessageKind\("success"\);setMessage\(result\)/);
 assert.match(page,/messageKind==="error"\?"error"/);
 assert.match(page,/Payment not confirmed for booking/);
 assert.match(css,/\.form-note\.error\{border:1px solid #e7a498!important;background:#fff0ed!important/);
 assert.ok(page.indexOf('function Bookings(')<page.indexOf('messageKind==="error"'),"Explicit status must be in booking component");
});
test('mobile and website default to today in venue timezone',async()=>{
 const page=await source('app/dashboard/page.tsx');
 assert.match(page,/function venueDate\(\)\{return new Date\(Date\.now\(\)\+330\*60_000\)/);
 assert.match(page,/\[date,setDate\]=useState\(\(\)=>venueDate\(\)\)/);
 assert.match(page,/min=\{venueDate\(\)\}/);
 assert.doesNotMatch(page,/Date\.now\(\)\+86400000/);
});

test('checkout verifies browser order id and payment fields, and handles failed attempts',async()=>{
 const page=await source('app/dashboard/page.tsx');
 assert.match(page,/result\.razorpay_order_id\|\|""/);
 assert.match(page,/order\.providerOrderId/);
 assert.match(page,/!result\.razorpay_payment_id\|\|!result\.razorpay_signature/);
 assert.match(page,/checkout\.on\?\.\("payment\.failed"/);
 assert.match(page,/Razorpay reported a failed payment attempt/);
 assert.match(page,/Do not pay again/);
});

test('generated payment receipt uses the current shared Jnapanam contact numbers',async()=>{
 const page=await source('app/dashboard/page.tsx');
 assert.match(page,/import \{CONTACT_PHONES\} from "@\/lib\/contact"/);
 assert.match(page,/CONTACT_PHONES\.map\(p=>p\.display\)/);
 assert.doesNotMatch(page,/82960 22107/);
});

test('booking history displays independent booking and payment status',async()=>{
 const page=await source('app/dashboard/page.tsx');
 const bookings=page.slice(page.indexOf('function Bookings('),page.indexOf('function feedbackClass('));
 assert.match(bookings,/<th>Booking status<\/th><th>Payment<\/th>/);
 assert.match(bookings,/prettyStatus\(String\(row\.paymentStatus\|\|""\)\)/);
 assert.match(bookings,/row\.paymentStatus==="UNPAID"/);
});
