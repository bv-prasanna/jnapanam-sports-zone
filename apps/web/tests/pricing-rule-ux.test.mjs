import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
import test from "node:test";

const file=new URL("../app/admin/pricing/page.tsx",import.meta.url);
const stylesheet=new URL("../app/admin/pricing/pricing.module.css",import.meta.url);
const priceSource=await readFile(file,"utf8");

test("recurring pricing defaults to facility and fixed price without a date",()=>{
  assert.match(priceSource,/const empty=\(scope:Scope="FACILITY"\)/);
  assert.match(priceSource,/adjustment:"FIXED_PRICE"/);
  assert.match(priceSource,/effectiveFrom:"",effectiveTo:""/);
  assert.match(priceSource,/Recurring facility price|Regular facility prices|recurring facility/i);
});

test("facility pricing requires selected court but not a dated slot",()=>{
  assert.match(priceSource,/form\.scope!=="SITE"&&!form\.target/);
  assert.match(priceSource,/form\.scope==="SLOT"&&!editing&&\(!slotFacility/);
  assert.match(priceSource,/form\.scope==="FACILITY"&&/);
  assert.match(priceSource,/slotFacility,setSlotFacility/);
});

test("no slots shows an actionable explanation, not a silent mandatory dropdown",()=>{
  assert.match(priceSource,/No slots exist for this facility on the selected date/);
  assert.match(priceSource,/Switch to Facility \/ court/);
  assert.match(priceSource,/slotLoading\|\|!slotFacility\|\|slots\.length===0/);
  assert.match(priceSource,/First configure the court operating hours/);
});

test("cricket regular daytime and floodlight examples use separate time bands",()=>{
  assert.match(priceSource,/name:preset==="day"\?"Daytime standard rate":"Evening floodlight rate"/);
  assert.match(priceSource,/value:preset==="day"\?"300":"400"/);
  assert.match(priceSource,/startTime:preset==="day"\?"00:00":"18:00"/);
  assert.match(priceSource,/endTime:preset==="day"\?"18:00":"23:00"/);
  assert.match(priceSource,/FIXED_PRICE/);
});

test("one-day exception inherits facility rule and returns to recurring rate after the day",()=>{
  assert.match(priceSource,/exceptionForDate\(rule:Item\)/);
  assert.match(priceSource,/const priority=Math\.max\([\s\S]*?\)\+1/);
  assert.match(priceSource,/effectiveFrom:e\.target\.value,effectiveTo:e\.target\.value/);
  assert.match(priceSource,/One-day override saved/);
  assert.match(priceSource,/!editing&&!exceptionFor/);
});

test("existing slot rules remain editable when the current date has no generated slots",()=>{
  assert.match(priceSource,/form\.scope==="SLOT"&&!editing/);
  assert.match(priceSource,/form\.target&&!targets\.some/);
});

test("responsive slot empty state and optional validity controls use shared admin theme",async()=>{
  const css=await readFile(stylesheet,"utf8");
  assert.match(priceSource,/Optional validity dates/);
  assert.match(css,/\.slotNotice/);
  assert.match(css,/\.advancedDates/);
  assert.match(css,/\.guide/);
  assert.match(css,/--admin-primary/);
});
