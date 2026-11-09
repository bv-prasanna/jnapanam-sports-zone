import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

test('administrator error log supports known status families and copy feedback', async ()=>{
  const source=await readFile(new URL('../app/admin/errors/page.tsx',import.meta.url),'utf8');
  for(const status of ['400','401','403','404','408','409','422','426','429','500','502','503','504']){
    assert.match(source,new RegExp('value:"'+status+'"'), 'Missing status filter '+status);
  }
  assert.match(source,/role="status">\{copyFeedback\}/);
  assert.match(source,/aria-label=\{"Copy support reference "/);
  assert.match(source,/Automatic copying is unavailable/);
  assert.doesNotMatch(source,/void navigator\.clipboard\?\.writeText\(r\.correlationId!/);
});
