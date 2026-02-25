import { describe, it, expect } from "vitest";
import robots from "./robots";

describe("robots", () => {
  it("returns rules with userAgent and allow", () => {
    const result = robots();
    expect(result).toHaveProperty("rules");
    expect(result.rules).toHaveProperty("userAgent", "*");
    expect(result.rules).toHaveProperty("allow", "/");
  });

  it("includes sitemap URL with base and /sitemap.xml", () => {
    const result = robots();
    expect(result).toHaveProperty("sitemap");
    expect(typeof result.sitemap).toBe("string");
    expect(result.sitemap).toMatch(/\/sitemap\.xml$/);
    expect(result.sitemap).toMatch(/^https?:\/\//);
  });
});
