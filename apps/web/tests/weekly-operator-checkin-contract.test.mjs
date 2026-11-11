import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=path=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('weekly editor shows exactly seven day cards with nested split periods',async()=>{
 const page=await source('components/facility-schedule-dialog.tsx');
 assert.match(page,/const days=\["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"\]/);
 assert.match(page,/className="weekly-hours">\{days\.map\(day=>/);
 assert.match(page,/className="weekly-day-periods"/);
 assert.match(page,/periods\.map\(\(\{hour,index\},periodIndex\)=>/);
 assert.match(page,/copyDayToAll\(day\)/);
 assert.match(page,/setHours\(days\.flatMap\(day=>hours\.filter/);
 assert.match(page,/window\.confirm/);
 assert.doesNotMatch(page,/className=\{hour\.closed\?"hour-row/);
});

test('operators can select next three days and expand one slot with safe counts',async()=>{
 const page=await source('components/operator-facility-board.tsx');
 assert.match(page,/\["Today",0\],\["Tomorrow",1\],\["Day after",2\]/);
 assert.match(page,/aria-expanded=\{expanded\}/);
 assert.match(page,/currentSlot\?\.key/);
 assert.match(page,/Checked in: \{present\}/);
 assert.match(page,/Yet to check in: \{awaiting\}/);
 assert.match(page,/Bookings: \{active\.length\}/);
 assert.match(page,/g\.bookings\.map\(b=>/);
 assert.match(page,/Verify & check in/);
});

test('operator identity modal retrieves sensitive evidence only after a staff action',async()=>{
 const page=await source('app/operations/page.tsx');
 assert.match(page,/if\(!checkingIn\)/);
 assert.match(page,/apiBlob\(path\+"\/document"\)/);
 assert.match(page,/apiBlob\(path\+"\/photo"\)/);
 assert.match(page,/URL\.revokeObjectURL/);
 assert.match(page,/disabled=\{!!busy\|\|!verified\}/);
});

test('customer website uses compressed uploaded profile photo rather than external URL',async()=>{
 const page=await source('app/dashboard/page.tsx');
 assert.match(page,/Upload profile photo/);
 assert.match(page,/createImageBitmap\(file\)/);
 assert.match(page,/image\/jpeg",\.55/);
 assert.match(page,/api\/v1\/profile\/photo/);
 assert.doesNotMatch(page,/Profile photo URL<input/);
});
