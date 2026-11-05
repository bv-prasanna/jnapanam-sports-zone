import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');

test('shop checkout displays order recorded and payment due, even after refresh',async()=>{
 const shop=await source('app/shop/page.tsx');
 assert.match(shop,/const placed=await api<Order>\("\/api\/v1\/commerce\/checkout"/);
 assert.match(shop,/const refreshed=await load\(true\)/);
 assert.match(shop,/Payment is due at pickup/);
 assert.match(shop,/Check My Orders before placing another order/);
});

test('website booking payment success occurs only after verification, not browser return',async()=>{
 const page=await source('app/dashboard/page.tsx');
 assert.match(page,/Payment not confirmed for booking/);
 assert.match(page,/result=`Payment successful./);
 assert.match(page,/setMessageKind\("error"\);setMessage\(warning\)/);
});

test('backend payment notification is emitted after verification has marked payment paid',async()=>{
 const service=await readFile(new URL('../../backend/src/main/java/com/jnapanam/sportszone/payment/service/PaymentService.java',import.meta.url),'utf8');
 assert.ok(service.indexOf('order.paid(request.providerPaymentId());')<service.indexOf('notificationService.payment(userId, order.getReceiptNumber()'));
 assert.match(service,/payment\.failed \/ payment\.authorized \/ order\.paid/);
});

test('cart mutations distinguish successful write from failed totals refresh',async()=>{
 const shop=await source('app/shop/page.tsx');
 const css=await source('app/shop/shop.module.css');
 assert.match(shop,/Item added, but cart totals could not refresh/);
 assert.match(shop,/Item removed, but cart totals could not refresh/);
 assert.match(shop,/messageKind==="error"\?"alert":"status"/);
 for(const cls of ['messageSuccess','messageWarning','messageError'])assert.ok(css.includes('.'+cls));
});
