import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
const source=path=>readFile(new URL("../"+path,import.meta.url),"utf8");
test("two public numbers are centralized and dialable; obsolete number removed",async()=>{
 const phones=await source("lib/contact.ts");
 const header=await source("components/site-header.tsx");
 const footer=await source("components/site-footer.tsx");
 const page=await source("app/contact/page.tsx");
 assert.match(phones,/9449284747/);
 assert.match(phones,/9448233989/);
 for(const component of [header,footer,page]){
  assert.match(component,/CONTACT_PHONES\.map/);
  assert.match(component,/tel:\$\{phone\.e164\}/);
  assert.doesNotMatch(component,/8296022107|82960\s*22107/);
 }
});
test("verification step shows exact email and permits correcting address",async()=>{
 const registration=await source("app/register/page.tsx");
 assert.match(registration,/setSaved\(payload\);setOtpSent\(true\)/);
 assert.match(registration,/\{saved\?\.email\}/);
 assert.match(registration,/Wrong email\? Change address/);
 assert.match(registration,/registration-otp-destination/);
 assert.match(registration,/registerWithEmailOtp\(\{\.\.\.saved,otp:/);
});

test("registration requires mobile number without activating mobile OTP",async()=>{
 const registration=await source("app/register/page.tsx");
 const api=await source("lib/api.ts");
 assert.match(registration,/<label>Mobile number<input name="mobile" required type="tel"/);
 assert.doesNotMatch(registration,/optional, not verified/i);
 assert.match(registration,/payload\.mobile\.replace\(\/\[\^0-9\]\/g,""\)\.length<10/);
 assert.match(registration,/requestEmailRegistrationOtp\(payload\.email\)/);
 assert.doesNotMatch(registration,/requestRegistrationOtp\(payload\.mobile\)/);
 assert.match(api,/registration\/email\/request/);
});
