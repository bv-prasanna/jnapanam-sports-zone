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
 assert.match(dialog,/Save hours & close/);
 assert.match(dialog,/\/operating-periods/);
});

test("schedule footer saves before closing, with discard protection instead of misleading Done",async()=>{
 const dialog=await read("components/facility-schedule-dialog.tsx");
 const css=await read("app/globals.css");
 assert.match(dialog,/Save hours & close/);
 assert.doesNotMatch(dialog,/>Done<\/button>/);
 assert.match(dialog,/onClick=\{\(\)=>void saveHours\(true\)\}/);
 assert.match(dialog,/if\(closeAfter\)onClose\(\)/);
 assert.match(dialog,/function requestClose\(\)\{if\(saving\)return;if\(editedHours\|\|savedDays\.length<7\)/);
 assert.match(dialog,/confirmDiscard&&/);
 assert.match(dialog,/Keep editing/);
 assert.match(dialog,/Discard and close/);
 assert.match(css,/\.facility-schedule-dialog \.schedule-dialog-actions\{[^}]*position:sticky/);
});

test("closure changes do not discard edited but unsaved weekly hours",async()=>{
 const dialog=await read("components/facility-schedule-dialog.tsx");
 assert.match(dialog,/await load\(editedHours\)/);
 assert.match(dialog,/if\(!preserveEdits\)\{setEditedHours\(false\)/);
});

test("split windows, cleaning buffer, capacity and terms are configurable",async()=>{
 const dialog=await read("components/facility-schedule-dialog.tsx");
 const admin=await read("app/admin/page.tsx");
 const booking=await read("app/dashboard/page.tsx");
 assert.match(dialog,/Add another period/);
 assert.match(dialog,/replaceOperatingPeriods|operating-periods/);
 assert.match(admin,/Maximum members per slot/);
 assert.match(admin,/bufferMinutes/);
 assert.match(admin,/termsAndConditions/);
 assert.match(booking,/acceptedTerms:termsAccepted/);
});
test("feature visibility is controlled by admin and customer navigation",async()=>{
 const config=await read("lib/features.ts");
 const admin=await read("app/admin/features/page.tsx");
 assert.match(config,/\/api\/v1\/public\/features/);
 assert.match(admin,/\/api\/v1\/admin\/features/);
 assert.match(admin,/allowedRoles/);
});

test("disabled features are removed from customer shortcuts, nav and direct customer views",async()=>{
 const dashboard=await read("app/dashboard/page.tsx");
 assert.match(dashboard,/filter\(enabled\)\.map/);
 assert.match(dashboard,/visibleSection=featureEnabled\(section\)\?section:"overview"/);
 assert.match(dashboard,/enabled=\{featureEnabled\}/);
});
