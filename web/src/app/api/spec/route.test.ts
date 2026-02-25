import { describe, it, expect } from "vitest";
import { GET } from "./route";

describe("GET /api/spec", () => {
  it("returns 200 with OpenAPI YAML when spec is available", async () => {
    const res = await GET();
    expect(res.status).toBe(200);
    expect(res.headers.get("Content-Type")).toMatch(/yaml/);
    const body = await res.text();
    expect(body).toContain("openapi:");
    expect(body).toContain("HerdManager API");
  });
});
