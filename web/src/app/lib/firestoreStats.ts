/**
 * Fetch herd stats, animal list, and alerts from Firestore (users/{uid}/...) to match Android sync layout.
 * Used when the web user is signed in so the dashboard shows real synced data.
 * Uses dynamic import so the app can run without the firebase package when not configured.
 */

import type { HerdStats } from "./herdStatsTypes";
import {
  computeHerdStatsFromDocs,
  type AnimalDoc,
  type BreedingDoc,
  type CalvingDoc,
} from "./herdStatsFromDocs";
import type {
  AnimalProfile,
  AlertItem,
  AnimalDetail,
  AnimalDetailPhoto,
  AnimalBreedingEvent,
  AnimalCalvingEvent,
  AnimalHealthEvent,
  AnimalWeightRecord,
  FarmProfile,
  FarmContact,
} from "./mockHerdData";

const GESTATION_DAYS = 283;
const DUE_SOON_DAYS = 14;

function dateToEpochDay(date: Date): number {
  return Math.floor(date.getTime() / 86400_000);
}

function epochDayToYyyyMmDd(epochDay: number): string {
  const date = new Date(epochDay * 86400_000);
  const y = date.getUTCFullYear();
  const m = String(date.getUTCMonth() + 1).padStart(2, "0");
  const d = String(date.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** Firestore stores dateOfBirth as epoch day (or ms); normalize to YYYY-MM-DD. */
function storedDateToYyyyMmDd(value: number | undefined): string | undefined {
  if (value == null || typeof value !== "number") return undefined;
  const epochDay = value > 1e12 ? Math.floor(value / 86400_000) : value;
  return epochDayToYyyyMmDd(epochDay);
}

export async function fetchHerdStatsFromFirestore(
  db: unknown,
  uid: string
): Promise<HerdStats> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const animalsRef = firestore.collection(d, "users", uid, "animals");
  const breedingRef = firestore.collection(d, "users", uid, "breeding_events");
  const calvingRef = firestore.collection(d, "users", uid, "calving_events");
  const weightsRef = firestore.collection(d, "users", uid, "weight_records");
  const conditionRef = firestore.collection(d, "users", uid, "condition_records");

  const [animalsSnap, breedingSnap, calvingSnap, weightSnap, conditionSnap, settings] = await Promise.all([
    firestore.getDocs(animalsRef),
    firestore.getDocs(breedingRef),
    firestore.getDocs(calvingRef),
    firestore.getDocs(weightsRef),
    firestore.getDocs(conditionRef).catch(() => ({ docs: [] })),
    fetchFarmSettingsFromFirestore(db, uid),
  ]);

  const animals: AnimalDoc[] = animalsSnap.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      status: (data.status as string) || "ACTIVE",
      sex: (data.sex as string) || "FEMALE",
      dateOfBirth: data.dateOfBirth != null && typeof data.dateOfBirth === "number" ? data.dateOfBirth : null,
      isCastrated: data.isCastrated as boolean | undefined,
    };
  });

  const breeding: BreedingDoc[] = breedingSnap.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      animalId: data.animalId as string | undefined,
      breedingEventId: data.breedingEventId as string | undefined,
      pregnancyCheckResult: data.pregnancyCheckResult as string | undefined,
      serviceDate: data.serviceDate as number | undefined,
    };
  });

  const calving: CalvingDoc[] = calvingSnap.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      breedingEventId: data.breedingEventId as string | undefined,
      actualDate: data.actualDate as number | undefined,
    };
  });

  const weightRecords = weightSnap.docs
    .map((doc) => {
      const data = doc.data();
      const animalId = data.animalId as string | undefined;
      const dateEpoch = toEpochDay(data.date);
      const weightKg = typeof data.weightKg === "number" ? data.weightKg : undefined;
      if (!animalId || dateEpoch == null || typeof weightKg !== "number" || Number.isNaN(weightKg)) return null;
      return { animalId, dateEpoch, weightKg };
    })
    .filter((r): r is { animalId: string; dateEpoch: number; weightKg: number } => r != null);

  const activeAnimals = animals.filter(
    (a) => a.status !== "SOLD" && a.status !== "DECEASED" && a.status !== "CULLED"
  );
  const activeAnimalIds = new Set(activeAnimals.map((a) => a.id));
  const earTagByAnimalId = new Map<string, string>();
  animalsSnap.docs.forEach((doc) => {
    const data = doc.data();
    earTagByAnimalId.set(doc.id, (data.earTagNumber as string) ?? doc.id);
  });

  const baseStats = computeHerdStatsFromDocs(animals, breeding, calving);
  const growthKpis = computeGrowthKpisFromDocs(
    activeAnimals,
    weightRecords,
    settings?.weaningAgeDays ?? DEFAULT_WEANING_AGE_DAYS
  );

  const conditionRecords: { animalId: string; score: number }[] = [];
  const conditionDocs = "docs" in conditionSnap ? conditionSnap.docs : [];
  conditionDocs.forEach((doc: { data: () => Record<string, unknown> }) => {
    const data = doc.data();
    const animalId = data.animalId as string | undefined;
    const score = typeof data.score === "number" ? data.score : undefined;
    if (animalId && score != null && score >= 1 && score <= 9 && activeAnimalIds.has(animalId)) {
      conditionRecords.push({ animalId, score });
    }
  });
  const avgConditionScore =
    conditionRecords.length > 0
      ? conditionRecords.reduce((s, r) => s + r.score, 0) / conditionRecords.length
      : undefined;
  const bcsDistribution: Record<number, number> = {};
  for (let i = 1; i <= 9; i++) bcsDistribution[i] = conditionRecords.filter((r) => r.score === i).length;

  const weaningAgeDays = settings?.weaningAgeDays ?? DEFAULT_WEANING_AGE_DAYS;
  const todayEpoch = dateToEpochDay(new Date());
  const OPEN_COW_DAYS = 60;
  const CALF_AGE_MONTHS = 12;
  const HEIFER_AGE_MONTHS = 24;
  const breedingByAnimal = new Map<string, number>();
  breeding.forEach((b) => {
    if (b.animalId == null) return;
    const sd = b.serviceDate;
    if (typeof sd !== "number") return;
    const cur = breedingByAnimal.get(b.animalId);
    if (cur == null || sd > cur) breedingByAnimal.set(b.animalId, sd);
  });
  function category(a: AnimalDoc): string {
    if (a.dateOfBirth == null) return "Cows";
    const dobEpoch = a.dateOfBirth > 1e12 ? Math.floor(a.dateOfBirth / 86400_000) : a.dateOfBirth;
    const monthsOld = Math.floor((todayEpoch - dobEpoch) / 30.44);
    if (monthsOld < CALF_AGE_MONTHS) return "Calves";
    if (a.sex === "FEMALE") return monthsOld >= HEIFER_AGE_MONTHS ? "Cows" : "Heifers";
    return a.isCastrated === true ? "Steers" : "Bulls";
  }
  const weightsByAnimalEpochs = new Map<string, number[]>();
  weightRecords.forEach((w) => {
    const list = weightsByAnimalEpochs.get(w.animalId) ?? [];
    list.push(w.dateEpoch);
    weightsByAnimalEpochs.set(w.animalId, list);
  });

  const atRiskPreview: { animalId: string; earTag: string; reason: string }[] = [];
  activeAnimals.forEach((a) => {
    const cat = category(a);
    if ((cat === "Cows" || cat === "Heifers") && a.sex === "FEMALE") {
      const lastBreeding = breedingByAnimal.get(a.id);
      const daysSince = lastBreeding != null ? todayEpoch - lastBreeding : 9999;
      if (daysSince > OPEN_COW_DAYS) {
        atRiskPreview.push({
          animalId: a.id,
          earTag: earTagByAnimalId.get(a.id) ?? a.id,
          reason: `No breeding in ${OPEN_COW_DAYS} days`,
        });
      }
    }
    if (a.dateOfBirth == null) return;
    const dobEpoch = a.dateOfBirth > 1e12 ? Math.floor(a.dateOfBirth / 86400_000) : a.dateOfBirth;
    const weaningDueEpoch = dobEpoch + weaningAgeDays;
    if (weaningDueEpoch >= todayEpoch) return;
    const weightDates = weightsByAnimalEpochs.get(a.id) ?? [];
    const hasWeight = weightDates.some(
      (epoch) => epoch >= weaningDueEpoch - WEANING_ALERT_WINDOW_DAYS
    );
    if (!hasWeight) {
      atRiskPreview.push({
        animalId: a.id,
        earTag: earTagByAnimalId.get(a.id) ?? a.id,
        reason: "Weaning weight overdue",
      });
    }
  });

  // Low-growth at risk: animals whose gain per day (from last two weights) is well below herd average.
  const gainPerDayByAnimalId = new Map<string, number>();
  const weightsByAnimalForGain = new Map<string, { dateEpoch: number; weightKg: number }[]>();
  weightRecords.forEach((w) => {
    const list = weightsByAnimalForGain.get(w.animalId) ?? [];
    list.push(w);
    weightsByAnimalForGain.set(w.animalId, list);
  });
  weightsByAnimalForGain.forEach((records, animalId) => {
    if (records.length < 2) return;
    const sorted = records.slice().sort((a, b) => a.dateEpoch - b.dateEpoch);
    const last = sorted[sorted.length - 1];
    const prev = sorted[sorted.length - 2];
    const days = Math.max(1, last.dateEpoch - prev.dateEpoch);
    const gain = last.weightKg - prev.weightKg;
    const gainPerDay = gain / days;
    gainPerDayByAnimalId.set(animalId, gainPerDay);
  });
  const allGains = Array.from(gainPerDayByAnimalId.values());
  const avgDailyGainAll =
    allGains.length > 0 ? allGains.reduce((a, b) => a + b, 0) / allGains.length : undefined;
  if (avgDailyGainAll != null && avgDailyGainAll > 0) {
    const target = avgDailyGainAll * 0.7;
    activeAnimals.forEach((a) => {
      const gain = gainPerDayByAnimalId.get(a.id);
      if (gain == null || gain >= target) return;
      atRiskPreview.push({
        animalId: a.id,
        earTag: earTagByAnimalId.get(a.id) ?? a.id,
        reason: `Low growth (${gain.toFixed(2)} kg/day)`,
      });
    });
  }

  return {
    ...baseStats,
    ...growthKpis,
    ...(avgConditionScore != null && { avgConditionScore }),
    ...(Object.values(bcsDistribution).some((c) => c > 0) && { bcsDistribution }),
    ...(atRiskPreview.length > 0 && { atRiskPreview: atRiskPreview.slice(0, 5) }),
  };
}

