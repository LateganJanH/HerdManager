import { describe, it, expect } from "vitest";
import { seriesFromStats, STATUS_COLORS } from "./analyticsSeries";
import type { HerdStats } from "./herdStatsTypes";

function stats(overrides: Partial<HerdStats> = {}): HerdStats {
  return {
    totalAnimals: 0,
    dueSoon: 0,
    calvingsThisYear: 0,
    breedingEventsThisYear: 0,
    openPregnant: 0,
    byStatus: {},
    bySex: {},
    ...overrides,
  };
}

describe("seriesFromStats", () => {
  it("returns empty array for empty stats", () => {
    expect(seriesFromStats(stats())).toEqual([]);
  });

  it("includes byStatus entries with colors for known statuses", () => {
    const result = seriesFromStats(
      stats({ byStatus: { Active: 10, Open: 2, Pregnant: 3 } })
    );
    expect(result).toHaveLength(3);
    expect(result.find((s) => s.label === "Active")).toEqual({
      label: "Active",
      value: 10,
      color: STATUS_COLORS.Active,
    });
    expect(result.find((s) => s.label === "Open")).toMatchObject({
      label: "Open",
      value: 2,
    });
    expect(result.find((s) => s.label === "Pregnant")).toMatchObject({
      label: "Pregnant",
      value: 3,
    });
  });

  it("uses default color for unknown status", () => {
    const result = seriesFromStats(
      stats({ byStatus: { Custom: 5 } })
    );
    expect(result[0]).toMatchObject({
      label: "Custom",
      value: 5,
      color: "var(--color-primary, #1b4332)",
    });
  });

  it("includes bySex entries with Sex: prefix", () => {
    const result = seriesFromStats(
      stats({ bySex: { MALE: 2, FEMALE: 8 } })
    );
    expect(result).toHaveLength(2);
    expect(result.find((s) => s.label === "Sex: MALE")).toEqual({
      label: "Sex: MALE",
      value: 2,
    });
    expect(result.find((s) => s.label === "Sex: FEMALE")).toEqual({
      label: "Sex: FEMALE",
      value: 8,
    });
  });

  it("returns status entries first then sex entries", () => {
    const result = seriesFromStats(
      stats({
        byStatus: { Active: 1 },
        bySex: { FEMALE: 1 },
      })
    );
    expect(result.map((s) => s.label)).toEqual(["Active", "Sex: FEMALE"]);
  });
});
