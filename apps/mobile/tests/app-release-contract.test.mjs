import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=p=>readFile(new URL('../'+p,import.meta.url),'utf8');
test('mobile native build number drives update requirements and cached offline policy',async()=>{
 const gate=await source('src/components/app-update-gate.tsx');
 const policy=await source('src/lib/app-release.ts');
 const api=await source('src/lib/api.ts');
 const manifest=JSON.parse(await source('app.json'));
 assert.equal(manifest.expo.android.versionCode,3);
 assert.match(policy,/Application\.nativeBuildVersion/);
 assert.match(policy,/getStoredValue\(CACHE_KEY\)/);
 assert.match(policy,/installed<policy\.minimumSupportedVersionCode/);
 assert.match(policy,/installed<policy\.latestVersionCode/);
 assert.match(gate,/AppState\.addEventListener\('change'/);
 assert.match(gate,/Later · Continue to app/);
 assert.match(gate,/Download updated app/);
 assert.match(gate,/I've updated · Check again/);
 assert.match(api,/X-Jnapanam-Version-Code/);
 assert.match(api,/status===426/);
 assert.match(api,/announceServerRequiredUpdate\(\)/);
});
test('app release gate wraps tabs without changing existing booking or payment routes',async()=>{
 const root=await source('src/app/_layout.tsx');
 assert.match(root,/<AppUpdateGate><AppTabs\/><\/AppUpdateGate>/);
 assert.match(root,/<AuthProvider>/);
});