export function computeGrowthKpisFromDocs(
  animals: AnimalDoc[],
  weightRecords: { animalId: string; dateEpoch: number; weightKg: number }[],
  weaningAgeDays: number
): Pick<HerdStats, "avgDailyGainAllKgPerDay" | "avgDailyGainBySexKgPerDay" | "avgWeaningWeightKg" | "weaningWeightSamplesKg"> {
  if (animals.length === 0 || weightRecords.length === 0) {
    return {
      avgDailyGainAllKgPerDay: undefined,
      avgDailyGainBySexKgPerDay: undefined,
      avgWeaningWeightKg: undefined,
      weaningWeightSamplesKg: undefined,
    };
  }

  const todayEpoch = dateToEpochDay(new Date());
  const weightsByAnimal = new Map<string, { dateEpoch: number; weightKg: number }[]>();
  weightRecords.forEach((w) => {
    const list = weightsByAnimal.get(w.animalId) ?? [];
    list.push(w);
    weightsByAnimal.set(w.animalId, list);
  });

  // Per-animal gain per day using the last two weights
  const gainPerDayByAnimalId = new Map<string, number>();
  weightsByAnimal.forEach((records, animalId) => {
    if (records.length < 2) return;
    const sorted = records.slice().sort((a, b) => a.dateEpoch - b.dateEpoch);
    const last = sorted[sorted.length - 1];
    const prev = sorted[sorted.length - 2];
    const days = Math.max(1, last.dateEpoch - prev.dateEpoch);
    const gain = last.weightKg - prev.weightKg;
    const gainPerDay = gain / days;
    gainPerDayByAnimalId.set(animalId, gainPerDay);
  });

  const allGains = Array.from(gainPerDayByAnimalId.values());
  const avgDailyGainAllKgPerDay = allGains.length > 0 ? allGains.reduce((a, b) => a + b, 0) / allGains.length : undefined;

  const avgDailyGainBySexKgPerDay: Record<string, number> = {};
  const gainsBySex = new Map<string, number[]>();
  animals.forEach((a) => {
    const gain = gainPerDayByAnimalId.get(a.id);
    if (gain == null) return;
    const list = gainsBySex.get(a.sex) ?? [];
    list.push(gain);
    gainsBySex.set(a.sex, list);
  });
  gainsBySex.forEach((list, sex) => {
    if (list.length > 0) {
      avgDailyGainBySexKgPerDay[sex] = list.reduce((a, b) => a + b, 0) / list.length;
    }
  });

  // Weaning weights for calves whose weaning date has passed
  const weaningWeights: number[] = [];
  animals.forEach((a) => {
    if (a.dateOfBirth == null) return;
    const dobEpoch = a.dateOfBirth > 1e12 ? Math.floor(a.dateOfBirth / 86400_000) : Math.floor(a.dateOfBirth);
    const weaningDueEpoch = dobEpoch + weaningAgeDays;
    if (weaningDueEpoch > todayEpoch) return;
    const records = weightsByAnimal.get(a.id) ?? [];
    if (records.length === 0) return;
    const cutoff = weaningDueEpoch + WEANING_ALERT_WINDOW_DAYS;
    const candidates = records.filter((r) => r.dateEpoch <= cutoff);
    if (candidates.length === 0) return;
    const closest = candidates.reduce((best, r) => {
      const bestDiff = Math.abs(best.dateEpoch - weaningDueEpoch);
      const diff = Math.abs(r.dateEpoch - weaningDueEpoch);
      return diff < bestDiff ? r : best;
    });
    weaningWeights.push(closest.weightKg);
  });
  const avgWeaningWeightKg =
    weaningWeights.length > 0 ? weaningWeights.reduce((a, b) => a + b, 0) / weaningWeights.length : undefined;

  // For very large herds, keep a simple sampled subset so payload size stays reasonable.
  const MAX_WEANING_SAMPLES = 200;
  let weaningWeightSamplesKg: number[] | undefined;
  if (weaningWeights.length > 0) {
    if (weaningWeights.length <= MAX_WEANING_SAMPLES) {
      weaningWeightSamplesKg = weaningWeights.slice();
    } else {
      // Reservoir-style uniform sample
      const step = weaningWeights.length / MAX_WEANING_SAMPLES;
      const sampled: number[] = [];
      for (let i = 0; i < MAX_WEANING_SAMPLES; i++) {
        const idx = Math.floor(i * step);
        sampled.push(weaningWeights[idx]);
      }
      weaningWeightSamplesKg = sampled;
    }
  }

  return {
    avgDailyGainAllKgPerDay,
    avgDailyGainBySexKgPerDay: Object.keys(avgDailyGainBySexKgPerDay).length > 0 ? avgDailyGainBySexKgPerDay : undefined,
    avgWeaningWeightKg,
    weaningWeightSamplesKg,
  };
}

