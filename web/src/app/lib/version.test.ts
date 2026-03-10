import { describe, it, expect } from "vitest";
import { APP_NAME, APP_VERSION, SOLUTION_ID, SUPPORT_BASE_URL } from "./version";

describe("version", () => {
  it("exports a non-empty APP_NAME", () => {
    expect(APP_NAME).toBe("HerdManager");
  });

  it("exports a non-empty APP_VERSION", () => {
    expect(APP_VERSION).toBeDefined();
    expect(typeof APP_VERSION).toBe("string");
    expect(APP_VERSION.length).toBeGreaterThan(0);
  });

  it("APP_VERSION matches semver-like pattern (x.y.z)", () => {
    expect(APP_VERSION).toMatch(/^\d+\.\d+\.\d+(-.*)?$/);
  });

  it("exports SOLUTION_ID and SUPPORT_BASE_URL as strings (may be empty for single-instance)", () => {
    expect(typeof SOLUTION_ID).toBe("string");
    expect(typeof SUPPORT_BASE_URL).toBe("string");
  });
});
