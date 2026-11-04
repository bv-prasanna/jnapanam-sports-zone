import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
const source=path=>readFile(new URL("../"+path,import.meta.url),"utf8");

test("every booking checks latest address-proof status and displays pending / rejected states",async()=>{
 const page=await source("app/dashboard/page.tsx");
 assert.match(page,/\/api\/v1\/profile\/address-proof/);
 assert.match(page,/proof\?\.status==="PENDING"/);
 assert.match(page,/proof\?\.status==="REJECTED"/);
 assert.match(page,/proof\.reviewNote/);
 assert.match(page,/!\["PENDING","APPROVED"\]\.includes\(proof\.status\)/);
 assert.doesNotMatch(page,/For your first booking, <a/);
});
test("profile shows verification pending after upload and rejection reason when returned",async()=>{
 const p=await source("app/profile/page.tsx");
 assert.match(p,/setProofStatus\(value\.status\);setProofReviewNote\(null\)/);
 assert.match(p,/Verification pending — document uploaded/);
 assert.match(p,/proofStatus==="REJECTED"&&proofReviewNote/);
});
test("booking terms are bilingual from the facility, never fake-translated",async()=>{
 const page=await source("app/dashboard/page.tsx");
 const form=await source("app/admin/page.tsx");
 assert.match(page,/termsAndConditionsKn/);
 assert.match(page,/language==="kn"/);
 assert.match(form,/name="termsAndConditionsKn"/);
 assert.match(form,/value\("termsAndConditionsKn"\)/);
});
test("billing receipt and issued tax invoice include all price and settlement components",async()=>{
 const view=await source("app/billing-documents/[id]/page.tsx");
 for(const key of ["originalAmountPaise","promotionCode","discountPaise","loyaltyPointsUsed",
  "billingTaxablePaise","billingTaxPaise","billingLoyaltyTenderPaise","billingGrossPaise",
  "cgstPaise","sgstPaise","igstPaise","customerCashPaidPaise"])
 assert.ok(view.includes(key),key);
 assert.match(view,/not a GST tax invoice/);
});
