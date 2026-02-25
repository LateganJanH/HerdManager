import { describe, it, expect } from "vitest";
import sitemap from "./sitemap";

describe("sitemap", () => {
  it("returns non-empty array of entries with url and lastModified", () => {
    const result = sitemap();
    expect(Array.isArray(result)).toBe(true);
    expect(result.length).toBeGreaterThan(0);
    for (const entry of result) {
      expect(entry).toHaveProperty("url");
      expect(typeof entry.url).toBe("string");
      expect(entry.url.length).toBeGreaterThan(0);
      expect(entry).toHaveProperty("lastModified");
      expect(entry.lastModified).toBeInstanceOf(Date);
    }
  });

  it("includes base URL (default or from env)", () => {
    const result = sitemap();
    const urls = result.map((e) => e.url);
    const hasValidUrl = urls.some((u) => u.startsWith("http") && u.includes("herdmanager"));
    expect(hasValidUrl).toBe(true);
  });
});
