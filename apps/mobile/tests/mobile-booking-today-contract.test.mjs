import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
const source=path=>readFile(new URL('../'+path,import.meta.url),'utf8');
test('mobile booking includes today, with all venue dates based on Asia/Kolkata',async()=>{
 const page=await source('src/app/bookings.tsx');
 assert.match(page,/dateValue\(0\)/);
 assert.match(page,/330 \* 60_000/);
 assert.match(page,/86_400_000/);
 assert.match(page,/Array\.from\(\{ length: 7 \}, \(_, index\) =>/);
 assert.match(page,/setDate\(selected => selected < current \? current : selected\)/);
 assert.doesNotMatch(page,/dateValue\(index \+ 1\)/);
});
test('mobile booking buttons remain tappable when facility rules are not yet accepted',async()=>{
 const page=await source('src/app/bookings.tsx');
 assert.match(page,/if \(facilities\.find\(item => item\.id === facilityId\)\?\.termsAndConditions && !termsAccepted\)/);
 assert.match(page,/setError\(t\('bookings\.termsRequired'\)\)/);
 assert.doesNotMatch(page,/disabled=\{!slot\.available \|\| !quotes\[slot\.id\] \|\| !!processing \|\| \(\(!!facilities/);
});

test('Razorpay success uses a native app-return deep link and rechecks server payment status',async()=>{
 const payments=await source('src/lib/payments.ts');
 const route=await source('src/app/payment-return.tsx');
 assert.match(payments,/WebBrowser\.openAuthSessionAsync/);
 assert.match(payments,/Linking\.createURL\('payment-return', \{ scheme: 'jnapanamsportszone' \}\)/);
 assert.doesNotMatch(payments,/WebBrowser\.openBrowserAsync/);
 assert.match(payments,/receipt\.status !== 'PAID'/);
 assert.match(route,/<Redirect href="\/bookings"/);
});
