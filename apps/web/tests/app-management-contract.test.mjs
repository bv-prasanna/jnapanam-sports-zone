import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');
test('admin system nav links to App Management and exposes update policy controls',async()=>{
 const nav=await source('app/admin/admin-navigation.ts');
 const page=await source('app/admin/app-management/page.tsx');
 assert.match(nav,/href:"\/admin\/app-management"/);
 assert.match(page,/minimumSupportedVersionCode/);
 assert.match(page,/latestVersionCode/);
 assert.match(page,/OPTIONAL/);
 assert.match(page,/MANDATORY/);
 assert.match(page,/Save app settings/);
 assert.match(page,/api\/v1\/admin\/app-management/);
});
test('permanent QR and Android download page always point to /app and resolve backend',async()=>{
 const page=await source('app/app/page.tsx');
 const admin=await source('app/admin/app-management/page.tsx');
 const qr=await source('public/app-download-qr.svg');
 assert.match(page,/api\/v1\/public\/app-release\/android/);
 assert.match(page,/api\/v1\/public\/app-download\/android/);
 assert.match(admin,/fixedLink="https:\/\/jnapanam\.in\/app"/);
 assert.match(qr,/viewBox="0 0 33 33"/);
 assert.match(qr,/https:\/\/jnapanam.in\/app/);
 assert.doesNotMatch(qr,/drive\.google\.com/);
});
