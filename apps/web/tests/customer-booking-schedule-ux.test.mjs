import test from "node:test";
import assert from "node:assert/strict";
import {readFile} from "node:fs/promises";
const read=(p)=>readFile(new URL("../"+p,import.meta.url),"utf8");

test("customer booking header is not split into competing flex columns",async()=>{
 const page=await read("app/dashboard/page.tsx");
 const css=await read("app/globals.css");
 assert.match(page,/<header className="customer-booking-heading">/);
 assert.match(page,/<header className="customer-booking-heading customer-booking-subheading">/);
 assert.doesNotMatch(page,/<div className="portal-heading"><h1>Bookings<\/h1>/);
 assert.match(css,/\.customer-booking-heading\{display:grid;grid-template-columns:minmax\(0,1fr\)/);
 assert.match(css,/@media\(max-width:520px\)\{\.booking-search/);
});

test("booking UI diagnoses absent operating hours for Admin and links to existing schedule editor",async()=>{
 const page=await read("app/dashboard/page.tsx");
 assert.match(page,/setNoSlots\(true\)/);
 assert.match(page,/\/api\/v1\/admin\/catalog\/facilities\/\$\{facility\}\/schedule/);
 assert.match(page,/No operating hours are saved for/);
 assert.match(page,/This facility has a closure on the selected date/);
 assert.match(page,/isAdmin&&<div className="booking-schedule-help">/);
 assert.match(page,/href="\/admin\?view=facilities"/);
 assert.match(page,/api<Slot\[]>\(`\/api\/v1\/public\/facilities/);
});

test("facility schedule clearly marks sample hours as unsaved until persisted",async()=>{
 const dialog=await read("components/facility-schedule-dialog.tsx");
 assert.match(dialog,/savedDays\.length===0/);
 assert.match(dialog,/unsaved example/);
 assert.match(dialog,/customers will not see bookable slots/i);
 assert.match(dialog,/Not saved/);
 assert.match(dialog,/Save operating hours/);
 assert.match(dialog,/\/operating-hours/);
});
