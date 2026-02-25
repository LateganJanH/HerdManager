"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useMemo } from "react";
import { formatRelativeTime } from "../lib/formatRelativeTime";
import { isSampleDataEnabled } from "../lib/mockHerdData";
import { useHerdStats } from "../lib/useHerdStats";
import { useAlerts } from "../lib/useAlerts";
import { APP_NAME } from "../lib/version";

const DUE_SOON_PREVIEW_MAX = 3;

function dueSoonLabel(type: "calving" | "pregnancy_check" | "withdrawal", daysUntil: number): string {
  if (daysUntil < 0) {
    if (type === "calving") return `Calving overdue by ${-daysUntil} days`;
    if (type === "pregnancy_check") return `Pregnancy check overdue by ${-daysUntil} days`;
    return `Withdrawal overdue by ${-daysUntil} days`;
  }
  if (daysUntil === 0) {
    if (type === "calving") return "Calving due today";
    if (type === "pregnancy_check") return "Pregnancy check due today";
    return "Withdrawal ends today";
  }
  if (type === "calving") return `Calving in ${daysUntil} days`;
  if (type === "pregnancy_check") return `Pregnancy check in ${daysUntil} days`;
  return `Withdrawal ends in ${daysUntil} days`;
}

export function DashboardHome() {
  const pathname = usePathname();
  const { stats, fromApi, loading, isError, dataUpdatedAt, refetch } = useHerdStats();
  const { alerts } = useAlerts();
  const dueSoonPreview = useMemo(
    () => [...alerts].sort((a, b) => a.daysUntil - b.daysUntil).slice(0, DUE_SOON_PREVIEW_MAX),
    [alerts]
  );
  const dueSoonCount = alerts.length > 0 ? alerts.length : stats.dueSoon;
  const sample = isSampleDataEnabled();
  const sourceLabel = fromApi ? "From API" : sample ? "Sample data" : "Connect app to sync";
  const updatedLabel = useMemo(
    () => (fromApi && dataUpdatedAt > 0 ? formatRelativeTime(dataUpdatedAt) : null),
    [fromApi, dataUpdatedAt]
  );

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
      {/* Hero – gradient and pattern; optional farm/hero image can be added when available */}
      <section
        className="relative rounded-card-lg overflow-hidden bg-gradient-to-br from-primary to-primary-dark text-white min-h-[160px] flex items-end p-6"
        aria-label="Overview"
      >
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNiI+PHBhdGggZD0iTTM2IDM0djItSDI0di0yaDEyem0wLTR2Mkg0MnYtMkgzNnptLTYgNnYyaC0ydi0yaDJ6bTAtNHYyaC0ydi0yaDJ6Ii8+PC9nPjwvZz48L3N2Zz4=')] opacity-40" aria-hidden />
        <div className="absolute top-4 right-4 z-10 flex items-center gap-2">
          {loading && (
            <span className="text-sm text-white/80" aria-live="polite">
              Loading…
            </span>
          )}
          <button
            type="button"
            onClick={() => refetch()}
            disabled={loading}
            className="rounded-lg px-3 py-2 text-sm font-medium text-white/90 bg-white/10 hover:bg-white/20 focus:outline-none focus-visible:ring-2 focus-visible:ring-white/50 disabled:opacity-60 disabled:pointer-events-none"
            aria-label="Refresh stats"
          >
            Refresh
          </button>
        </div>
        <div className="relative z-10">
          <h2 className="text-2xl font-bold tracking-tight">
            Your herd at a glance
          </h2>
          <p className="mt-1 text-white/90 text-readable-lg">
            Modern cattle management for the field
          </p>
        </div>
      </section>

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

      {/* Horizontal scroll – farm/herd selections (card strip) */}
      <section aria-label="Quick stats">
        <div className="flex gap-3 overflow-x-auto pb-2 -mx-4 px-4 sm:mx-0 sm:px-0 scrollbar-hide">
          {loading ? (
            <>
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  className="flex-shrink-0 w-[280px] rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-4 shadow-card animate-pulse"
                  aria-hidden
                >
                  <div className="h-4 w-24 rounded bg-stone-200 dark:bg-stone-600" />
                  <div className="mt-2 h-8 w-16 rounded bg-stone-200 dark:bg-stone-600" />
                  <div className="mt-2 h-4 w-32 rounded bg-stone-200 dark:bg-stone-600" />
                </div>
              ))}
            </>
          ) : (
            <>
              <div className="flex-shrink-0 w-[280px] rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-4 shadow-card hover:shadow-card-hover transition-shadow">
                <p className="text-sm font-medium text-stone-500 dark:text-stone-400">Total animals</p>
                <p className="mt-1 text-2xl font-semibold text-primary">
                  {stats.totalAnimals > 0 ? stats.totalAnimals : "—"}
                </p>
                <p className="text-sm text-stone-600 dark:text-stone-300">
                  {sourceLabel}
                  {updatedLabel && <> · {updatedLabel}</>}
                </p>
              </div>
              <div className="flex-shrink-0 w-[280px] rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-4 shadow-card hover:shadow-card-hover transition-shadow">
                <p className="text-sm font-medium text-stone-500 dark:text-stone-400">Due soon</p>
                <p className="mt-1 text-2xl font-semibold text-accent-alert">
                  {dueSoonCount > 0 ? dueSoonCount : "—"}
                </p>
                <p className="text-sm text-stone-600 dark:text-stone-300">Calving, checks & withdrawal</p>
                {dueSoonCount > 0 && (
                  <>
                    {dueSoonPreview.length > 0 && (
                      <ul className="mt-2 space-y-1 text-sm text-stone-700 dark:text-stone-300" aria-label="Upcoming alerts preview">
                        {dueSoonPreview.map((a) => (
                          <li key={a.id}>
                            {a.earTag} – {dueSoonLabel(a.type, a.daysUntil)}
                          </li>
                        ))}
                      </ul>
                    )}
                    <p className="mt-2">
                      <Link
                        href={`${pathname || "/"}?tab=alerts`}
                        className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
                      >
                        View Alerts →
                      </Link>
                    </p>
                  </>
                )}
              </div>
              <div className="flex-shrink-0 w-[280px] rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-4 shadow-card hover:shadow-card-hover transition-shadow">
                <p className="text-sm font-medium text-stone-500 dark:text-stone-400">Calvings (YTD)</p>
                <p className="mt-1 text-2xl font-semibold text-accent-success">
                  {stats.calvingsThisYear > 0 ? stats.calvingsThisYear : "—"}
                </p>
                <p className="text-sm text-stone-600 dark:text-stone-300">This year</p>
              </div>
            </>
          )}
        </div>
      </section>

      {/* Status cards – dashboard overview (by status / reproduction) */}
      <section className="grid gap-4 sm:grid-cols-2" aria-label="Status overview">
        <div className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card">
          <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100">
            By status
          </h3>
          {Object.keys(stats.byStatus).length > 0 ? (
            <ul className="mt-2 space-y-1 text-base text-stone-700 dark:text-stone-300">
              {Object.entries(stats.byStatus).map(([name, count]) => (
                <li key={name} className="flex justify-between">
                  <span>{name}</span>
                  <span className="font-medium text-primary">{count}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
              Status breakdown will appear here once data is synced.
            </p>
          )}
        </div>
        <div className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card">
          <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100">
            Reproduction
          </h3>
          <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
            Calvings this year: <strong className="text-primary">{stats.calvingsThisYear}</strong>
            <br />
            Open / pregnant: <strong className="text-primary">{stats.openPregnant}</strong>
          </p>
        </div>
      </section>

      {/* Analytics preview – charts / progress (compact dashboard) */}
      <section className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card" aria-label="Analytics">
        <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          Analytics
        </h3>
        {stats.totalAnimals > 0 ? (
          <>
            <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
              Herd data available. Full trends and reports will appear here as more data is synced.
            </p>
            <div className="mt-4 h-2 rounded-full bg-stone-200 dark:bg-stone-600 overflow-hidden" role="progressbar" aria-valuenow={100} aria-valuemin={0} aria-valuemax={100} aria-label="Data coverage">
              <div className="h-full rounded-full bg-primary transition-all duration-500" style={{ width: "100%" }} />
            </div>
            <p className="mt-3">
              <Link
                href={`${pathname || "/"}?tab=analytics`}
                className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
              >
                View full Analytics →
              </Link>
            </p>
          </>
        ) : (
          <>
            <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
              Herd trends, yield tracking, and visual reports will be available here. Sync with the app to enable.
            </p>
            <div className="mt-4 h-2 rounded-full bg-stone-200 dark:bg-stone-600 overflow-hidden" role="progressbar" aria-valuenow={0} aria-valuemin={0} aria-valuemax={100} aria-label="Data coverage">
              <div className="h-full w-0 rounded-full bg-primary transition-all duration-500" style={{ width: "0%" }} />
            </div>
          </>
        )}
      </section>

      {/* Connect herd CTA – when no real data or to promote app */}
      <section
        className="rounded-card-lg border-2 border-dashed border-primary/30 bg-primary/5 dark:bg-primary/10 p-6"
        aria-label="Connect your herd"
      >
        <h3 className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          Connect your herd
        </h3>
        {!sample && stats.totalAnimals === 0 ? (
          <>
            <p className="mt-2 text-stone-700 dark:text-stone-300 text-base">
              Use the {APP_NAME} Android app to register animals, log breeding and health, and sync data to this dashboard. Offline-first for the field.
            </p>
            <p className="mt-2 text-sm text-stone-600 dark:text-stone-400">
              Build the Android app from the <code className="rounded bg-stone-200 dark:bg-stone-600 px-1">android/</code> folder, or{" "}
              <Link
                href={`${pathname || "/"}?tab=settings`}
                className="text-primary font-medium hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
              >
                enable sample data in Settings
              </Link>
              {" "}to explore the dashboard.
            </p>
          </>
        ) : (
          <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
            {sample
              ? `You’re viewing sample data. Connect the ${APP_NAME} Android app and configure sync to see your real herd here.`
              : `Configure sync in Settings once the ${APP_NAME} Android app is set up to see live data.`}
          </p>
        )}
      </section>
    </div>
  );
}
