"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback, useMemo } from "react";
import { jsPDF } from "jspdf";
import { seriesFromStats } from "../lib/analyticsSeries";
import { formatRelativeTime } from "../lib/formatRelativeTime";
import { getMockAnalyticsSeries, getMockEventsByMonth, isSampleDataEnabled } from "../lib/mockHerdData";
import { useHerdStats } from "../lib/useHerdStats";
import { useAnalyticsByMonth } from "../lib/useAnalyticsByMonth";
import { APP_NAME } from "../lib/version";
import type { HerdStats } from "../lib/herdStatsTypes";
import type { AnalyticsSeries } from "../lib/mockHerdData";

const MONTH_LABELS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

/** Y-axis scale: at least 1, or next "nice" step (5, 10, …). Returns max value and tick values. */
function barScale(counts: number[]): { scaleMax: number; ticks: number[] } {
  const maxCount = Math.max(...counts, 1);
  if (maxCount <= 5) {
    const scaleMax = 5;
    return { scaleMax, ticks: [0, 1, 2, 3, 4, 5] };
  }
  const scaleMax = Math.ceil(maxCount / 5) * 5;
  const step = scaleMax <= 10 ? 1 : scaleMax / 5;
  const ticks: number[] = [];
  for (let v = 0; v <= scaleMax; v += step) ticks.push(v);
  if (ticks[ticks.length - 1] !== scaleMax) ticks.push(scaleMax);
  return { scaleMax, ticks };
}

function downloadCsvReport(stats: HerdStats, series: AnalyticsSeries[]) {
  const rows: string[][] = [];
  rows.push(["Herd summary", ""]);
  rows.push(["Total animals", String(stats.totalAnimals)]);
  rows.push(["Due soon", String(stats.dueSoon)]);
  rows.push(["Calvings this year", String(stats.calvingsThisYear)]);
  rows.push(["Breeding events this year", String(stats.breedingEventsThisYear)]);
  rows.push(["Open / pregnant", String(stats.openPregnant)]);
  rows.push([]);
  rows.push(["By status", "Count"]);
  for (const [name, count] of Object.entries(stats.byStatus)) {
    rows.push([name, String(count)]);
  }
  rows.push([]);
  rows.push(["By sex", "Count"]);
  for (const [name, count] of Object.entries(stats.bySex)) {
    rows.push([name, String(count)]);
  }
  if (stats.byCategory) {
    rows.push([]);
    rows.push(["By category", "Count"]);
    rows.push(["Calves", String(stats.byCategory.Calves)]);
    rows.push(["Heifers", String(stats.byCategory.Heifers)]);
    rows.push(["Cows", String(stats.byCategory.Cows)]);
    rows.push(["Bulls", String(stats.byCategory.Bulls)]);
    rows.push(["Steers", String(stats.byCategory.Steers)]);
  }
  if (series.length > 0) {
    rows.push([]);
    rows.push(["Analytics (by status)", "Value"]);
    for (const item of series) {
      rows.push([item.label, String(item.value)]);
    }
  }
  const csv = rows.map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(",")).join("\r\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `herd-report-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function downloadPdfReport(stats: HerdStats, series: AnalyticsSeries[]) {
  const doc = new jsPDF();
  const margin = 20;
  let y = margin;
  const lineHeight = 7;

  doc.setFontSize(16);
  doc.text("Herd report", margin, y);
  y += lineHeight + 4;
  doc.text(new Date().toISOString().slice(0, 10), margin, y);
  y += lineHeight + 6;

  doc.setFontSize(11);
  doc.text(`Total animals: ${stats.totalAnimals}`, margin, y);
  y += lineHeight;
  doc.text(`Due soon: ${stats.dueSoon}`, margin, y);
  y += lineHeight;
  doc.text(`Calvings this year: ${stats.calvingsThisYear}`, margin, y);
  y += lineHeight;
  doc.text(`Breeding events this year: ${stats.breedingEventsThisYear}`, margin, y);
  y += lineHeight;
  doc.text(`Open / pregnant: ${stats.openPregnant}`, margin, y);
  y += lineHeight + 4;

  doc.setFont("helvetica", "bold");
  doc.text("By status", margin, y);
  y += lineHeight;
  doc.setFont("helvetica", "normal");
  for (const [name, count] of Object.entries(stats.byStatus)) {
    doc.text(`${name}: ${count}`, margin + 5, y);
    y += lineHeight;
  }
  y += 2;

  doc.setFont("helvetica", "bold");
  doc.text("By sex", margin, y);
  y += lineHeight;
  doc.setFont("helvetica", "normal");
  for (const [name, count] of Object.entries(stats.bySex)) {
    doc.text(`${name}: ${count}`, margin + 5, y);
    y += lineHeight;
  }
  if (stats.byCategory) {
    y += 2;
    doc.setFont("helvetica", "bold");
    doc.text("By category", margin, y);
    y += lineHeight;
    doc.setFont("helvetica", "normal");
    doc.text(`Calves: ${stats.byCategory.Calves}`, margin + 5, y);
    y += lineHeight;
    doc.text(`Heifers: ${stats.byCategory.Heifers}`, margin + 5, y);
    y += lineHeight;
    doc.text(`Cows: ${stats.byCategory.Cows}`, margin + 5, y);
    y += lineHeight;
    doc.text(`Bulls: ${stats.byCategory.Bulls}`, margin + 5, y);
    y += lineHeight;
    doc.text(`Steers: ${stats.byCategory.Steers}`, margin + 5, y);
    y += lineHeight;
  }
  if (series.length > 0) {
    y += 4;
    doc.setFont("helvetica", "bold");
    doc.text("Analytics (by status)", margin, y);
    y += lineHeight;
    doc.setFont("helvetica", "normal");
    for (const item of series) {
      doc.text(`${item.label}: ${item.value}`, margin + 5, y);
      y += lineHeight;
    }
  }

  doc.save(`herd-report-${new Date().toISOString().slice(0, 10)}.pdf`);
}

