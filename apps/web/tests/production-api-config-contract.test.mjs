import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const source = path => readFile(new URL('../'+path, import.meta.url), 'utf8');

test('production web client has no localhost API fallback', async () => {
  const api = await source('lib/api.ts');
  assert.match(api, /process\.env\.NODE_ENV !== "production" \? "http:\/\/localhost:8080" : ""/);
  assert.match(api, /function assertApiBaseUrlConfigured\(\)/);
  assert.match(api, /The website is not configured to reach Jnapanam services/);
  for (const signature of [
    /async function refreshAccess\(\): Promise<string \| null> \{\s*assertApiBaseUrlConfigured\(\)/,
    /export async function api<T>\(path: string, init: RequestInit = \{\}, retry = true\): Promise<T> \{\s*assertApiBaseUrlConfigured\(\)/,
    /export async function apiBlob\(path:string\):Promise<Blob>\{\s*assertApiBaseUrlConfigured\(\)/,
    /export async function apiDocument\(path:string\):Promise<Blob>\{\s*assertApiBaseUrlConfigured\(\)/
  ]) assert.match(api, signature);
});
