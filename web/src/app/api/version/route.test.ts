import { describe, it, expect } from "vitest";
import { GET } from "./route";

describe("GET /api/version", () => {
  it("returns 200 with version string", async () => {
    const res = await GET();
    expect(res.status).toBe(200);

    const data = (await res.json()) as { version: string };
    expect(typeof data.version).toBe("string");
    expect(data.version.length).toBeGreaterThan(0);
  });

  it("sets Cache-Control no-store for fresh version checks", async () => {
    const res = await GET();
    const cacheControl = res.headers.get("Cache-Control") ?? "";
    expect(cacheControl).toMatch(/no-store|max-age=0/);
  });
});