export function DashboardAnalytics() {
  const pathname = usePathname();
  const { stats, fromApi, loading, isError, dataUpdatedAt, refetch } = useHerdStats();
  const { data: eventsByMonth, loading: eventsByMonthLoading } = useAnalyticsByMonth();
  const sample = isSampleDataEnabled();
  const eventsByMonthForCharts = useMemo(() => {
    if (fromApi) {
      if (eventsByMonth) return eventsByMonth;
      if (!eventsByMonthLoading) {
        return { calvingsByMonth: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], breedingByMonth: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0] };
      }
      return null;
    }
    if (sample) return getMockEventsByMonth();
    return null;
  }, [fromApi, sample, eventsByMonth, eventsByMonthLoading]);
  const showByMonthSection = fromApi ? (eventsByMonth != null || !eventsByMonthLoading) : sample;
  const updatedLabel = useMemo(
    () => (fromApi && dataUpdatedAt > 0 ? formatRelativeTime(dataUpdatedAt) : null),
    [fromApi, dataUpdatedAt]
  );
  const series = useMemo(() => {
    if (fromApi && (stats.totalAnimals > 0 || Object.keys(stats.byStatus).length > 0 || Object.keys(stats.bySex).length > 0)) {
      return seriesFromStats(stats);
    }
    return getMockAnalyticsSeries();
  }, [fromApi, stats]);
  const maxValue = Math.max(...series.map((s) => s.value), 1);
  const handleExport = useCallback(() => {
    downloadCsvReport(stats, series);
  }, [stats, series]);
  const handleExportPdf = useCallback(() => {
    downloadPdfReport(stats, series);
  }, [stats, series]);
  const dataSourceLabel = loading ? "Loading…" : fromApi ? "From API" : sample ? "Sample data" : "Connect app to sync";

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
      <h2 className="text-xl font-semibold text-stone-900 dark:text-stone-100">
        Analytics
      </h2>
      <p className="text-stone-600 dark:text-stone-300 text-base">
        Herd summaries and status breakdown. Data visualisation for quick reading on mobile and desktop.
      </p>
      {loading && (
        <p className="text-sm text-stone-500 dark:text-stone-400" aria-live="polite">
          Loading stats…
        </p>
      )}
      {isError && !fromApi && (
        <div
          className="rounded-card border border-accent-alert/50 bg-accent-alert-bg dark:bg-stone-800/80 px-4 py-3 flex flex-wrap items-center justify-between gap-2"
          role="alert"
        >
          <p className="text-sm text-stone-700 dark:text-stone-300">
            Couldn’t load latest stats. Showing cached or sample data.
          </p>
          <button
            type="button"
            onClick={() => refetch()}
            className="rounded-button border border-stone-300 dark:border-stone-600 px-3 py-1.5 text-sm font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            Retry
          </button>
        </div>
      )}
      <div className="flex flex-wrap items-center gap-2">
        <p className="text-sm text-stone-500 dark:text-stone-400">
          Data: {dataSourceLabel}
          {updatedLabel != null && ` · Last updated ${updatedLabel}`}
        </p>
        <button
          type="button"
          onClick={() => refetch()}
          disabled={loading}
          className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded disabled:opacity-60"
          aria-label="Refresh stats"
        >
          Refresh
        </button>
      </div>

      {series.length > 0 ? (
        <section
          className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
          aria-label="Status and sex breakdown chart"
        >
          <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-4">
            By status &amp; sex
          </h3>
          <div className="space-y-3">
            {series.map((item) => (
              <div key={item.label} className="flex items-center gap-3">
                <span className="w-24 text-sm font-medium text-stone-700 dark:text-stone-300 shrink-0">
                  {item.label}
                </span>
                <div className="flex-1 h-6 rounded-full bg-stone-200 dark:bg-stone-600 overflow-hidden min-w-[40px]">
                  <div
                    className="h-full rounded-full transition-all duration-500"
                    style={{
                      width: `${(item.value / maxValue) * 100}%`,
                      backgroundColor: item.color || "var(--color-primary, #1b4332)",
                    }}
                    role="presentation"
                  />
                </div>
                <span className="text-sm font-medium text-stone-900 dark:text-stone-100 w-8 text-right">
                  {item.value}
                </span>
              </div>
            ))}
          </div>
          {!fromApi && (sample ? (
            <p className="mt-3 text-xs text-stone-500 dark:text-stone-400">
              Sample data — connect app to see live stats
            </p>
          ) : (
            <p className="mt-3 text-xs text-stone-500 dark:text-stone-400">
              Connect the {APP_NAME} Android app and sync to see analytics here.
            </p>
          )          )}
        </section>
      ) : !loading ? (
        <section
          className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
          aria-label="Status breakdown"
        >
          <p className="text-stone-600 dark:text-stone-300 text-base">
            No breakdown data yet. Sync herd data or enable sample data in Settings to see status and sex charts.
          </p>
          <p className="mt-3">
            <Link
              href={`${pathname || "/"}?tab=home`}
              className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
            >
              View dashboard
            </Link>
          </p>
        </section>
      ) : null}

      {stats.byCategory && (
        <section
          className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
          aria-label="Herd by category breakdown"
        >
          <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-4">
            By category
          </h3>
          <p className="text-sm text-stone-600 dark:text-stone-400 mb-4">
            Calves, heifers, cows, bulls, and steers (age + sex + castration).
          </p>
          <div className="space-y-3">
            {(["Calves", "Heifers", "Cows", "Bulls", "Steers"] as const).map((label) => {
              const value = stats.byCategory![label];
              const categoryMax = Math.max(
                stats.byCategory!.Calves,
                stats.byCategory!.Heifers,
                stats.byCategory!.Cows,
                stats.byCategory!.Bulls,
                stats.byCategory!.Steers,
                1
              );
              return (
                <div key={label} className="flex items-center gap-3">
                  <span className="w-20 text-sm font-medium text-stone-700 dark:text-stone-300 shrink-0">
                    {label}
                  </span>
                  <div className="flex-1 h-6 rounded-full bg-stone-200 dark:bg-stone-600 overflow-hidden min-w-[40px]">
                    <div
                      className="h-full rounded-full transition-all duration-500 bg-primary/90 dark:bg-primary/80"
                      style={{
                        width: `${(value / categoryMax) * 100}%`,
                      }}
                      role="presentation"
                    />
                  </div>
                  <span className="text-sm font-medium text-stone-900 dark:text-stone-100 w-8 text-right">
                    {value}
                  </span>
                </div>
              );
            })}
          </div>
          {!fromApi && (sample ? (
            <p className="mt-3 text-xs text-stone-500 dark:text-stone-400">
              Sample data — connect app to see live category breakdown
            </p>
          ) : (
            <p className="mt-3 text-xs text-stone-500 dark:text-stone-400">
              Synced from the {APP_NAME} app when animals have date of birth and sex.
            </p>
          ))}
        </section>
      )}

      {showByMonthSection && (
        <section
          className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
          aria-label="Events this year by month"
        >
          <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-4">
            This year by month
          </h3>
          {fromApi && eventsByMonthLoading ? (
            <p className="text-sm text-stone-500 dark:text-stone-400">Loading…</p>
          ) : eventsByMonthForCharts ? (
            <div className="space-y-8">
              <div>
                <h4 className="text-sm font-medium text-stone-700 dark:text-stone-300 mb-3">Calvings</h4>
                {(() => {
                  const counts = eventsByMonthForCharts.calvingsByMonth;
                  const { scaleMax, ticks } = barScale(counts);
                  return (
                    <div className="flex gap-3 items-stretch">
                      <div
                        className="flex flex-col justify-between text-[10px] text-stone-500 dark:text-stone-400 pt-0.5 pb-6 shrink-0 w-6 text-right"
                        aria-hidden
                      >
                        {ticks.slice().reverse().map((t) => (
                          <span key={t}>{t}</span>
                        ))}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-end gap-1.5 h-28" role="img" aria-label="Calvings per month">
                          {counts.map((count, i) => {
                            const barHeightPct = scaleMax > 0 ? (count / scaleMax) * 100 : 0;
                            return (
                              <div
                                key={i}
                                className="flex-1 min-w-0 flex flex-col items-center justify-end h-full"
                                title={`${MONTH_LABELS[i]}: ${count}`}
                              >
                                <div
                                  className="w-full rounded-t bg-primary/80 dark:bg-primary/70 transition-all flex-shrink-0"
                                  style={{
                                    height: `${barHeightPct}%`,
                                    minHeight: count > 0 ? 6 : 0,
                                  }}
                                  aria-hidden
                                />
                                <span className="text-[10px] text-stone-500 dark:text-stone-400 truncate w-full text-center mt-1.5 flex-shrink-0">
                                  {MONTH_LABELS[i]}
                                </span>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  );
                })()}
                <p className="text-xs text-stone-500 dark:text-stone-400 mt-2">
                  Total this year: {eventsByMonthForCharts.calvingsByMonth.reduce((a, b) => a + b, 0)}
                </p>
              </div>
              <div>
                <h4 className="text-sm font-medium text-stone-700 dark:text-stone-300 mb-3">Breeding events</h4>
                {(() => {
                  const counts = eventsByMonthForCharts.breedingByMonth;
                  const { scaleMax, ticks } = barScale(counts);
                  return (
                    <div className="flex gap-3 items-stretch">
                      <div
                        className="flex flex-col justify-between text-[10px] text-stone-500 dark:text-stone-400 pt-0.5 pb-6 shrink-0 w-6 text-right"
                        aria-hidden
                      >
                        {ticks.slice().reverse().map((t) => (
                          <span key={t}>{t}</span>
                        ))}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-end gap-1.5 h-28" role="img" aria-label="Breeding events per month">
                          {counts.map((count, i) => {
                            const barHeightPct = scaleMax > 0 ? (count / scaleMax) * 100 : 0;
                            return (
                              <div
                                key={i}
                                className="flex-1 min-w-0 flex flex-col items-center justify-end h-full"
                                title={`${MONTH_LABELS[i]}: ${count}`}
                              >
                                <div
                                  className="w-full rounded-t bg-stone-500/80 dark:bg-stone-400/70 transition-all flex-shrink-0"
                                  style={{
                                    height: `${barHeightPct}%`,
                                    minHeight: count > 0 ? 6 : 0,
                                  }}
                                  aria-hidden
                                />
                                <span className="text-[10px] text-stone-500 dark:text-stone-400 truncate w-full text-center mt-1.5 flex-shrink-0">
                                  {MONTH_LABELS[i]}
                                </span>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  );
                })()}
                <p className="text-xs text-stone-500 dark:text-stone-400 mt-2">
                  Total this year: {eventsByMonthForCharts.breedingByMonth.reduce((a, b) => a + b, 0)}
                </p>
              </div>
            </div>
          ) : null}
          {fromApi && !eventsByMonthLoading && eventsByMonthForCharts && (
            <p className="mt-3 text-xs text-stone-500 dark:text-stone-400">
              Synced from the {APP_NAME} app. Data updates when you sync.
            </p>
          )}
          {sample && eventsByMonthForCharts && (
            <p className="mt-3 text-xs text-stone-500 dark:text-stone-400">
              Sample data — connect app and sync to see real events by month.
            </p>
          )}
        </section>
      )}

      <section className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card">
        <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-2">
          Summary
        </h3>
        <p className="text-stone-600 dark:text-stone-300 text-base">
          Total animals: <strong>{stats.totalAnimals}</strong>
          {stats.totalAnimals > 0 && (
            <> · Due soon: <strong>{stats.dueSoon}</strong> · Open/pregnant: <strong>{stats.openPregnant}</strong></>
          )}
        </p>
        <p className="text-stone-600 dark:text-stone-300 text-base mt-1">
          This year: <strong>{stats.breedingEventsThisYear}</strong> breeding events · <strong>{stats.calvingsThisYear}</strong> calvings
        </p>
        {!fromApi && !sample && (
          <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">
            Connect the {APP_NAME} Android app and sync to see analytics here.
          </p>
        )}
      </section>

      <section className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card">
        <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-2">
          Export
        </h3>
        <p className="text-stone-600 dark:text-stone-300 text-base mb-3">
          Download herd summary and analytics as CSV or PDF. Uses current dashboard data (sample or synced).
        </p>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={handleExport}
            className="rounded-button border border-stone-300 dark:border-stone-600 px-4 py-2 text-sm font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            Export report (CSV)
          </button>
          <button
            type="button"
            onClick={handleExportPdf}
            className="rounded-button border border-stone-300 dark:border-stone-600 px-4 py-2 text-sm font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            Export report (PDF)
          </button>
        </div>
      </section>
    </div>
  );
}
