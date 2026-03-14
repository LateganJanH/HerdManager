/**
 * Herd stats aggregation (mirrors web/src/app/lib/herdStatsFromDocs.ts).
 * Pure logic: no Firebase imports; used by getHerdStats callable.
 */

const GESTATION_DAYS = 283;
const DUE_SOON_DAYS = 14;
const CALF_AGE_MONTHS = 12;
const HEIFER_AGE_MONTHS = 24;

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
  pregnancyCheckResult?: string;
  serviceDate?: number;
}

export interface CalvingDoc {
  id: string;
  breedingEventId?: string;
  actualDate?: number;
}

/** Per-alert-type due-soon counts for FCM and API (extend when health/weaning added to job). */
export interface DueSoonBreakdown {
  calvingDue: number;
  /** Reserved for future: pregnancy check due in window. */
  pregnancyCheckDue?: number;
  /** Reserved for future: withdrawal due. */
  withdrawalDue?: number;
  /** Reserved for future: weaning weight due. */
  weaningDue?: number;
}

export interface HerdStats {
  totalAnimals: number;
  dueSoon: number;
  /** Per-type breakdown for notifications and API. */
  dueSoonBreakdown: DueSoonBreakdown;
  calvingsThisYear: number;
  breedingEventsThisYear: number;
  openPregnant: number;
  byStatus: Record<string, number>;
  bySex: Record<string, number>;
  byCategory: {
    Calves: number;
    Heifers: number;
    Cows: number;
    Bulls: number;
    Steers: number;
  };
}

export function computeHerdStatsFromDocs(
  animals: AnimalDoc[],
  breeding: BreedingDoc[],
  calving: CalvingDoc[]
): HerdStats {
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
  let calvingDue = 0;
  breeding.forEach((doc) => {
    if (calvedBreedingIds.has(doc.id)) return;
    const result = doc.pregnancyCheckResult || "";
    if (result === "NOT_PREGNANT") return;
    openPregnant++;
    const serviceDate = doc.serviceDate;
    if (typeof serviceDate !== "number") return;
    const dueEpoch = serviceDate + GESTATION_DAYS;
    if (dueEpoch >= todayEpoch && dueEpoch <= todayEpoch + DUE_SOON_DAYS) calvingDue++;
  });
  const dueSoon = calvingDue; // total; extend with pregnancyCheckDue, withdrawalDue, weaningDue when data available
  const dueSoonBreakdown: DueSoonBreakdown = {
    calvingDue,
    pregnancyCheckDue: 0,
    withdrawalDue: 0,
    weaningDue: 0,
  };

  const calvingsThisYear = calvingDates.filter((epoch) => epoch >= yearStartEpoch).length;

  let breedingEventsThisYear = 0;
  breeding.forEach((doc) => {
    if (typeof doc.serviceDate === "number" && doc.serviceDate >= yearStartEpoch) {
      breedingEventsThisYear++;
    }
  });

  const byStatus: Record<string, number> = {};
  const bySex: Record<string, number> = {};
  animals.forEach((a) => {
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
  animals.forEach((a) => {
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
    totalAnimals: animals.length,
    dueSoon,
    dueSoonBreakdown,
    calvingsThisYear,
    breedingEventsThisYear,
    openPregnant,
    byStatus,
    bySex,
    byCategory,
  };
}

/** Normalize Firestore value (Timestamp, ms, or epoch day) to epoch day. */
export function toEpochDay(value: unknown): number | null {
  if (value == null) return null;
  if (typeof value === "number") {
    if (Number.isNaN(value)) return null;
    if (value > 1e12) return Math.floor(value / 86400_000);
    return Math.floor(value);
  }
  if (
    typeof value === "object" &&
    value !== null &&
    "toDate" in value &&
    typeof (value as { toDate: () => Date }).toDate === "function"
  ) {
    const date = (value as { toDate: () => Date }).toDate();
    return Math.floor(date.getTime() / 86400_000);
  }
  return null;
}
