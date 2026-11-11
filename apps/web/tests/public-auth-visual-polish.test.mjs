import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

test('public auth and contact surfaces have responsive branded forms and visible focus',async()=>{
  const css=await readFile(new URL('../app/globals.css',import.meta.url),'utf8');
  assert.match(css,/UAT visual-quality pass/);
  assert.match(css,/\.auth-page \.auth-card,.contact-form\s*\{/);
  assert.match(css,/\.auth-page \.auth-card :is\(input,select,textarea\):focus-visible/);
  assert.match(css,/\.site-header :is\(a,button\):focus-visible/);
  assert.match(css,/@media\(max-width:520px\)/);
  assert.match(css,/@media\(prefers-reduced-motion:reduce\)/);
});
