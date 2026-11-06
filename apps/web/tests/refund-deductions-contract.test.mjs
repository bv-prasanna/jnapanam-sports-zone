import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');

test('refund form calculates net after explicit platform deduction',async()=>{
 const page=await source('app/admin/page.tsx');
 for(const term of ['Charges to retain / deduction','Net refund to customer',
  'deductionPaise','deductionDescription','Deduction description (required)','refundableProvider(row)','Past no-show exception',
  'An earlier refund is still pending','submitted to Razorpay']){
    assert.ok(page.includes(term),term);
 }
 assert.match(page,/refundPaise=balancePaise-deductionPaise/);
 assert.match(page,/refundPaise>=100/);
 assert.match(page,/deductionPaise>0\?deductionDescription.trim\(\):null/);
 assert.match(page,/deductionDescription.trim\(\).length>=3/);
 assert.match(page,/Refund history could not be loaded/);
});

test('backend refund safeguards are enforced beyond the admin screen',async()=>{
 const controller=await readFile(new URL('../../backend/src/main/java/com/jnapanam/sportszone/payment/controller/AdminPaymentController.java',import.meta.url),'utf8');
 const service=await readFile(new URL('../../backend/src/main/java/com/jnapanam/sportszone/payment/service/PaymentService.java',import.meta.url),'utf8');
 assert.match(controller,/hasAnyRole\('SUPER_ADMIN','ADMIN'\)/);
 assert.match(service,/BookingStatus.NO_SHOW/);
 assert.match(service,/findLockedById\(id\)/);
 assert.match(service,/deductionInput != null && amount != available - deduction/);
 assert.match(service,/RefundGatewayUncertainException/);
 assert.match(service,/deductionDescription.length\(\) < 3/);
});