/** Events by month for current year (1–12). For Analytics charts. */
export type EventsByMonth = {
  calvingsByMonth: number[];
  breedingByMonth: number[];
};

/** Normalize stored date (epoch day, ms, or Firestore Timestamp) to epoch day integer. */
function toEpochDay(value: unknown): number | undefined {
  if (value == null) return undefined;
  if (typeof value === "number") {
    if (Number.isNaN(value)) return undefined;
    if (value > 1e12) return Math.floor(value / 86400_000);
    return Math.floor(value);
  }
  if (typeof value === "object" && value !== null && "toDate" in value && typeof (value as { toDate: () => Date }).toDate === "function") {
    const date = (value as { toDate: () => Date }).toDate();
    return Math.floor(date.getTime() / 86400_000);
  }
  if (typeof value === "string") {
    const n = Number(value);
    if (!Number.isNaN(n)) return toEpochDay(n);
  }
  return undefined;
}

/** Fetch calving and breeding event counts by month for the current year. */
export async function fetchEventsByMonthFromFirestore(
  db: unknown,
  uid: string
): Promise<EventsByMonth> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const breedingRef = firestore.collection(d, "users", uid, "breeding_events");
  const calvingRef = firestore.collection(d, "users", uid, "calving_events");
  const [breedingSnap, calvingSnap] = await Promise.all([
    firestore.getDocs(breedingRef),
    firestore.getDocs(calvingRef),
  ]);

  const now = new Date();
  const year = now.getFullYear();
  const yearStartEpochDay = dateToEpochDay(new Date(year, 0, 1));

  const calvingsByMonth = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
  const breedingByMonth = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];

  calvingSnap.docs.forEach((doc) => {
    const epochDay = toEpochDay(doc.data().actualDate);
    if (epochDay == null || epochDay < yearStartEpochDay) return;
    const date = new Date(epochDay * 86400_000);
    if (date.getUTCFullYear() !== year) return;
    calvingsByMonth[date.getUTCMonth()]++;
  });

  breedingSnap.docs.forEach((doc) => {
    const epochDay = toEpochDay(doc.data().serviceDate);
    if (epochDay == null || epochDay < yearStartEpochDay) return;
    const date = new Date(epochDay * 86400_000);
    if (date.getUTCFullYear() !== year) return;
    breedingByMonth[date.getUTCMonth()]++;
  });

  return { calvingsByMonth, breedingByMonth };
}

