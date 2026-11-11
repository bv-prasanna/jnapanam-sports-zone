import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');
test('booking verifies current address proof and displays correct status',async()=>{
 const page=await source('src/app/bookings.tsx');
 assert.match(page,/api\/v1\/profile\/address-proof/);
 assert.match(page,/proof\.status==='PENDING'/);
 assert.match(page,/proof\.status==='REJECTED'/);
 assert.match(page,/!\['PENDING','APPROVED'\]\.includes\(proof\.status\)/);
 assert.match(page,/bookings\.proofRequired/);
});
test('Kannada T&C text is sourced from translation and facility custom Kannada',async()=>{
 const page=await source('src/app/bookings.tsx');
 const kn=await source('src/i18n/locales/kn.ts');
 assert.match(page,/i18n\.language\.startsWith\('kn'\)/);
 assert.match(page,/termsAndConditionsKn/);
 assert.match(page,/t\('bookings\.facilityTerms'\)/);
 assert.match(page,/t\('bookings\.termsAgreement'\)/);
 assert.ok(kn.includes("ಸೌಲಭ್ಯದ ನಿಯಮಗಳು ಮತ್ತು ಷರತ್ತುಗಳು"));
});
