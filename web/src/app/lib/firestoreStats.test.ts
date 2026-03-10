import { describe, it, expect } from "vitest";
import type { AnimalDoc } from "./herdStatsFromDocs";
import { computeGrowthKpisFromDocs } from "./firestoreStats";

describe("computeGrowthKpisFromDocs", () => {
  const animals: AnimalDoc[] = [
    { id: "a1", status: "ACTIVE", sex: "FEMALE", dateOfBirth: 20000, isCastrated: false },
    { id: "a2", status: "ACTIVE", sex: "MALE", dateOfBirth: 20000, isCastrated: false },
  ];

  it("computes positive average daily gain when weights increase", () => {
    const weights = [
      { animalId: "a1", dateEpoch: 21000, weightKg: 300 },
      { animalId: "a1", dateEpoch: 21010, weightKg: 320 },
      { animalId: "a2", dateEpoch: 21000, weightKg: 350 },
      { animalId: "a2", dateEpoch: 21020, weightKg: 390 },
    ];

    const kpis = computeGrowthKpisFromDocs(animals, weights, 200);

    expect(kpis.avgDailyGainAllKgPerDay).toBeDefined();
    expect(kpis.avgDailyGainAllKgPerDay!).toBeGreaterThan(0);
    expect(kpis.avgDailyGainBySexKgPerDay).toBeDefined();
    expect(kpis.avgDailyGainBySexKgPerDay!["FEMALE"]).toBeGreaterThan(0);
    expect(kpis.avgDailyGainBySexKgPerDay!["MALE"]).toBeGreaterThan(0);
  });

  it("returns undefined averages when there are no weights", () => {
    const kpis = computeGrowthKpisFromDocs(animals, [], 200);
    expect(kpis.avgDailyGainAllKgPerDay).toBeUndefined();
    expect(kpis.avgDailyGainBySexKgPerDay).toBeUndefined();
    expect(kpis.avgWeaningWeightKg).toBeUndefined();
  });

  it("computes average weaning weight when weaning date has passed", () => {
    const weights = [
      // a1: dateOfBirth=20000, weaningDueEpoch = 20000 + 200 = 20200
      { animalId: "a1", dateEpoch: 20200, weightKg: 210 },
      { animalId: "a1", dateEpoch: 20205, weightKg: 215 },
    ];

    const kpis = computeGrowthKpisFromDocs(animals, weights, 200);

    expect(kpis.avgWeaningWeightKg).toBeDefined();
    // Closest to weaning due date is 210 kg
    expect(kpis.avgWeaningWeightKg).toBeCloseTo(210, 1);
  });
});

