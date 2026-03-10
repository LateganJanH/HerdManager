import { describe, it, expect } from "vitest";
import { GET } from "./route";

describe("GET /api/instance-config", () => {
  it("returns 200 with solutionId and supportBaseUrl strings", async () => {
    const res = await GET();
    expect(res.status).toBe(200);

    const data = (await res.json()) as { solutionId: string; supportBaseUrl: string };
    expect(typeof data.solutionId).toBe("string");
    expect(typeof data.supportBaseUrl).toBe("string");
  });

  it("sets Cache-Control no-store", async () => {
    const res = await GET();
    const cacheControl = res.headers.get("Cache-Control") ?? "";
    expect(cacheControl).toMatch(/no-store|max-age=0/);
  });
});
