import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const read = (path) => readFile(new URL('../'+path, import.meta.url), 'utf8');

test('OpenAPI and Swagger are not publicly accessible', async () => {
  const security = await read('../../backend/src/main/java/com/jnapanam/sportszone/security/SecurityConfig.java');
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
