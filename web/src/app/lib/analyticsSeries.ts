import type { HerdStats } from "./herdStatsTypes";
import type { AnalyticsSeries } from "./mockHerdData";

export const STATUS_COLORS: Record<string, string> = {
  Active: "var(--color-primary, #1b4332)",
  Open: "var(--color-accent-warning, #e6b800)",
  Pregnant: "var(--color-accent-pregnancy, #3d5a80)",
  Sold: "var(--color-text-muted, #4a4339)",
};

/**
 * Build chart series from herd stats: byStatus entries first, then bySex with "Sex: " prefix.
 */
export function seriesFromStats(stats: HerdStats): AnalyticsSeries[] {
  const byStatus = Object.entries(stats.byStatus).map(([label, value]) => ({
    label,
    value,
    color: STATUS_COLORS[label] ?? "var(--color-primary, #1b4332)",
  }));
  const bySex = Object.entries(stats.bySex).map(([label, value]) => ({
    label: `Sex: ${label}`,
    value,
  }));
  return [...byStatus, ...bySex];
}
