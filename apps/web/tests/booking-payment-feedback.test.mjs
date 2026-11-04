import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');
test('booking page distinguishes payment failure from successful payment explicitly',async()=>{
 const page=await source('app/dashboard/page.tsx');
 const css=await source('app/globals.css');
 assert.match(page,/const \[messageKind,setMessageKind\]=useState<"error"\|"success"\|"info">/);
 assert.match(page,/setMessageKind\("error"\);setMessage\(warning\)/);
 assert.match(page,/setMessageKind\("success"\);setMessage\(result\)/);
 assert.match(page,/messageKind==="error"\?"error"/);
 assert.match(page,/Payment not confirmed for booking/);
 assert.match(css,/\.form-note\.error\{border:1px solid #e7a498!important;background:#fff0ed!important/);
 assert.ok(page.indexOf('function Bookings(')<page.indexOf('messageKind==="error"'),"Explicit status must be in booking component");
});
test('mobile and website default to today in venue timezone',async()=>{
 const page=await source('app/dashboard/page.tsx');
 assert.match(page,/function venueDate\(\)\{return new Date\(Date\.now\(\)\+330\*60_000\)/);
 assert.match(page,/\[date,setDate\]=useState\(\(\)=>venueDate\(\)\)/);
 assert.match(page,/min=\{venueDate\(\)\}/);
 assert.doesNotMatch(page,/Date\.now\(\)\+86400000/);
});
