import { describe, it, expect } from "vitest";
import { GET } from "./route";

describe("GET /api/health", () => {
  it("returns 200 with ok, service, and version", async () => {
    const res = await GET();
    expect(res.status).toBe(200);

    const data = await res.json();
    expect(data.ok).toBe(true);
    expect(data.service).toBe("herdmanager-web");
    expect(typeof data.version).toBe("string");
    expect(data.version.length).toBeGreaterThan(0);
  });

  it("sets Cache-Control no-store for fresh health checks", async () => {
    const res = await GET();
    const cacheControl = res.headers.get("Cache-Control") ?? "";
    expect(cacheControl).toMatch(/no-store|max-age=0/);
  });
});