/** Fetch animal list for Profiles page. */
export async function fetchAnimalsFromFirestore(
  db: unknown,
  uid: string
): Promise<AnimalProfile[]> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.collection(d, "users", uid, "animals");
  const snap = await firestore.getDocs(ref);
  return snap.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      earTag: (data.earTagNumber as string) ?? "",
      status: (data.status as string) ?? "ACTIVE",
      sex: (data.sex as string) ?? "FEMALE",
    };
  });
}

/** Fetch alerts (calving due, pregnancy check, withdrawal period end, weaning weight due) for Alerts page. */
export async function fetchAlertsFromFirestore(
  db: unknown,
  uid: string
): Promise<AlertItem[]> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const [animalsSnap, breedingSnap, calvingSnap, healthSnap, settings, weightSnap] = await Promise.all([
    firestore.getDocs(firestore.collection(d, "users", uid, "animals")),
    firestore.getDocs(firestore.collection(d, "users", uid, "breeding_events")),
    firestore.getDocs(firestore.collection(d, "users", uid, "calving_events")),
    firestore.getDocs(firestore.collection(d, "users", uid, "health_events")),
    fetchFarmSettingsFromFirestore(db, uid),
    firestore.getDocs(firestore.collection(d, "users", uid, "weight_records")),
  ]);
  const earTagByAnimalId = new Map<string, string>();
  const dobEpochByAnimalId = new Map<string, number>();
  const activeAnimalIds = new Set<string>();
  animalsSnap.docs.forEach((doc) => {
    const data = doc.data();
    const status = (data.status as string) ?? "ACTIVE";
    if (status !== "SOLD" && status !== "DECEASED" && status !== "CULLED") {
      activeAnimalIds.add(doc.id);
    }
    earTagByAnimalId.set(doc.id, (data.earTagNumber as string) ?? doc.id);
    const dob = toEpochDay(data.dateOfBirth);
    if (dob != null) dobEpochByAnimalId.set(doc.id, dob);
  });
  const weightsByAnimalId = new Map<string, number[]>();
  weightSnap.docs.forEach((doc) => {
    const data = doc.data();
    const animalId = data.animalId as string | undefined;
    const dateEpoch = toEpochDay(data.date);
    if (!animalId || dateEpoch == null) return;
    const list = weightsByAnimalId.get(animalId) ?? [];
    list.push(dateEpoch);
    weightsByAnimalId.set(animalId, list);
  });
  const weaningAgeDays = Math.max(
    WEANING_AGE_DAYS_MIN,
    Math.min(WEANING_AGE_DAYS_MAX, settings?.weaningAgeDays ?? DEFAULT_WEANING_AGE_DAYS)
  );
  const calvedBreedingIds = new Set<string>();
  calvingSnap.docs.forEach((doc) => {
    const breedingId = doc.data().breedingEventId as string | undefined;
    if (breedingId) calvedBreedingIds.add(breedingId);
  });

  const now = new Date();
  const todayEpoch = dateToEpochDay(now);
  const items: AlertItem[] = [];

  breedingSnap.docs.forEach((doc) => {
    const data = doc.data();
    const animalId = data.animalId as string | undefined;
    if (animalId != null && !activeAnimalIds.has(animalId)) return;
    if (calvedBreedingIds.has(doc.id)) return;
    const result = (data.pregnancyCheckResult as string) || "";
    if (result === "NOT_PREGNANT") return;
    const serviceDate = data.serviceDate as number | undefined;
    if (typeof serviceDate !== "number") return;
    const dueEpoch = serviceDate + GESTATION_DAYS;
    if (dueEpoch < todayEpoch || dueEpoch > todayEpoch + DUE_SOON_DAYS) return;
    const earTag = earTagByAnimalId.get(animalId ?? "") ?? "—";
    items.push({
      id: doc.id,
      type: "calving",
      earTag,
      dueOrCheckDate: epochDayToYyyyMmDd(dueEpoch),
      daysUntil: dueEpoch - todayEpoch,
    });

    const pregnancyCheckEpoch = data.pregnancyCheckDateEpochDay as number | undefined;
    if (typeof pregnancyCheckEpoch === "number" && pregnancyCheckEpoch >= todayEpoch) {
      const pcEarTag = earTagByAnimalId.get(data.animalId as string) ?? "—";
      items.push({
        id: `${doc.id}-pc`,
        type: "pregnancy_check",
        earTag: pcEarTag,
        dueOrCheckDate: epochDayToYyyyMmDd(pregnancyCheckEpoch),
        daysUntil: pregnancyCheckEpoch - todayEpoch,
      });
    }
  });

  healthSnap.docs.forEach((doc) => {
    const data = doc.data();
    const animalId = data.animalId as string | undefined;
    if (animalId != null && !activeAnimalIds.has(animalId)) return;
    const endEpoch = toEpochDay(data.withdrawalPeriodEnd);
    if (endEpoch == null || endEpoch < todayEpoch || endEpoch > todayEpoch + DUE_SOON_DAYS) return;
    const earTag = earTagByAnimalId.get(animalId ?? "") ?? "—";
    items.push({
      id: `wd-${doc.id}`,
      type: "withdrawal",
      earTag,
      dueOrCheckDate: epochDayToYyyyMmDd(endEpoch),
      daysUntil: endEpoch - todayEpoch,
      product: (data.product as string) || undefined,
    });
  });

  dobEpochByAnimalId.forEach((dobEpoch, animalId) => {
    if (!activeAnimalIds.has(animalId)) return;
    const weaningDueEpoch = dobEpoch + weaningAgeDays;
    if (weaningDueEpoch < todayEpoch - WEANING_OVERDUE_DAYS || weaningDueEpoch > todayEpoch + WEANING_ALERT_WINDOW_DAYS) return;
    const weightDates = weightsByAnimalId.get(animalId) ?? [];
    const hasWeightInWindow = weightDates.some((epoch) => epoch >= weaningDueEpoch - WEANING_ALERT_WINDOW_DAYS);
    if (hasWeightInWindow) return;
    const earTag = earTagByAnimalId.get(animalId) ?? "—";
    items.push({
      id: `weaning-${animalId}`,
      type: "weaning_weight",
      earTag,
      dueOrCheckDate: epochDayToYyyyMmDd(weaningDueEpoch),
      daysUntil: weaningDueEpoch - todayEpoch,
    });
  });

  return items.sort((a, b) => a.daysUntil - b.daysUntil);
}

