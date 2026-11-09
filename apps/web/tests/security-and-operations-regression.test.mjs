import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const read = (path) => readFile(new URL('../'+path, import.meta.url), 'utf8');

test('OpenAPI and Swagger are not publicly accessible', async () => {
  const security = await read('../backend/src/main/java/com/jnapanam/sportszone/security/SecurityConfig.java');
  assert.match(security, /requestMatchers\("\/v3\/api-docs\/\*\*", "\/swagger-ui\/\*\*", "\/swagger-ui.html"\)\.hasAnyRole\("SUPER_ADMIN", "ADMIN"\)/);
  assert.doesNotMatch(security, /requestMatchers\([^\n]*\/v3\/api-docs[^\n]*\)\.permitAll\(\)/);
});

test('operations page does not mistake a failed API load for no records', async () => {
  const page = await read('app/admin/release-readiness/page.tsx');
  assert.match(page, /setTicketsLoaded\(true\)/);
  assert.match(page, /setPaymentsLoaded\(true\)/);
  assert.match(page, /!loading&&ticketsLoaded&&tickets.length===0/);
  assert.match(page, /!loading&&paymentsLoaded&&payments.length===0/);
});

test('booking explorer dates use UTC consistently across timezones', async () => {
  const source = await read('app/admin/bookings/page.tsx');
  assert.match(source, /function addDays\(value:string,days:number\).*setUTCDate\(d.getUTCDate\(\)\+days\)/);
  assert.match(source, /function monthEnd\(value:string\).*Date.UTC\(y,m,0\)/);
  const addDays = (value,days) => {const d=new Date(value+'T00:00:00Z');d.setUTCDate(d.getUTCDate()+days);return d.toISOString().slice(0,10)};
  assert.equal(addDays('2026-11-09',1),'2026-11-10');
  assert.equal(addDays('2026-12-31',1),'2027-01-01');
});
