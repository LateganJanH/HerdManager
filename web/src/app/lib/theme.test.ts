import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { getTheme, setTheme, type Theme } from "./theme";

describe("theme", () => {
  const storage: Record<string, string> = {};
  const mockLocalStorage = {
    getItem: (key: string) => storage[key] ?? null,
    setItem: (key: string, value: string) => {
      storage[key] = value;
    },
    removeItem: () => {},
    clear: () => {},
    length: 0,
    key: () => null,
  };

  beforeEach(() => {
    vi.stubGlobal("localStorage", mockLocalStorage);
    vi.stubGlobal("window", {
      localStorage: mockLocalStorage,
      document: { documentElement: { classList: { toggle: vi.fn() } } },
      matchMedia: () => ({ addEventListener: vi.fn(), removeEventListener: vi.fn() }),
      dispatchEvent: vi.fn(),
    });
    storage["hm-theme"] = "";
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("getTheme", () => {
    it("returns 'system' when localStorage is empty or invalid", () => {
      delete storage["hm-theme"];
      expect(getTheme()).toBe("system");
      storage["hm-theme"] = "";
      expect(getTheme()).toBe("system");
      storage["hm-theme"] = "invalid";
      expect(getTheme()).toBe("system");
    });

    it("returns stored theme when valid", () => {
      const valid: Theme[] = ["light", "dark", "system"];
      for (const t of valid) {
        storage["hm-theme"] = t;
        expect(getTheme()).toBe(t);
      }
    });
  });

  describe("setTheme", () => {
    it("writes to localStorage", () => {
      setTheme("dark");
      expect(storage["hm-theme"]).toBe("dark");
      setTheme("light");
      expect(storage["hm-theme"]).toBe("light");
      setTheme("system");
      expect(storage["hm-theme"]).toBe("system");
    });
  });
});

