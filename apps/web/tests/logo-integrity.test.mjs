import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";
const root = new URL("../", import.meta.url);
const image = new URL("../public/images/jnapanam-logo.jpg", import.meta.url);
const headerCss = readFileSync(new URL("../app/globals.css", import.meta.url), "utf8");
const footer = readFileSync(new URL("../components/site-footer.tsx", import.meta.url), "utf8");
test("brand uses a nonempty JPEG with valid SOI and EOI markers", () => {
 const bytes = readFileSync(image);
 assert.ok(bytes.length > 10_000);
 assert.equal(bytes[0], 0xff); assert.equal(bytes[1], 0xd8);
 assert.equal(bytes.at(-2), 0xff); assert.equal(bytes.at(-1), 0xd9);
});
test("header and footer use the same logo and legacy PNG is removed", () => {
 assert.ok(headerCss.includes("jnapanam-logo.jpg"));
 assert.ok(footer.includes("jnapanam-logo.jpg"));
 assert.equal(existsSync(new URL("../public/images/jnapanam-logo.png", import.meta.url)), false);
});
