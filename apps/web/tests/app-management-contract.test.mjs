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

test('top navigation shows a bilingual download link after registration in desktop and mobile menus',async()=>{
 const header=await source('components/site-header.tsx');
 const translations=await source('lib/i18n.ts');
 const styles=await source('app/globals.css');
 const links=[...header.matchAll(/className="btn app-download-link" href="\/app"/g)];
 assert.equal(links.length,2,'desktop and mobile navigation both link to the permanent download page');
 assert.match(header,/<Link className="btn primary" href="\/register"[^>]*>\{t\("register"\)\}<\/Link><\/\>}<Link className="btn app-download-link" href="\/app"/);
 assert.match(header, /onClick=\{\(\)=>setOpen\(false\)\}><AndroidMark\/>/);
 assert.match(header, /t\("downloadApp"\)/);
 assert.match(header,/function AndroidMark\(\)/);
 assert.match(translations, /downloadApp:"Download App"/);
 assert.match(translations, /downloadApp:"ಆ್ಯಪ್ ಡೌನ್‌ಲೋಡ್"/);
 assert.match(styles,/\.site-header \.app-download-link/);
 assert.match(styles,/@media\(max-width:1320px\)/);
});

test('footer includes existing app QR beside venue contact details and links to stable /app',async()=>{
 const footer=await source('components/site-footer.tsx');
 const styles=await source('app/globals.css');
 const visit=footer.indexOf('<h3>Visit Us</h3>');
 const qr=footer.indexOf('src="/app-download-qr.svg"');
 assert.ok(visit>=0 && qr>visit,'QR is inside visit/contact column');
 assert.match(footer,/href="\/app" className="footer-app-qr"/);
 assert.match(footer,/Scan to download the latest APK/);
 assert.match(styles,/\.site-footer \.footer-app-qr/);
});
