import type { HerdStats } from "./herdStatsTypes";

/**
 * Type guard for API herd stats. Use when validating /api/stats response.
 */
function isRecordOfNumbers(obj: unknown): obj is Record<string, number> {
  if (obj == null || typeof obj !== "object" || Array.isArray(obj)) return false;
  return Object.values(obj as Record<string, unknown>).every((v) => typeof v === "number");
}

export function isValidStats(data: unknown): data is HerdStats {
  if (data == null || typeof data !== "object") return false;
  const d = data as Record<string, unknown>;
  return (
    typeof d.totalAnimals === "number" &&
    typeof d.dueSoon === "number" &&
    typeof d.calvingsThisYear === "number" &&
    typeof d.breedingEventsThisYear === "number" &&
    typeof d.openPregnant === "number" &&
    isRecordOfNumbers(d.byStatus) &&
    isRecordOfNumbers(d.bySex)
  );
}
