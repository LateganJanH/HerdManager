import { describe, it, expect } from "vitest";
import { isValidStats } from "./herdStatsValidation";

describe("isValidStats", () => {
  it("returns true for valid HerdStats", () => {
    expect(
      isValidStats({
        totalAnimals: 10,
        dueSoon: 2,
        calvingsThisYear: 3,
        breedingEventsThisYear: 4,
        openPregnant: 1,
        byStatus: { Active: 8, Pregnant: 2 },
        bySex: { MALE: 2, FEMALE: 8 },
      })
    ).toBe(true);
  });

  it("returns false for null", () => {
    expect(isValidStats(null)).toBe(false);
  });

  it("returns false for undefined", () => {
    expect(isValidStats(undefined)).toBe(false);
  });

  it("returns false for non-object", () => {
    expect(isValidStats(42)).toBe(false);
    expect(isValidStats("string")).toBe(false);
    expect(isValidStats([])).toBe(false);
  });

  it("returns false when required number fields are missing or wrong type", () => {
    expect(isValidStats({})).toBe(false);
    expect(
      isValidStats({
        totalAnimals: "10",
        dueSoon: 2,
        calvingsThisYear: 3,
        breedingEventsThisYear: 0,
        openPregnant: 1,
        byStatus: {},
        bySex: {},
      })
    ).toBe(false);
    expect(
      isValidStats({
        totalAnimals: 10,
        dueSoon: 2,
        calvingsThisYear: 3,
        breedingEventsThisYear: 0,
        openPregnant: 1,
        byStatus: null,
        bySex: {},
      })
    ).toBe(false);
  });

  it("returns true for empty byStatus and bySex", () => {
    expect(
      isValidStats({
        totalAnimals: 0,
        dueSoon: 0,
        calvingsThisYear: 0,
        breedingEventsThisYear: 0,
        openPregnant: 0,
        byStatus: {},
        bySex: {},
      })
    ).toBe(true);
  });

  it("returns false when byStatus or bySex has non-number values", () => {
    expect(
      isValidStats({
        totalAnimals: 10,
        dueSoon: 0,
        calvingsThisYear: 0,
        breedingEventsThisYear: 0,
        openPregnant: 0,
        byStatus: { Active: 8, Other: "2" },
        bySex: { MALE: 2, FEMALE: 8 },
      } as unknown)
    ).toBe(false);
    expect(
      isValidStats({
        totalAnimals: 10,
        dueSoon: 0,
        calvingsThisYear: 0,
        breedingEventsThisYear: 0,
        openPregnant: 0,
        byStatus: { Active: 8 },
        bySex: { MALE: 2, FEMALE: "8" },
      } as unknown)
    ).toBe(false);
  });

  it("returns false when byStatus or bySex is an array", () => {
    expect(
      isValidStats({
        totalAnimals: 10,
        dueSoon: 0,
        calvingsThisYear: 0,
        breedingEventsThisYear: 0,
        openPregnant: 0,
        byStatus: [],
        bySex: {},
      })
    ).toBe(false);
  });
});
