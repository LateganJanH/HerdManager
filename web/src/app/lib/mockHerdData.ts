/**
 * Mock herd data for web dashboard development.
 * Replace with API/Firebase when backend is connected.
 */

import { SAMPLE_STATS_DATA } from "./sampleStatsData";

export interface HerdStats {
  totalAnimals: number;
  dueSoon: number;
  calvingsThisYear: number;
  breedingEventsThisYear: number;
  openPregnant: number;
  byStatus: Record<string, number>;
  bySex: Record<string, number>;
}

export interface AlertItem {
  id: string;
  type: "calving" | "pregnancy_check" | "withdrawal";
  earTag: string;
  dueOrCheckDate: string;
  daysUntil: number;
  /** Product/medication name for withdrawal alerts. */
  product?: string;
}

export interface AnalyticsSeries {
  label: string;
  value: number;
  color?: string;
}

/** A single farm contact (name, phone, email). */
export interface FarmContact {
  name: string;
  phone: string;
  email: string;
}

/** Farm profile (settings) synced from Firestore; matches Android FarmSettings. */
export interface FarmProfile {
  id: string;
  name: string;
  address: string;
  /** Multiple contacts. Replaces legacy contactPhone/contactEmail. */
  contacts: FarmContact[];
  calvingAlertDays: number;
  pregnancyCheckDaysAfterBreeding: number;
  /** Gestation length in days (250–320); used for due date from service date. */
  gestationDays?: number;
}

export interface AnimalProfile {
  id: string;
  earTag: string;
  status: string;
  sex: string;
}

/** Breeding event for animal detail (from Firestore). */
export interface AnimalBreedingEvent {
  id: string;
  serviceDate: string; // YYYY-MM-DD
  eventType: string;
  pregnancyCheckResult?: string;
  dueDate?: string;
}

/** Calving event (dam) for animal detail. */
export interface AnimalCalvingEvent {
  id: string;
  actualDate: string;
  calfId?: string;
  calfSex?: string;
  assistanceRequired?: boolean;
}

/** Health event for animal detail. */
export interface AnimalHealthEvent {
  id: string;
  date: string;
  eventType: string;
  product?: string;
  notes?: string;
}

/** Photo with optional URL (from Firebase Storage) for display on web/other devices. */
export interface AnimalDetailPhoto {
  id: string;
  url?: string; // storageUrl or http uri when available
  angle?: string;
}

/** Full animal detail for Profiles modal (Firestore + derived). */
export interface AnimalDetail {
  id: string;
  earTag: string;
  status: string;
  sex: string;
  name?: string;
  breed?: string;
  dateOfBirth?: string; // YYYY-MM-DD
  herdName?: string;
  /** Pedigree (Phase 2): parent animal IDs and display labels. */
  sireId?: string;
  damId?: string;
  sireEarTag?: string;
  damEarTag?: string;
  breedingEvents: AnimalBreedingEvent[];
  calvingEvents: AnimalCalvingEvent[];
  healthEvents: AnimalHealthEvent[];
  photoCount: number;
  /** Photo URLs for display when synced via Firebase Storage. */
  photos: AnimalDetailPhoto[];
}

const MOCK_STATS: HerdStats = {
  totalAnimals: 0,
  dueSoon: 0,
  calvingsThisYear: 0,
  breedingEventsThisYear: 0,
  openPregnant: 0,
  byStatus: {},
  bySex: {},
};

/** Use sample data when no sync is configured (for demo). */
const SAMPLE_STATS: HerdStats = SAMPLE_STATS_DATA;

const SAMPLE_ALERTS: AlertItem[] = [
  { id: "1", type: "calving", earTag: "A001", dueOrCheckDate: "2026-03-01", daysUntil: 9 },
  { id: "2", type: "calving", earTag: "A012", dueOrCheckDate: "2026-03-05", daysUntil: 13 },
  { id: "3", type: "pregnancy_check", earTag: "A034", dueOrCheckDate: "2026-02-25", daysUntil: 5 },
  { id: "4", type: "withdrawal", earTag: "A001", dueOrCheckDate: "2026-03-10", daysUntil: 13, product: "Example antibiotic" },
];

const SAMPLE_ANIMALS: AnimalProfile[] = [
  { id: "1", earTag: "A001", status: "Pregnant", sex: "FEMALE" },
  { id: "2", earTag: "A012", status: "Pregnant", sex: "FEMALE" },
  { id: "3", earTag: "A034", status: "Open", sex: "FEMALE" },
  { id: "4", earTag: "A056", status: "Active", sex: "FEMALE" },
  { id: "5", earTag: "B002", status: "Active", sex: "MALE" },
  { id: "6", earTag: "A078", status: "Pregnant", sex: "FEMALE" },
  { id: "7", earTag: "A089", status: "Active", sex: "FEMALE" },
  { id: "8", earTag: "B011", status: "Active", sex: "MALE" },
];

/** Whether to show sample data. Set hm-hide-sample in localStorage to disable. */
function useSample(): boolean {
  if (typeof window === "undefined") return true; // SSR: match client default
  return !localStorage.getItem("hm-hide-sample");
}

export function getMockHerdStats(): HerdStats {
  return useSample() ? SAMPLE_STATS : MOCK_STATS;
}

export function getMockAlerts(): AlertItem[] {
  return useSample() ? SAMPLE_ALERTS : [];
}

export function getMockAnimalProfiles(): AnimalProfile[] {
  return useSample() ? SAMPLE_ANIMALS : [];
}

export function getMockAnalyticsSeries(): AnalyticsSeries[] {
  if (!useSample()) return [];
  return [
    { label: "Active", value: 89, color: "var(--color-primary, #1b4332)" },
    { label: "Open", value: 12, color: "var(--color-accent-warning, #e6b800)" },
    { label: "Pregnant", value: 18, color: "var(--color-accent-pregnancy, #3d5a80)" },
    { label: "Calvings (YTD)", value: 18 },
  ];
}

/** Mock events-by-month for Analytics "This year by month" when using sample data. */
export function getMockEventsByMonth(): { calvingsByMonth: number[]; breedingByMonth: number[] } {
  if (!useSample()) {
    return { calvingsByMonth: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], breedingByMonth: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] };
  }
  const m = new Date().getMonth();
  const calvingsByMonth = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
  const breedingByMonth = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
  calvingsByMonth[m] = 4;
  calvingsByMonth[Math.max(0, m - 1)] = 6;
  calvingsByMonth[Math.max(0, m - 2)] = 8;
  breedingByMonth[m] = 5;
  breedingByMonth[Math.max(0, m - 1)] = 7;
  breedingByMonth[Math.max(0, m - 2)] = 12;
  return { calvingsByMonth, breedingByMonth };
}

export function isSampleDataEnabled(): boolean {
  return useSample();
}

const SAMPLE_CHANGED_EVENT = "hm-sample-changed";

export function setSampleDataEnabled(enabled: boolean): void {
  if (typeof window === "undefined") return;
  if (enabled) {
    localStorage.removeItem("hm-hide-sample");
  } else {
    localStorage.setItem("hm-hide-sample", "1");
  }
  window.dispatchEvent(new Event(SAMPLE_CHANGED_EVENT));
}

export function onSampleDataChange(callback: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  window.addEventListener(SAMPLE_CHANGED_EVENT, callback);
  return () => window.removeEventListener(SAMPLE_CHANGED_EVENT, callback);
}
