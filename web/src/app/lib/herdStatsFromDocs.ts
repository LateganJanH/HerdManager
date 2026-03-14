/**
 * Pure aggregation: compute HerdStats from animal/breeding/calving doc data.
 * Used by client (firestoreStats) and server (API route with Firebase Admin).
 */

import type { HerdStats } from "./herdStatsTypes";

const GESTATION_DAYS = 283;
const DUE_SOON_DAYS = 14;
const CALF_AGE_MONTHS = 12;
const HEIFER_AGE_MONTHS = 24;

function isInCurrentHerd(status: string): boolean {
  return status !== "SOLD" && status !== "DECEASED" && status !== "CULLED";
}

function dateToEpochDay(date: Date): number {
  return Math.floor(date.getTime() / 86400_000);
}

function monthsBetweenEpochDays(epochDayStart: number, epochDayEnd: number): number {
  const d = new Date(epochDayStart * 86400_000);
  const t = new Date(epochDayEnd * 86400_000);
  return (t.getFullYear() - d.getFullYear()) * 12 + (t.getMonth() - d.getMonth());
}

export interface AnimalDoc {
  id: string;
  status: string;
  sex: string;
  dateOfBirth: number | null;
  isCastrated?: boolean;
}

export interface BreedingDoc {
  id: string;
  animalId?: string;
  breedingEventId?: string;
  pregnancyCheckResult?: string;
  serviceDate?: number;
}

export interface CalvingDoc {
  id: string;
  breedingEventId?: string;
  actualDate?: number;
}

export function computeHerdStatsFromDocs(
  animals: AnimalDoc[],
  breeding: BreedingDoc[],
  calving: CalvingDoc[]
): HerdStats {
  const activeAnimals = animals.filter((a) => isInCurrentHerd(a.status));
  const activeAnimalIds = new Set(activeAnimals.map((a) => a.id));

  const calvedBreedingIds = new Set<string>();
  const calvingDates: number[] = [];
  calving.forEach((doc) => {
    if (doc.breedingEventId) calvedBreedingIds.add(doc.breedingEventId);
    if (typeof doc.actualDate === "number") calvingDates.push(doc.actualDate);
  });

  const now = new Date();
  const todayEpoch = dateToEpochDay(now);
  const yearStart = new Date(now.getFullYear(), 0, 1);
  const yearStartEpoch = dateToEpochDay(yearStart);

  let openPregnant = 0;
  let dueSoon = 0;
  breeding.forEach((doc) => {
    if (doc.animalId != null && !activeAnimalIds.has(doc.animalId)) return;
    if (calvedBreedingIds.has(doc.id)) return;
    const result = doc.pregnancyCheckResult || "";
    if (result === "NOT_PREGNANT") return;
    openPregnant++;
    const serviceDate = doc.serviceDate;
    if (typeof serviceDate !== "number") return;
    const dueEpoch = serviceDate + GESTATION_DAYS;
    if (dueEpoch >= todayEpoch && dueEpoch <= todayEpoch + DUE_SOON_DAYS) dueSoon++;
  });

  const calvingsThisYear = calvingDates.filter((epoch) => epoch >= yearStartEpoch).length;

  let breedingEventsThisYear = 0;
  breeding.forEach((doc) => {
    if (typeof doc.serviceDate === "number" && doc.serviceDate >= yearStartEpoch) {
      breedingEventsThisYear++;
    }
  });

  const byStatus: Record<string, number> = {};
  const bySex: Record<string, number> = {};
  activeAnimals.forEach((a) => {
    byStatus[a.status] = (byStatus[a.status] ?? 0) + 1;
    bySex[a.sex] = (bySex[a.sex] ?? 0) + 1;
  });

  const byCategory = {
    Calves: 0,
    Heifers: 0,
    Cows: 0,
    Bulls: 0,
    Steers: 0,
  };
  activeAnimals.forEach((a) => {
    const dobEpoch =
      a.dateOfBirth != null
        ? a.dateOfBirth > 1e12
          ? Math.floor(a.dateOfBirth / 86400_000)
          : Math.floor(a.dateOfBirth)
        : null;
    const monthsOld = dobEpoch != null ? monthsBetweenEpochDays(dobEpoch, todayEpoch) : null;
    const isMale = a.sex === "MALE";
    const isFemale = a.sex === "FEMALE";
    const castrated = a.isCastrated === true;
    if (monthsOld != null && monthsOld < CALF_AGE_MONTHS) {
      byCategory.Calves++;
    } else if (isFemale && monthsOld != null) {
      if (monthsOld >= HEIFER_AGE_MONTHS) byCategory.Cows++;
      else byCategory.Heifers++;
    } else if (isMale) {
      if (castrated) byCategory.Steers++;
      else byCategory.Bulls++;
    } else {
      if (monthsOld != null && monthsOld < HEIFER_AGE_MONTHS) byCategory.Heifers++;
      else if (isFemale) byCategory.Cows++;
      else byCategory.Bulls++;
    }
  });

  return {
    totalAnimals: activeAnimals.length,
    dueSoon,
    calvingsThisYear,
    breedingEventsThisYear,
    openPregnant,
    byStatus,
    bySex,
    byCategory,
  };
}
