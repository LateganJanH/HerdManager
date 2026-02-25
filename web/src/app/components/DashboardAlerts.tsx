"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useMemo, useCallback } from "react";
import { formatRelativeTime } from "../lib/formatRelativeTime";
import { isSampleDataEnabled } from "../lib/mockHerdData";
import { useHerdStats } from "../lib/useHerdStats";
import { useAlerts } from "../lib/useAlerts";
import type { AlertItem } from "../lib/mockHerdData";

type AlertFilter = "all" | "calving" | "pregnancy_check" | "withdrawal";

function downloadAlertsCsv(alerts: AlertItem[]) {
  const header = ["Ear tag", "Type", "Due / check date", "Days until", "Product"];
  const typeLabel = (a: AlertItem) =>
    a.type === "calving" ? "Calving" : a.type === "pregnancy_check" ? "Pregnancy check" : "Withdrawal ends";
  const rows = alerts.map((a) => [
    a.earTag,
    typeLabel(a),
    a.dueOrCheckDate,
    String(a.daysUntil),
    a.product ?? "",
  ]);
  const csv = [header, ...rows].map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(",")).join("\r\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `alerts-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

export function DashboardAlerts() {
  const pathname = usePathname();
  const [filter, setFilter] = useState<AlertFilter>("all");
  const { fromApi: statsFromApi, loading: statsLoading, isError, dataUpdatedAt, refetch } = useHerdStats();
  const { alerts: rawAlertsFromHook, fromApi: alertsFromApi, loading: alertsLoading, refetch: refetchAlerts } = useAlerts();
  const fromApi = statsFromApi || alertsFromApi;
  const loading = statsLoading || alertsLoading;
  const dataSourceLabel = loading ? "Loading…" : fromApi ? "Synced" : isSampleDataEnabled() ? "Sample data" : "Connect app to sync";
  const updatedLabel = useMemo(
    () => (fromApi && dataUpdatedAt > 0 ? formatRelativeTime(dataUpdatedAt) : null),
    [fromApi, dataUpdatedAt]
  );
  const rawAlerts = useMemo(
    () => [...rawAlertsFromHook].sort((a, b) => a.daysUntil - b.daysUntil),
    [rawAlertsFromHook]
  );
  const alerts = useMemo(() => {
    if (filter === "all") return rawAlerts;
    return rawAlerts.filter((a) => a.type === filter);
  }, [rawAlerts, filter]);
  const sample = isSampleDataEnabled();
  const handleExportCsv = useCallback(() => downloadAlertsCsv(alerts), [alerts]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
      <h2 className="text-xl font-semibold text-stone-900 dark:text-stone-100">
        Alerts
      </h2>
          <p className="text-stone-600 dark:text-stone-300 text-base">
        Calving due dates, pregnancy check reminders, and withdrawal-period end dates. Items due soon are highlighted.
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <p className="text-sm text-stone-500 dark:text-stone-400">
          Data: {dataSourceLabel}
          {updatedLabel != null && ` · Last updated ${updatedLabel}`}
        </p>
        <button
          type="button"
          onClick={() => {
            refetch();
            refetchAlerts();
          }}
          disabled={loading}
          className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded disabled:opacity-60"
          aria-label="Refresh stats and alerts"
        >
          Refresh
        </button>
      </div>

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

      {rawAlerts.length > 0 && (
        <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by type">
          {(["all", "calving", "pregnancy_check", "withdrawal"] as const).map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setFilter(value)}
              className={`rounded-button px-3 py-1.5 text-sm font-medium border transition-colors ${
                filter === value
                  ? "border-primary bg-primary text-white"
                  : "border-stone-300 dark:border-stone-600 text-stone-700 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700"
              }`}
            >
              {value === "all" ? "All" : value === "calving" ? "Calving" : value === "pregnancy_check" ? "Pregnancy check" : "Withdrawal"}
            </button>
          ))}
        </div>
      )}

      {alerts.length > 0 ? (
        <ul className="space-y-3" aria-label="Upcoming alerts">
          {alerts.map((alert) => (
            <li
              key={alert.id}
              className={`rounded-card border p-4 ${
                alert.daysUntil <= 7
                  ? "border-accent-alert bg-accent-alert-bg dark:bg-stone-800"
                  : "border-stone-200 dark:border-stone-600 bg-white dark:bg-stone-800"
              }`}
            >
              <div className="flex justify-between items-start">
                <div>
                  <span className="font-medium text-stone-900 dark:text-stone-100">
                    {alert.earTag}
                  </span>
                  <span className="ml-2 text-sm text-stone-500 dark:text-stone-400">
                    {alert.type === "calving" ? "Calving due" : alert.type === "pregnancy_check" ? "Pregnancy check due" : "Withdrawal ends"}
                  </span>
                </div>
                <span className="text-sm font-medium text-stone-600 dark:text-stone-300">
                  {alert.daysUntil} days
                </span>
              </div>
              <p className="mt-1 text-sm text-stone-600 dark:text-stone-300">
                {alert.dueOrCheckDate}
                {alert.product != null && alert.product !== "" && (
                  <span className="ml-2">· {alert.product}</span>
                )}
              </p>
            </li>
          ))}
        </ul>
      ) : (
        <div className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-6 text-center">
          <p className="text-stone-600 dark:text-stone-300 text-base">
            {rawAlerts.length === 0
              ? "No alerts right now. Connect the app to sync calving, pregnancy check, and withdrawal due dates."
              : `No ${filter === "calving" ? "calving" : filter === "pregnancy_check" ? "pregnancy check" : "withdrawal"} alerts.`}
          </p>
          {rawAlerts.length === 0 ? (
            <p className="mt-3">
              <Link
                href={`${pathname || "/"}?tab=home`}
                className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
              >
                View dashboard
              </Link>
            </p>
          ) : (
            <p className="mt-3">
              <button
                type="button"
                onClick={() => setFilter("all")}
                className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
              >
                Show all
              </button>
            </p>
          )}
        </div>
      )}
      {rawAlerts.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          {sample && (
            <p className="text-xs text-stone-500 dark:text-stone-400">
              Sample data — connect app for live alerts
            </p>
          )}
          <button
            type="button"
            onClick={handleExportCsv}
            className="rounded-button border border-stone-300 dark:border-stone-600 px-3 py-1.5 text-sm font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            Export alerts (CSV)
          </button>
        </div>
      )}
    </div>
  );
}
