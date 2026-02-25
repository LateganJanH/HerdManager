import { describe, it, expect } from "vitest";
import {
  getMockAlerts,
  getMockAnimalProfiles,
  getMockHerdStats,
  getMockAnalyticsSeries,
} from "./mockHerdData";

describe("mockHerdData", () => {
  describe("getMockHerdStats", () => {
    it("returns object with required HerdStats fields", () => {
      const stats = getMockHerdStats();
      expect(stats).toHaveProperty("totalAnimals", expect.any(Number));
      expect(stats).toHaveProperty("dueSoon", expect.any(Number));
      expect(stats).toHaveProperty("calvingsThisYear", expect.any(Number));
      expect(stats).toHaveProperty("openPregnant", expect.any(Number));
      expect(stats.byStatus).toBeDefined();
      expect(typeof stats.byStatus).toBe("object");
      expect(stats.bySex).toBeDefined();
      expect(typeof stats.bySex).toBe("object");
    });
  });

  describe("getMockAlerts", () => {
    it("returns an array", () => {
      const alerts = getMockAlerts();
      expect(Array.isArray(alerts)).toBe(true);
    });

    it("each alert has id, type, earTag, dueOrCheckDate, daysUntil", () => {
      const alerts = getMockAlerts();
      for (const a of alerts) {
        expect(a).toHaveProperty("id", expect.any(String));
        expect(["calving", "pregnancy_check", "withdrawal"]).toContain(a.type);
        expect(a).toHaveProperty("earTag", expect.any(String));
        expect(a).toHaveProperty("dueOrCheckDate", expect.any(String));
        expect(a).toHaveProperty("daysUntil", expect.any(Number));
      }
    });
  });

  describe("getMockAnimalProfiles", () => {
    it("returns an array", () => {
      const profiles = getMockAnimalProfiles();
      expect(Array.isArray(profiles)).toBe(true);
    });

    it("each profile has id, earTag, status, sex", () => {
      const profiles = getMockAnimalProfiles();
      for (const p of profiles) {
        expect(p).toHaveProperty("id", expect.any(String));
        expect(p).toHaveProperty("earTag", expect.any(String));
        expect(p).toHaveProperty("status", expect.any(String));
        expect(p).toHaveProperty("sex", expect.any(String));
      }
    });
  });

  describe("getMockAnalyticsSeries", () => {
    it("returns an array", () => {
      const series = getMockAnalyticsSeries();
      expect(Array.isArray(series)).toBe(true);
    });

    it("each item has label and value", () => {
      const series = getMockAnalyticsSeries();
      for (const s of series) {
        expect(s).toHaveProperty("label", expect.any(String));
        expect(s).toHaveProperty("value", expect.any(Number));
      }
    });
  });
});