/** Fetch full animal detail for Profiles modal (one animal + events + herd name + photo count). */
export async function fetchAnimalDetailFromFirestore(
  db: unknown,
  uid: string,
  animalId: string
): Promise<AnimalDetail | null> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const animalRef = firestore.doc(d, "users", uid, "animals", animalId);

  const [animalSnap, herdsSnap, breedingSnap, calvingSnap, healthSnap, photosSnap, weightSnap] =
    await Promise.all([
      firestore.getDoc(animalRef),
      firestore.getDocs(firestore.collection(d, "users", uid, "herds")),
      firestore.getDocs(
        firestore.query(
          firestore.collection(d, "users", uid, "breeding_events"),
          firestore.where("animalId", "==", animalId)
        )
      ),
      firestore.getDocs(
        firestore.query(
          firestore.collection(d, "users", uid, "calving_events"),
          firestore.where("damId", "==", animalId)
        )
      ),
      firestore.getDocs(
        firestore.query(
          firestore.collection(d, "users", uid, "health_events"),
          firestore.where("animalId", "==", animalId)
        )
      ),
      firestore.getDocs(
        firestore.query(
          firestore.collection(d, "users", uid, "photos"),
          firestore.where("animalId", "==", animalId)
        )
      ),
      firestore.getDocs(
        firestore.query(
          firestore.collection(d, "users", uid, "weight_records"),
          firestore.where("animalId", "==", animalId)
        )
      ),
    ]);

  if (!animalSnap.exists()) return null;
  const data = animalSnap.data()!;

  const sireId = (data.sireId as string) || undefined;
  const damId = (data.damId as string) || undefined;
  let sireEarTag: string | undefined;
  let damEarTag: string | undefined;
  if (sireId || damId) {
    const animalsRef = firestore.collection(d, "users", uid, "animals");
    if (sireId) {
      const sireSnap = await firestore.getDoc(firestore.doc(animalsRef, sireId));
      if (sireSnap.exists()) sireEarTag = (sireSnap.data()?.earTagNumber as string) ?? undefined;
    }
    if (damId) {
      const damSnap = await firestore.getDoc(firestore.doc(animalsRef, damId));
      if (damSnap.exists()) damEarTag = (damSnap.data()?.earTagNumber as string) ?? undefined;
    }
  }

  const herdId = (data.currentHerdId as string) ?? undefined;
  const herds = new Map<string, string>();
  herdsSnap.docs.forEach((doc) => {
    const name = doc.data()?.name as string | undefined;
    if (name) herds.set(doc.id, name);
  });
  const herdName = herdId ? herds.get(herdId) : undefined;

  const breedingEvents: AnimalBreedingEvent[] = breedingSnap.docs.map((doc) => {
    const b = doc.data();
    const serviceDate = (b.serviceDate as number) ?? 0;
    const dueDate = serviceDate + GESTATION_DAYS;
    return {
      id: doc.id,
      serviceDate: epochDayToYyyyMmDd(serviceDate),
      eventType: (b.eventType as string) ?? "AI",
      pregnancyCheckResult: b.pregnancyCheckResult as string | undefined,
      dueDate: epochDayToYyyyMmDd(dueDate),
    };
  });
  breedingEvents.sort((a, b) => b.serviceDate.localeCompare(a.serviceDate));

  const calvingEvents: AnimalCalvingEvent[] = calvingSnap.docs.map((doc) => {
    const c = doc.data();
    const actual = (c.actualDate as number) ?? 0;
    return {
      id: doc.id,
      actualDate: epochDayToYyyyMmDd(actual),
      calfId: c.calfId as string | undefined,
      calfSex: c.calfSex as string | undefined,
      assistanceRequired: c.assistanceRequired as boolean | undefined,
    };
  });
  calvingEvents.sort((a, b) => b.actualDate.localeCompare(a.actualDate));

  const healthEvents: AnimalHealthEvent[] = healthSnap.docs.map((doc) => {
    const h = doc.data();
    const date = (h.date as number) ?? 0;
    return {
      id: doc.id,
      date: storedDateToYyyyMmDd(date) ?? "—",
      eventType: (h.eventType as string) ?? "",
      product: h.product as string | undefined,
      notes: h.notes as string | undefined,
    };
  });
  healthEvents.sort((a, b) => b.date.localeCompare(a.date));

  const weightRecords: AnimalWeightRecord[] = weightSnap.docs.map((doc) => {
    const w = doc.data();
    const dateEpoch = (w.date as number) ?? 0;
    const dateStr = epochDayToYyyyMmDd(dateEpoch > 1e12 ? Math.floor(dateEpoch / 86400_000) : dateEpoch);
    return {
      id: doc.id,
      date: dateStr,
      weightKg: typeof w.weightKg === "number" ? w.weightKg : 0,
      note: (w.note as string) || null,
    };
  });
  weightRecords.sort((a, b) => b.date.localeCompare(a.date));

  const dateOfBirth = (data.dateOfBirth as number) ?? undefined;

  const photos: AnimalDetailPhoto[] = photosSnap.docs.map((doc) => {
    const p = doc.data();
    const url = (p.storageUrl as string) || ((p.uri as string)?.startsWith("http") ? (p.uri as string) : undefined);
    return { id: doc.id, url, angle: p.angle as string | undefined };
  });

  return {
    id: animalSnap.id,
    earTag: (data.earTagNumber as string) ?? "",
    status: (data.status as string) ?? "ACTIVE",
    sex: (data.sex as string) ?? "FEMALE",
    name: (data.name as string) || undefined,
    breed: (data.breed as string) || undefined,
    dateOfBirth: storedDateToYyyyMmDd(dateOfBirth),
    herdName,
    sireId,
    damId,
    sireEarTag,
    damEarTag,
    breedingEvents,
    calvingEvents,
    healthEvents,
    weightRecords,
    photoCount: photosSnap.size,
    photos,
  };
}

