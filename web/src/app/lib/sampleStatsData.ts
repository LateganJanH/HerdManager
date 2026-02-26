/**
 * Sample herd stats used by the stats API and mock data.
 * Single source of truth for the default/sample stats shape.
 */
import type { HerdStats } from "./herdStatsTypes";

export const SAMPLE_STATS_DATA: HerdStats = {
  totalAnimals: 124,
  dueSoon: 5,
  calvingsThisYear: 18,
  breedingEventsThisYear: 24,
  openPregnant: 12,
  byStatus: { Active: 89, Open: 12, Pregnant: 18, Sold: 5 },
  bySex: { MALE: 8, FEMALE: 116 },
};
