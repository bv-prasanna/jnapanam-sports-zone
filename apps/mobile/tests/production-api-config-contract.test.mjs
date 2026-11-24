import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const source = path => readFile(new URL('../'+path, import.meta.url), 'utf8');

test('Android release builds cannot silently target emulator localhost', async () => {
  const api = await source('src/lib/api.ts');
  assert.match(api, /EXPO_PUBLIC_API_URL\|\|\(typeof __DEV__!=='undefined'&&__DEV__\?/);
  assert.match(api, /function assertApiUrlConfigured\(\)/);
  assert.match(api, /API_URL_NOT_CONFIGURED/);
  assert.match(api, /refreshAccessToken\(\):Promise<string\|null>\{\s*assertApiUrlConfigured\(\)/);
  assert.match(api, /api<T>\(path:string,options:RequestInit=\{\}\):Promise<T>\{\s*assertApiUrlConfigured\(\)/);
});

test('EAS release profiles explicitly use named environments', async () => {
  const eas = JSON.parse(await source('eas.json'));
  assert.equal(eas.build.preview.environment, 'preview');
  assert.equal(eas.build.production.environment, 'production');
});