/** YYYY-MM-DD to epoch day for Firestore weight_records (matches Android). */
function yyyyMmDdToEpochDay(yyyyMmDd: string): number {
  const [y, m, d] = yyyyMmDd.split("-").map(Number);
  const date = new Date(Date.UTC(y, (m ?? 1) - 1, d ?? 1));
  return Math.floor(date.getTime() / 86400_000);
}

/** Add a weight record to Firestore. Returns the new document id. */
export async function addWeightRecordToFirestore(
  db: unknown,
  uid: string,
  animalId: string,
  payload: { date: string; weightKg: number; note?: string | null }
): Promise<string> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const col = firestore.collection(d, "users", uid, "weight_records");
  const docRef = await firestore.addDoc(col, {
    animalId,
    date: yyyyMmDdToEpochDay(payload.date),
    weightKg: payload.weightKg,
    note: payload.note?.trim() || null,
    createdAt: Date.now(),
    updatedAt: Date.now(),
  });
  return docRef.id;
}

/** Update a weight record in Firestore. */
export async function updateWeightRecordInFirestore(
  db: unknown,
  uid: string,
  recordId: string,
  payload: { date: string; weightKg: number; note?: string | null }
): Promise<void> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.doc(d, "users", uid, "weight_records", recordId);
  await firestore.updateDoc(ref, {
    date: yyyyMmDdToEpochDay(payload.date),
    weightKg: payload.weightKg,
    note: payload.note?.trim() || null,
    updatedAt: Date.now(),
  });
}

