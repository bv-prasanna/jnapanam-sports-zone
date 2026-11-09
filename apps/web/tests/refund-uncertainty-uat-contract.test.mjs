import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const source=path=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('web does not invite retry of a potentially submitted refund',async()=>{
  const api=await source('lib/api.ts');
  assert.match(api,/status===503 && body\?\.code==="REFUND_STATUS_UNCERTAIN"/);
  assert.match(api,/Check Razorpay and reconcile the payment before trying again/);
  assert.ok(api.indexOf('body?.code==="REFUND_STATUS_UNCERTAIN"')<api.indexOf('if(status>=500)'));
});

test('mobile distinguishes uncertain refunds and MFA mail delivery failures from generic errors',async()=>{
  const api=await readFile(new URL('../../mobile/src/lib/api.ts',import.meta.url),'utf8');
  assert.match(api,/problem\?\.code==='REFUND_STATUS_UNCERTAIN'/);
  assert.match(api,/Check Razorpay and reconcile the payment before trying again/);
  assert.match(api,/code==='MFA_DELIVERY_FAILED'/);
  assert.match(api,/const code=problem\?\.code;/);
  assert.ok(api.indexOf("problem?.code==='REFUND_STATUS_UNCERTAIN'")<api.indexOf("if(status>=500)"));
});

test('backend exposes stable refund uncertainty and failed mail delivery codes',async()=>{
  const handler=await readFile(new URL('../../backend/src/main/java/com/jnapanam/sportszone/common/exception/GlobalExceptionHandler.java',import.meta.url),'utf8');
  assert.match(handler,/setProperty\("code","REFUND_STATUS_UNCERTAIN"\)/);
  assert.match(handler,/setProperty\("code","MFA_DELIVERY_FAILED"\)/);
});
