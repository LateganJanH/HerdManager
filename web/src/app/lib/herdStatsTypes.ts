/**
 * Shared HerdStats type to avoid circular dependency between mockHerdData and sampleStatsData.
 */

export interface HerdStatsCategory {
  Calves: number;
  Heifers: number;
  Cows: number;
  Bulls: number;
  Steers: number;
}

export interface HerdStats {
  totalAnimals: number;
  dueSoon: number;
  calvingsThisYear: number;
  breedingEventsThisYear: number;
  openPregnant: number;
  byStatus: Record<string, number>;
  bySex: Record<string, number>;
  /** Breakdown by cattle category (age + sex + castration). Optional when not computed. */
  byCategory?: HerdStatsCategory;
  /** Average daily gain (kg/day) across all animals with at least two weight records. */
  avgDailyGainAllKgPerDay?: number;
  /** Average daily gain (kg/day) grouped by sex (e.g. MALE, FEMALE). */
  avgDailyGainBySexKgPerDay?: Record<string, number>;
  /** Average weaning weight (kg) for calves with a recorded weight near the weaning age. */
  avgWeaningWeightKg?: number;
  /** Raw weaning weights (kg) used to compute the average. Optional and may be sampled for large herds. */
  weaningWeightSamplesKg?: number[];
  /** Average body condition score (1–9) for current herd. */
  avgConditionScore?: number;
  /** Count per BCS score 1–9 for current herd. */
  bcsDistribution?: Record<number, number>;
  /** At-risk items: open cows (no breeding in 60 days), overdue weaning weight. */
  atRiskPreview?: { animalId: string; earTag: string; reason: string }[];
}