/** Delete a weight record from Firestore. */
export async function deleteWeightRecordFromFirestore(
  db: unknown,
  uid: string,
  recordId: string
): Promise<void> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.doc(d, "users", uid, "weight_records", recordId);
  await firestore.deleteDoc(ref);
}

const DEFAULT_CALVING_ALERT_DAYS = 14;
const DEFAULT_PREGNANCY_DAYS = 28;
const DEFAULT_GESTATION_DAYS = 283;
const DEFAULT_WEANING_AGE_DAYS = 200;
const CALVING_ALERT_MIN = 1;
const CALVING_ALERT_MAX = 90;
const PREGNANCY_DAYS_MIN = 14;
const PREGNANCY_DAYS_MAX = 60;
const GESTATION_DAYS_MIN = 250;
const GESTATION_DAYS_MAX = 320;
const WEANING_AGE_DAYS_MIN = 150;
const WEANING_AGE_DAYS_MAX = 300;
const WEANING_ALERT_WINDOW_DAYS = 14;
const WEANING_OVERDUE_DAYS = 30;

/** Fetch farm profile (settings) from Firestore. Synced from Android; same data for all devices. */
export async function fetchFarmSettingsFromFirestore(
  db: unknown,
  uid: string
): Promise<FarmProfile | null> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.doc(d, "users", uid, "settings", "farm");
  const snap = await firestore.getDoc(ref);
  if (!snap.exists()) return null;
  const data = snap.data()!;
  const calving = (data.calvingAlertDays as number | undefined) ?? DEFAULT_CALVING_ALERT_DAYS;
  const pregnancy =
    (data.pregnancyCheckDaysAfterBreeding as number | undefined) ?? DEFAULT_PREGNANCY_DAYS;
  const gestation = (data.gestationDays as number | undefined) ?? DEFAULT_GESTATION_DAYS;
  const weaning =
    (data.weaningAgeDays as number | undefined) ?? DEFAULT_WEANING_AGE_DAYS;
  const contacts = parseContactsFromFirestore(data.contacts, data.contactPhone, data.contactEmail);
  const currencyCode = (data.currencyCode as string)?.trim() || "ZAR";
  return {
    id: (data.id as string) ?? "farm",
    name: (data.name as string) ?? "",
    address: (data.address as string) ?? "",
    contacts,
    currencyCode,
    calvingAlertDays: Math.max(
      CALVING_ALERT_MIN,
      Math.min(CALVING_ALERT_MAX, typeof calving === "number" ? calving : DEFAULT_CALVING_ALERT_DAYS)
    ),
    pregnancyCheckDaysAfterBreeding: Math.max(
      PREGNANCY_DAYS_MIN,
      Math.min(
        PREGNANCY_DAYS_MAX,
        typeof pregnancy === "number" ? pregnancy : DEFAULT_PREGNANCY_DAYS
      )
    ),
    gestationDays: Math.max(
      GESTATION_DAYS_MIN,
      Math.min(GESTATION_DAYS_MAX, typeof gestation === "number" ? gestation : DEFAULT_GESTATION_DAYS)
    ),
    weaningAgeDays: Math.max(
      WEANING_AGE_DAYS_MIN,
      Math.min(WEANING_AGE_DAYS_MAX, typeof weaning === "number" ? weaning : DEFAULT_WEANING_AGE_DAYS)
    ),
  };
}

