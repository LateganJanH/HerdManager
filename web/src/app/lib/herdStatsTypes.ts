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
}
