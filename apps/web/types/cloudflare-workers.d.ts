/** Minimal type bridge for Vinext's Cloudflare module binding. */
declare module "cloudflare:workers" {
  export const env: { DB: Parameters<typeof import("drizzle-orm/d1").drizzle>[0] };
}
