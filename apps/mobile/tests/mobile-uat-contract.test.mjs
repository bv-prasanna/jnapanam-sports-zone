import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=(path)=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('one hamburger navigation replaces every bottom tab and omits Admin tools',async()=>{
  const tabs=await source('src/components/app-tabs.tsx');
  const menu=await source('src/components/mobile-header.tsx');
  const admin=await source('src/app/admin.tsx');
  assert.match(tabs,/tabBar=\{\(\)=>null\}/);
  assert.match(menu,/name="menu"/);
  assert.match(menu,/onRequestClose=\{\(\)=>setOpen\(false\)\}/);
  assert.match(menu,/roles\.some/);
  assert.match(menu,/canOperate/);
  assert.match(menu,/canShop/);
  assert.doesNotMatch(menu,/href:'\/admin'/);
  assert.match(admin,/<Redirect href="\/"\/>/);
});

test('approved website logo is used in shared mobile header, not oversize legacy logo',async()=>{
  const header=await source('src/components/mobile-header.tsx');
  const home=await source('src/app/index.tsx');
  assert.match(header,/jnapanam-brand\.png/);
  assert.match(header,/SafeAreaView/);
  assert.doesNotMatch(home,/jnapanam-logo-current\.png/);
  assert.doesNotMatch(home,/<LanguageSwitch/);
});

test('web profile fields are editable on Android without a remounting nested field component',async()=>{
  const profile=await source('src/app/profile.tsx');
  const auth=await source('src/features/auth/auth-context.tsx');
  for(const field of ['displayName','mobile','addressLine','city','state','postalCode','profilePhotoUrl']){
    assert.match(profile,new RegExp('field\\("'+field+'"'),'Field '+field+' is required');
  }
  assert.match(profile,/\/api\/v1\/profile\/me/);
  assert.match(profile,/updateUser\(updated\)/);
  assert.match(profile,/useFocusEffect/);
  assert.match(auth,/refreshProfile/);
  assert.match(profile,/\/api\/v1\/profile\/address-proof/);
  assert.match(profile,/DocumentPicker\.getDocumentAsync/);
  assert.match(profile,/\/api\/v1\/profile\/photo/);
  assert.match(profile,/\/api\/v1\/profile\/address-proof/);
  assert.match(profile,/body\.append\('file'/);
  assert.doesNotMatch(profile,/openWebProfile/);
  assert.doesNotMatch(profile,/const Field=/);
});

test('English and Kannada have menu, profile and network error messages',async()=>{
  const en=await source('src/i18n/locales/en.ts');
  const kn=await source('src/i18n/locales/kn.ts');
  for(const content of [en,kn]){
    for(const section of ['mobileMenu:{','mobileProfile:{','errors:{','sports:{'])assert.ok(content.includes(section),section);
    for(const key of ['network:','server:','session:','displayName:','open:','shopCounter:','operations:'])
      assert.ok(content.includes(key),key);
  }
  const language=await source('src/components/language-switch.tsx');
  assert.match(language,/i18n\.changeLanguage\(language\)/);
  assert.match(language,/setStoredValue\('jnapanam\.language'/);
});

test('socket and Java exceptions do not leak to users and money requests are not auto-retried',async()=>{
  const api=await source('src/lib/api.ts');
  assert.match(api,/SocketException|socketexception/);
  assert.match(api,/status>=500/);
  assert.match(api,/throw new ApiError\(friendly\('network'\),0\)/);
  assert.doesNotMatch(api,/setTimeout\(|retryCount/);
});

test('UAT APK builds arm64 only and logs compressed versus installed native sizes',async()=>{
  const workflow=await readFile(new URL('../../../.github/workflows/android-apk-cloud.yml',import.meta.url),'utf8');
  assert.match(workflow,/-PreactNativeArchitectures=arm64-v8a/);
  assert.match(workflow,/native libraries/);
});

test('multipart file uploads preserve device-generated boundary and enforce file limits',async()=>{
 const api=await source('src/lib/api.ts');
 const profile=await source('src/app/profile.tsx');
 const pkg=JSON.parse(await source('package.json'));
 const lock=JSON.parse(await source('package-lock.json'));
 assert.match(api,/options\.body instanceof FormData/);
 assert.match(profile,/asset\.size>5\*1024\*1024/);
 assert.match(profile,/asset\.size>2\*1024\*1024/);
 assert.match(profile,/copyToCacheDirectory:true/);
 assert.ok(pkg.dependencies['expo-document-picker']);
 assert.ok(lock.packages['node_modules/expo-document-picker']);
});

test('registration OTP displays actual recipient in English and Kannada',async()=>{
 const screen=await source('src/app/register.tsx');
 const auth=await source('src/features/auth/auth-context.tsx');
 const en=await source('src/i18n/locales/en.ts');
 const kn=await source('src/i18n/locales/kn.ts');
 assert.match(auth,/registration\/email\/request/);
 assert.match(screen,/setOtpEmail\(form\.email\.trim\(\)\.toLowerCase\(\)\)/);
 assert.match(screen,/\{otpEmail\}/);
 assert.match(screen,/!otpSent&&\(\['fullName'/);
 assert.match(screen,/auth\.changeEmail/);
 for(const locale of [en,kn]){
  for(const key of ['emailOtpSentTo:','verifyEmail:','emailVerificationCode:','changeEmail:','verifyAndCreateAccount:'])
   assert.ok(locale.includes(key),key);
 }
});

test('mobile registration requires phone details but continues email-only OTP',async()=>{
 const screen=await source('src/app/register.tsx');
 const auth=await source('src/features/auth/auth-context.tsx');
 const en=await source('src/i18n/locales/en.ts');
 const kn=await source('src/i18n/locales/kn.ts');
 assert.match(screen,/form\.mobile\.replace\(\/\[\^0-9\]\/g,'\'\)\.length<10/);
 assert.doesNotMatch(screen,/if\(form\.mobile &&/);
 assert.match(auth,/mobile: string; password: string/);
 assert.match(auth,/registration\/email\/request/);
 assert.match(auth,/registration\/email\/verify/);
 assert.doesNotMatch(auth,/registration\/request'/);
 for(const locale of [en,kn])assert.match(locale,/validMobile:/);
});
