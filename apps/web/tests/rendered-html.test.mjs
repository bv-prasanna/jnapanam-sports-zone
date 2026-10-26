import assert from "node:assert/strict";
import test from "node:test";

const developmentPreviewMeta =
  /<meta(?=[^>]*\bname=["']codex-preview["'])(?=[^>]*\bcontent=["']development["'])[^>]*>/i;

test("renders development preview metadata", async () => {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  const response = await worker.fetch(
    new Request("http://localhost/", {
      headers: { accept: "text/html" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );

  assert.equal(response.status, 200);
  assert.match(
    response.headers.get("content-type") ?? "",
    /^text\/html\b/i,
  );
  assert.match(await response.text(), developmentPreviewMeta);
});


test("registration route renders without server-side exceptions", async () => {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("route-test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);
  const response = await worker.fetch(
    new Request("http://localhost/register", { headers: { accept: "text/html" } }),
    { ASSETS: { fetch: async () => new Response("Not found", { status: 404 }) } },
    { waitUntil() {}, passThroughOnException() {} },
  );
  const html = await response.text();
  assert.equal(response.status, 200, `Registration SSR returned ${response.status}: ${html.slice(0, 500)}`);
  assert.match(html, /Create Account|Send Email OTP|Verify email/);
});

test("login and profile routes render without Worker exceptions", async()=>{
 const moduleUrl=new URL("../dist/server/index.js",import.meta.url);
 moduleUrl.searchParams.set("extra",String(Date.now()));
 const {default:worker}=await import(moduleUrl.href);
 const env={ASSETS:{fetch:async()=>new Response("Not found",{status:404})}};
 const ctx={waitUntil(){},passThroughOnException(){}};
 for(const route of ["/login","/profile"]){
  const result=await worker.fetch(new Request("http://localhost"+route,{headers:{accept:"text/html"}}),env,ctx);
  assert.equal(result.status,200,route+" returned "+result.status);
 }
});
