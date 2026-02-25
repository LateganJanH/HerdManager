import { describe, it, expect } from "vitest";
import manifest from "./manifest";

describe("manifest", () => {
  it("returns required PWA manifest fields", () => {
    const result = manifest();
    expect(result).toHaveProperty("name");
    expect(typeof result.name).toBe("string");
    expect(result.name.length).toBeGreaterThan(0);
    expect(result).toHaveProperty("short_name");
    expect(result).toHaveProperty("start_url", "/");
    expect(result).toHaveProperty("display", "standalone");
    expect(result).toHaveProperty("scope", "/");
  });

  it("includes HerdManager in name or short_name", () => {
    const result = manifest();
    const name = result.name ?? "";
    const shortName = result.short_name ?? "";
    expect(name.includes("HerdManager") || shortName.includes("HerdManager")).toBe(true);
  });

  it("has at least one icon with src and type", () => {
    const result = manifest();
    expect(Array.isArray(result.icons)).toBe(true);
    expect(result.icons!.length).toBeGreaterThan(0);
    const icon = result.icons![0];
    expect(icon).toHaveProperty("src");
    expect(icon).toHaveProperty("type", "image/svg+xml");
  });

  it("sets theme_color and background_color", () => {
    const result = manifest();
    expect(result.theme_color).toBeDefined();
    expect(result.background_color).toBeDefined();
  });
});
