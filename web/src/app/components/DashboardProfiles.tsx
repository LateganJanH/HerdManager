"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useMemo, useCallback, useEffect } from "react";
import { formatRelativeTime } from "../lib/formatRelativeTime";
import { isSampleDataEnabled } from "../lib/mockHerdData";
import { useHerdStats } from "../lib/useHerdStats";
import { useAnimalProfiles } from "../lib/useAnimalProfiles";
import { useAnimalDetail } from "../lib/useAnimalDetail";
import type { AnimalProfile, AnimalDetail } from "../lib/mockHerdData";
import { APP_NAME } from "../lib/version";

function AnimalDetailContent({
  detail,
  onRefresh,
}: {
  detail: AnimalDetail;
  onRefresh: () => void;
}) {
  return (
    <div className="mt-4 space-y-5">
      <dl className="space-y-3">
        <div>
          <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Ear tag</dt>
          <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{detail.earTag}</dd>
        </div>
        {detail.name && (
          <div>
            <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Name</dt>
            <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{detail.name}</dd>
          </div>
        )}
        <div>
          <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Status</dt>
          <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{detail.status}</dd>
        </div>
        <div>
          <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Sex</dt>
          <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{detail.sex}</dd>
        </div>
        {detail.breed && (
          <div>
            <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Breed</dt>
            <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{detail.breed}</dd>
          </div>
        )}
        {detail.dateOfBirth && (
          <div>
            <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Date of birth</dt>
            <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{detail.dateOfBirth}</dd>
          </div>
        )}
        {detail.herdName && (
          <div>
            <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Herd</dt>
            <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{detail.herdName}</dd>
          </div>
        )}
      </dl>

      {detail.breedingEvents.length > 0 && (
        <section>
          <h4 className="text-sm font-semibold text-stone-700 dark:text-stone-300">Breeding</h4>
          <ul className="mt-2 space-y-2 rounded-card border border-stone-200 dark:border-stone-600 p-3 bg-stone-50 dark:bg-stone-800/50">
            {detail.breedingEvents.map((b) => (
              <li key={b.id} className="text-sm text-stone-800 dark:text-stone-200">
                <span className="font-medium">{b.serviceDate}</span> — {b.eventType}
                {b.pregnancyCheckResult && ` · ${b.pregnancyCheckResult.replace(/_/g, " ")}`}
                {b.dueDate && ` · Due ${b.dueDate}`}
              </li>
            ))}
          </ul>
        </section>
      )}

      {detail.calvingEvents.length > 0 && (
        <section>
          <h4 className="text-sm font-semibold text-stone-700 dark:text-stone-300">Calving</h4>
          <ul className="mt-2 space-y-2 rounded-card border border-stone-200 dark:border-stone-600 p-3 bg-stone-50 dark:bg-stone-800/50">
            {detail.calvingEvents.map((c) => (
              <li key={c.id} className="text-sm text-stone-800 dark:text-stone-200">
                <span className="font-medium">{c.actualDate}</span>
                {c.calfSex && ` · ${c.calfSex}`}
                {c.assistanceRequired && " · Assistance"}
              </li>
            ))}
          </ul>
        </section>
      )}

      {detail.healthEvents.length > 0 && (
        <section>
          <h4 className="text-sm font-semibold text-stone-700 dark:text-stone-300">Health & treatments</h4>
          <ul className="mt-2 space-y-2 rounded-card border border-stone-200 dark:border-stone-600 p-3 bg-stone-50 dark:bg-stone-800/50">
            {detail.healthEvents.map((h) => (
              <li key={h.id} className="text-sm text-stone-800 dark:text-stone-200">
                <span className="font-medium">{h.date}</span> — {h.eventType || "—"}
                {h.product && ` · ${h.product}`}
                {h.notes && ` · ${h.notes}`}
              </li>
            ))}
          </ul>
        </section>
      )}

      {(detail.photos ?? []).filter((p) => p.url).length > 0 && (
        <section>
          <h4 className="text-sm font-semibold text-stone-700 dark:text-stone-300">Photos</h4>
          <div className="mt-2 flex flex-wrap gap-2">
            {(detail.photos ?? [])
              .filter((p) => p.url)
              .map((p) => (
                <a
                  key={p.id}
                  href={p.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="block rounded-card border border-stone-200 dark:border-stone-600 overflow-hidden bg-stone-100 dark:bg-stone-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                >
                  <img
                    src={p.url}
                    alt={p.angle ? `Photo ${p.angle.replace(/_/g, " ")}` : "Animal photo"}
                    className="h-24 w-24 object-cover"
                  />
                </a>
              ))}
          </div>
        </section>
      )}
      {detail.photoCount > 0 && (detail.photos ?? []).filter((p) => p.url).length === 0 && (
        <p className="text-sm text-stone-500 dark:text-stone-400">
          {detail.photoCount} photo{detail.photoCount !== 1 ? "s" : ""} — view in {APP_NAME} app.
        </p>
      )}

      <button
        type="button"
        onClick={onRefresh}
        className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded"
      >
        Refresh detail
      </button>
    </div>
  );
}

function downloadProfilesCsv(animals: AnimalProfile[]) {
  const header = ["Ear tag", "Status", "Sex"];
  const rows = animals.map((a) => [a.earTag, a.status, a.sex]);
  const csv = [header, ...rows].map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(",")).join("\r\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `profiles-${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

type SortField = "earTag" | "status" | "sex";

function sortAnimals(animals: AnimalProfile[], field: SortField): AnimalProfile[] {
  return [...animals].sort((a, b) => {
    const va = a[field].toLowerCase();
    const vb = b[field].toLowerCase();
    return va.localeCompare(vb, undefined, { sensitivity: "base" });
  });
}

export function DashboardProfiles() {
  const pathname = usePathname();
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState<SortField>("earTag");
  const [selectedAnimal, setSelectedAnimal] = useState<AnimalProfile | null>(null);
  const { stats, fromApi: statsFromApi, loading: statsLoading, isError, dataUpdatedAt, refetch } = useHerdStats();
  const { animals: allAnimals, fromApi: profilesFromApi, loading: profilesLoading, refetch: refetchProfiles } = useAnimalProfiles();
  const { detail: animalDetail, loading: detailLoading, refetch: refetchDetail } = useAnimalDetail(selectedAnimal?.id ?? null);
  const fromApi = statsFromApi || profilesFromApi;
  const loading = statsLoading || profilesLoading;
  const dataSourceLabel = loading ? "Loading…" : fromApi ? "Synced" : isSampleDataEnabled() ? "Sample data" : "Connect app to sync";
  const updatedLabel = useMemo(
    () => (fromApi && dataUpdatedAt > 0 ? formatRelativeTime(dataUpdatedAt) : null),
    [fromApi, dataUpdatedAt]
  );
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return allAnimals;
    return allAnimals.filter(
      (a) =>
        a.earTag.toLowerCase().includes(q) ||
        a.status.toLowerCase().includes(q) ||
        a.sex.toLowerCase().includes(q)
    );
  }, [allAnimals, search]);
  const animals = useMemo(() => sortAnimals(filtered, sortBy), [filtered, sortBy]);
  const sample = isSampleDataEnabled();
  const handleExportCsv = useCallback(() => downloadProfilesCsv(animals), [animals]);

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === "Escape") setSelectedAnimal(null);
    };
    if (selectedAnimal) {
      document.addEventListener("keydown", handleEscape);
      return () => document.removeEventListener("keydown", handleEscape);
    }
  }, [selectedAnimal]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
      <h2 className="text-xl font-semibold text-stone-900 dark:text-stone-100">
        Profiles
      </h2>
      <p className="text-stone-600 dark:text-stone-300 text-base">
        Cattle profiles, ear tags, and herd assignments. Manage animals in the {APP_NAME} Android app; the list will sync here when connected.
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
            refetchProfiles();
          }}
          disabled={loading}
          className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded disabled:opacity-60"
          aria-label="Refresh herd stats"
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

      {allAnimals.length > 0 ? (
        <section aria-label="Herd list">
          <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-stone-600 dark:text-stone-400">
              {search.trim() ? (
                <>Showing <strong className="text-primary">{animals.length}</strong> of {allAnimals.length} animals</>
              ) : (
                <>
                  {stats.totalAnimals > 0 && (
                    <strong className="text-primary">{stats.totalAnimals}</strong>
                  )}{" "}
                  {stats.totalAnimals > 0 ? "animals in herd" : "Showing sample profiles."}
                </>
              )}
            </p>
            <div className="flex flex-wrap items-center gap-2">
              <label htmlFor="profiles-sort" className="sr-only">Sort by</label>
              <select
                id="profiles-sort"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as SortField)}
                className="rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-1.5 text-sm text-stone-900 dark:text-stone-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                aria-label="Sort profiles by"
              >
                <option value="earTag">Ear tag (A–Z)</option>
                <option value="status">Status</option>
                <option value="sex">Sex</option>
              </select>
              <label className="flex items-center gap-2 text-sm text-stone-600 dark:text-stone-400">
                <span className="sr-only">Search by ear tag or status</span>
                <input
                  type="search"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search ear tag, status…"
                  className="rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-1.5 text-stone-900 dark:text-stone-100 placeholder:text-stone-400 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary w-full sm:w-48"
                />
              </label>
              <button
                type="button"
                onClick={handleExportCsv}
                disabled={animals.length === 0}
                className="rounded-button border border-stone-300 dark:border-stone-600 px-3 py-1.5 text-sm font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50 disabled:pointer-events-none"
              >
                Export (CSV)
              </button>
            </div>
          </div>
          {animals.length > 0 ? (
          <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {animals.map((animal) => (
              <li key={animal.id}>
                <button
                  type="button"
                  onClick={() => setSelectedAnimal(animal)}
                  className="w-full text-left rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-4 shadow-card hover:shadow-card-hover transition-shadow focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
                  aria-label={`View details for ${animal.earTag}`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-semibold text-stone-900 dark:text-stone-100">
                      {animal.earTag}
                    </span>
                    <span className="rounded-full bg-primary/10 dark:bg-primary/20 px-2 py-0.5 text-xs font-medium text-primary">
                      {animal.status}
                    </span>
                  </div>
                  <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">
                    {animal.sex}
                  </p>
                </button>
              </li>
            ))}
          </ul>
          ) : (
            <div className="rounded-card bg-stone-50 dark:bg-stone-800/50 border border-stone-200 dark:border-stone-600 p-4 text-center">
              <p className="text-sm text-stone-600 dark:text-stone-400">
                No profiles match “{search.trim()}”.
            </p>
              <button
                type="button"
                onClick={() => setSearch("")}
                className="mt-2 text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
              >
                Clear search
              </button>
            </div>
          )}
          {sample && (
            <p className="mt-3 text-xs text-stone-500 dark:text-stone-400">
              Sample data — connect app to sync full herd list
            </p>
          )}
        </section>
      ) : (
        <div className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-6 shadow-card">
          <p className="text-stone-600 dark:text-stone-300 text-base">
            No herd data yet. Register animals in the {APP_NAME} Android app; once sync is
            configured, profiles will appear here. Enable sample data in Settings to preview.
          </p>
          <p className="mt-3">
            <Link
              href={`${pathname || "/"}?tab=home`}
              className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
            >
              View dashboard
            </Link>
          </p>
        </div>
      )}

      {selectedAnimal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
          role="dialog"
          aria-modal="true"
          aria-labelledby="animal-detail-title"
          onClick={() => setSelectedAnimal(null)}
        >
          <div
            className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 shadow-card max-w-lg w-full max-h-[90vh] overflow-y-auto p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 id="animal-detail-title" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
              Animal details
            </h3>
            {detailLoading && fromApi ? (
              <p className="mt-4 text-sm text-stone-500 dark:text-stone-400">Loading…</p>
            ) : animalDetail && fromApi ? (
              <AnimalDetailContent
                detail={animalDetail}
                onRefresh={() => { refetchDetail(); refetchProfiles(); }}
              />
            ) : (
              <>
                <dl className="mt-4 space-y-3">
                  <div>
                    <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Ear tag</dt>
                    <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{selectedAnimal.earTag}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Status</dt>
                    <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{selectedAnimal.status}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-stone-500 dark:text-stone-400">Sex</dt>
                    <dd className="mt-0.5 text-stone-900 dark:text-stone-100">{selectedAnimal.sex}</dd>
                  </div>
                </dl>
                <p className="mt-4 text-sm text-stone-500 dark:text-stone-400">
                  {fromApi ? "Could not load full detail. It may have been removed or sync failed." : "Sign in and sync to see full detail."}{" "}
                  Breeding, calving, and history are managed in the {APP_NAME} Android app.
                </p>
              </>
            )}
            <div className="mt-6 flex justify-end">
              <button
                type="button"
                onClick={() => setSelectedAnimal(null)}
                className="rounded-button border border-stone-300 dark:border-stone-600 px-4 py-2 text-sm font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
