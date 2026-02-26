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
}
