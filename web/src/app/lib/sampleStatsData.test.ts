import { describe, it, expect } from "vitest";
import { SAMPLE_STATS_DATA } from "./sampleStatsData";
import { isValidStats } from "./herdStatsValidation";

describe("sampleStatsData", () => {
  it("passes herdStats validation", () => {
    expect(isValidStats(SAMPLE_STATS_DATA)).toBe(true);
  });

  it("has expected sample values", () => {
    expect(SAMPLE_STATS_DATA.totalAnimals).toBe(124);
    expect(SAMPLE_STATS_DATA.dueSoon).toBe(5);
    expect(SAMPLE_STATS_DATA.calvingsThisYear).toBe(18);
    expect(SAMPLE_STATS_DATA.breedingEventsThisYear).toBe(24);
    expect(SAMPLE_STATS_DATA.openPregnant).toBe(12);
    expect(SAMPLE_STATS_DATA.byStatus).toEqual({ Active: 89, Open: 12, Pregnant: 18, Sold: 5 });
    expect(SAMPLE_STATS_DATA.bySex).toEqual({ MALE: 8, FEMALE: 116 });
  });
});