export function parseContactsFromFirestore(
  contactsValue: unknown,
  legacyPhone?: unknown,
  legacyEmail?: unknown
): FarmContact[] {
  if (Array.isArray(contactsValue) && contactsValue.length > 0) {
    return contactsValue.map((c: { name?: string; phone?: string; email?: string }) => ({
      name: (c.name as string) ?? "",
      phone: (c.phone as string) ?? "",
      email: (c.email as string) ?? "",
    }));
  }
  const p = typeof legacyPhone === "string" ? legacyPhone : "";
  const e = typeof legacyEmail === "string" ? legacyEmail : "";
  if (p || e) return [{ name: "", phone: p, email: e }];
  return [];
}

/** Save farm profile to Firestore. Android and other devices will receive it on next sync. */
export async function saveFarmSettingsToFirestore(
  db: unknown,
  uid: string,
  farm: FarmProfile
): Promise<void> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.doc(d, "users", uid, "settings", "farm");
  const calving = Math.max(CALVING_ALERT_MIN, Math.min(CALVING_ALERT_MAX, farm.calvingAlertDays));
  const pregnancy = Math.max(
    PREGNANCY_DAYS_MIN,
    Math.min(PREGNANCY_DAYS_MAX, farm.pregnancyCheckDaysAfterBreeding)
  );
  const gestation = Math.max(
    GESTATION_DAYS_MIN,
    Math.min(GESTATION_DAYS_MAX, farm.gestationDays ?? DEFAULT_GESTATION_DAYS)
  );
  await firestore.setDoc(
    ref,
    {
      id: farm.id || "farm",
      name: farm.name ?? "",
      address: farm.address ?? "",
      contacts: (farm.contacts ?? []).filter(
        (c) => (c.name ?? "").trim() || (c.phone ?? "").trim() || (c.email ?? "").trim()
      ).map((c) => ({
        name: (c.name ?? "").trim(),
        phone: (c.phone ?? "").trim(),
        email: (c.email ?? "").trim(),
      })),
      calvingAlertDays: calving,
      pregnancyCheckDaysAfterBreeding: pregnancy,
      gestationDays: gestation,
      weaningAgeDays: Math.max(
        WEANING_AGE_DAYS_MIN,
        Math.min(WEANING_AGE_DAYS_MAX, farm.weaningAgeDays ?? DEFAULT_WEANING_AGE_DAYS)
      ),
      currencyCode: (farm.currencyCode ?? "ZAR").trim() || "ZAR",
      updatedAt: Date.now(),
    },
    { merge: true }
  );
}
