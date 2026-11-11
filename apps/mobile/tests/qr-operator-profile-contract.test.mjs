import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=path=>readFile(new URL('../'+path,import.meta.url),'utf8');

test('customer QR pass is cached by user and booking, never refreshed on every screen open',async()=>{
 const page=await source('src/app/booking-qr.tsx');
 assert.match(page,/jnapanam\.checkin-pass/);
 assert.match(page,/getStoredValue\(key\)/);
 assert.match(page,/if\(cached\?\.startsWith\('JNZCI1\.'\)\)/);
 assert.match(page,/setStoredValue\(key,response\.value\)/);
 assert.match(page,/<QRCode value=\{pass\}/);
 assert.match(page,/Refresh pass \(after rescheduling\)/);
 const bookings=await source('src/app/bookings.tsx');
 assert.match(bookings,/booking\.status==='CONFIRMED'/);
 assert.match(bookings,/router\.push\(\{pathname:'\/booking-qr'/);
});

test('operator QR scanning requires server verification and explicit identity acknowledgement',async()=>{
 const page=await source('src/app/scan-checkin.tsx');
 assert.match(page,/useCameraPermissions\(\)/);
 assert.match(page,/barcodeScannerSettings=\{\{barcodeTypes:\['qr'\]\}\}/);
 assert.match(page,/operations\/check-in\/qr\/verify/);
 assert.match(page,/operations\/check-in\/qr\/confirm/);
 assert.match(page,/identityVerified:true/);
 assert.match(page,/!verified\|\|!identity\?\.photoAvailable/);
 assert.match(page,/Scan next booking/);
});

test('new customers are directed to photo and proof upload with camera and browse options',async()=>{
 const register=await source('src/app/register.tsx');
 const profile=await source('src/app/profile.tsx');
 const bookings=await source('src/app/bookings.tsx');
 assert.match(register,/router\.replace\('\/profile'\)/);
 assert.match(profile,/launchCameraAsync/);
 assert.match(profile,/launchImageLibraryAsync/);
 assert.match(profile,/0\.45/);
 assert.match(profile,/Upload a clear profile photo and a government ID/);
 assert.match(bookings,/Please upload a profile photo in My Profile before booking/);
});

test('operator mobile includes manual identity confirmation and day shortcuts',async()=>{
 const page=await source('src/app/operations.tsx');
 assert.match(page,/router\.push\('\/scan-checkin'\)/);
 assert.match(page,/\["Today",0\],\["Tomorrow",1\],\["Day after",2\]/);
 assert.match(page,/manualBooking/);
 assert.match(page,/identity\.profilePhotoUrl/);
 assert.match(page,/I verified the customer's identity/);
});
