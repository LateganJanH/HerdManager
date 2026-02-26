"use client";

import { useState, useEffect, useCallback } from "react";
import { formatRelativeTime } from "../lib/formatRelativeTime";
import { isSampleDataEnabled, setSampleDataEnabled, type FarmProfile } from "../lib/mockHerdData";
import { getTheme, setTheme, subscribeTheme, type Theme } from "../lib/theme";
import { useAuth, isAuthConfigured } from "../lib/useAuth";
import { useFarmSettings } from "../lib/useFarmSettings";
import { useHerdStats } from "../lib/useHerdStats";
import { useLinkedDevices } from "../lib/useLinkedDevices";
import { getFirebaseDb } from "../lib/firebase";
import { verifyPhotoSync, type PhotoSyncResult } from "../lib/verifyPhotoSync";
import { APP_NAME, APP_VERSION } from "../lib/version";

const THEME_OPTIONS: { value: Theme; label: string }[] = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System (follow device)" },
];

export function DashboardSettings({ onBack }: { onBack: () => void }) {
  const [sampleEnabled, setSampleEnabled] = useState(true);
  const [mounted, setMounted] = useState(false);
  const [theme, setThemeState] = useState<Theme>("system");
  const { user, signOut } = useAuth();
  const { fromApi, dataUpdatedAt } = useHerdStats();
  const { farm, fromApi: farmFromApi, loading: farmLoading, updateFarmSettings } = useFarmSettings();
  const { devices, loading: devicesLoading, isError: devicesError, refetch: refetchDevices } = useLinkedDevices();
  const lastSyncedLabel =
    fromApi && dataUpdatedAt > 0 ? formatRelativeTime(dataUpdatedAt) : null;

  const [photoSyncResult, setPhotoSyncResult] = useState<PhotoSyncResult | null>(null);
  const [photoSyncChecking, setPhotoSyncChecking] = useState(false);
  const [showEditFarm, setShowEditFarm] = useState(false);
  const [editFarm, setEditFarm] = useState<FarmProfile | null>(null);
  const [farmSaveError, setFarmSaveError] = useState<string | null>(null);
  const runPhotoSyncCheck = async () => {
    if (!user?.uid) return;
    setPhotoSyncChecking(true);
    setPhotoSyncResult(null);
    try {
      const db = await getFirebaseDb();
      if (!db) {
        setPhotoSyncResult({ total: 0, withStorageUrl: 0, sampleUrlLoads: "none", error: "Firebase not configured" });
        return;
      }
      const result = await verifyPhotoSync(db, user.uid);
      setPhotoSyncResult(result);
    } finally {
      setPhotoSyncChecking(false);
    }
  };

  useEffect(() => {
    setMounted(true);
  }, []);
  useEffect(() => {
    if (!mounted) return;
    setSampleEnabled(isSampleDataEnabled());
    setThemeState(getTheme());
  }, [mounted]);
  useEffect(() => {
    if (!mounted) return;
    return subscribeTheme((t) => setThemeState(t));
  }, [mounted]);

  const isDark =
    mounted &&
    typeof document !== "undefined" &&
    document.documentElement.classList.contains("dark");

  const handleSampleToggle = (enabled: boolean) => {
    setSampleDataEnabled(enabled);
    setSampleEnabled(enabled);
  };

  const openEditFarm = useCallback(() => {
    if (farm) {
      setEditFarm({
        ...farm,
        contacts: farm.contacts?.length ? [...farm.contacts] : [{ name: "", phone: "", email: "" }],
        gestationDays: farm.gestationDays ?? 283,
        weaningAgeDays: farm.weaningAgeDays ?? 200,
      });
      setShowEditFarm(true);
      setFarmSaveError(null);
    }
  }, [farm]);

  const handleSaveFarm = async () => {
    if (!editFarm) return;
    setFarmSaveError(null);
    try {
      await updateFarmSettings(editFarm);
      setShowEditFarm(false);
      setEditFarm(null);
    } catch (e) {
      setFarmSaveError(e instanceof Error ? e.message : "Failed to save");
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={onBack}
          className="touch-target rounded-button text-stone-600 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700 p-2 -ml-2"
          aria-label="Back to dashboard"
        >
          ← Back
        </button>
        <h2 className="text-xl font-semibold text-stone-900 dark:text-stone-100">
          Settings
        </h2>
      </div>

      <section
        className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
        aria-labelledby="farm-heading"
      >
        <h3 id="farm-heading" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          Farm &amp; sync
        </h3>
        {farmFromApi && (farmLoading ? (
          <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">Loading farm profile…</p>
        ) : farm ? (
          <div className="mt-3 space-y-3">
            {!showEditFarm ? (
              <>
                <div className="rounded-lg border border-stone-200 dark:border-stone-600 bg-stone-50 dark:bg-stone-700/50 px-3 py-2 text-sm">
                  <p className="font-medium text-stone-900 dark:text-stone-100">{farm.name || "Farm"}</p>
                  {farm.address && (
                    <p className="mt-1 text-stone-600 dark:text-stone-300">{farm.address}</p>
                  )}
                  {(farm.contacts?.length ?? 0) > 0 && (
                    <div className="mt-1 space-y-0.5">
                      {farm.contacts!.map((c, i) => (
                        <p key={i} className="text-stone-500 dark:text-stone-400">
                          {[c.name, c.phone, c.email].filter(Boolean).join(" · ") || "—"}
                        </p>
                      ))}
                    </div>
                  )}
                  <p className="mt-1 text-stone-500 dark:text-stone-400">
                    Calving alert: {farm.calvingAlertDays} days · Pregnancy check: {farm.pregnancyCheckDaysAfterBreeding} days after breeding
                    {farm.gestationDays != null && ` · Gestation: ${farm.gestationDays} days`}
                    {farm.weaningAgeDays != null && ` · Weaning age: ${farm.weaningAgeDays} days`}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={openEditFarm}
                  className="rounded-button border border-stone-300 dark:border-stone-600 px-3 py-2 text-sm font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                >
                  Edit farm
                </button>
                <p className="text-xs text-stone-400 dark:text-stone-500">
                  Farm profile is synced across devices. Edit here or in Farm Settings on the app; changes appear everywhere after sync.
                </p>
              </>
            ) : editFarm ? (
              <div className="rounded-lg border border-stone-200 dark:border-stone-600 bg-stone-50 dark:bg-stone-700/50 p-4 space-y-3">
                <h4 className="text-sm font-medium text-stone-700 dark:text-stone-200">Edit farm</h4>
                <label className="block text-sm text-stone-600 dark:text-stone-400">
                  Name
                  <input
                    type="text"
                    value={editFarm.name}
                    onChange={(e) => setEditFarm({ ...editFarm, name: e.target.value })}
                    className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100"
                  />
                </label>
                <label className="block text-sm text-stone-600 dark:text-stone-400">
                  Address
                  <input
                    type="text"
                    value={editFarm.address}
                    onChange={(e) => setEditFarm({ ...editFarm, address: e.target.value })}
                    className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100"
                  />
                </label>
                <div className="space-y-3">
                  <h4 className="text-sm font-medium text-stone-700 dark:text-stone-200">Contacts</h4>
                  {(editFarm.contacts ?? []).map((c, i) => (
                    <div key={i} className="flex flex-wrap gap-2 items-end rounded border border-stone-200 dark:border-stone-600 p-2 bg-white dark:bg-stone-800/50">
                      <label className="flex-1 min-w-[120px] text-sm text-stone-600 dark:text-stone-400">
                        Name
                        <input
                          type="text"
                          value={c.name}
                          onChange={(e) => {
                            const next = [...(editFarm.contacts ?? [])];
                            next[i] = { ...next[i], name: e.target.value };
                            setEditFarm({ ...editFarm, contacts: next });
                          }}
                          className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-2 py-1.5 text-stone-900 dark:text-stone-100"
                        />
                      </label>
                      <label className="flex-1 min-w-[120px] text-sm text-stone-600 dark:text-stone-400">
                        Phone
                        <input
                          type="text"
                          value={c.phone}
                          onChange={(e) => {
                            const next = [...(editFarm.contacts ?? [])];
                            next[i] = { ...next[i], phone: e.target.value };
                            setEditFarm({ ...editFarm, contacts: next });
                          }}
                          className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-2 py-1.5 text-stone-900 dark:text-stone-100"
                        />
                      </label>
                      <label className="flex-1 min-w-[140px] text-sm text-stone-600 dark:text-stone-400">
                        Email
                        <input
                          type="email"
                          value={c.email}
                          onChange={(e) => {
                            const next = [...(editFarm.contacts ?? [])];
                            next[i] = { ...next[i], email: e.target.value };
                            setEditFarm({ ...editFarm, contacts: next });
                          }}
                          className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-2 py-1.5 text-stone-900 dark:text-stone-100"
                        />
                      </label>
                      <button
                        type="button"
                        onClick={() => setEditFarm({ ...editFarm, contacts: editFarm.contacts!.filter((_, j) => j !== i) })}
                        className="rounded-button border border-stone-300 dark:border-stone-600 px-2 py-1.5 text-sm text-stone-600 dark:text-stone-400 hover:bg-stone-100 dark:hover:bg-stone-700"
                        aria-label="Remove contact"
                      >
                        Remove
                      </button>
                    </div>
                  ))}
                  <button
                    type="button"
                    onClick={() => setEditFarm({ ...editFarm, contacts: [...(editFarm.contacts ?? []), { name: "", phone: "", email: "" }] })}
                    className="rounded-button border border-stone-300 dark:border-stone-600 px-3 py-2 text-sm font-medium text-stone-700 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700"
                  >
                    Add contact
                  </button>
                </div>
                <label className="block text-sm text-stone-600 dark:text-stone-400">
                  Calving alert (days before due)
                  <input
                    type="number"
                    min={1}
                    max={90}
                    value={editFarm.calvingAlertDays}
                    onChange={(e) => setEditFarm({ ...editFarm, calvingAlertDays: Math.max(1, Math.min(90, e.target.valueAsNumber || 14)) })}
                    className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100"
                  />
                </label>
                <label className="block text-sm text-stone-600 dark:text-stone-400">
                  Pregnancy check (days after breeding)
                  <input
                    type="number"
                    min={14}
                    max={60}
                    value={editFarm.pregnancyCheckDaysAfterBreeding}
                    onChange={(e) => setEditFarm({ ...editFarm, pregnancyCheckDaysAfterBreeding: Math.max(14, Math.min(60, e.target.valueAsNumber || 28)) })}
                    className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100"
                  />
                </label>
                <label className="block text-sm text-stone-600 dark:text-stone-400">
                  Gestation length (days)
                  <input
                    type="number"
                    min={250}
                    max={320}
                    value={editFarm.gestationDays ?? 283}
                    onChange={(e) => setEditFarm({ ...editFarm, gestationDays: Math.max(250, Math.min(320, e.target.valueAsNumber || 283)) })}
                    className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100"
                  />
                </label>
                <label className="block text-sm text-stone-600 dark:text-stone-400">
                  Weaning age (days) — alert when weaning weight is due (150–300, typical 200)
                  <input
                    type="number"
                    min={150}
                    max={300}
                    value={editFarm.weaningAgeDays ?? 200}
                    onChange={(e) => setEditFarm({ ...editFarm, weaningAgeDays: Math.max(150, Math.min(300, e.target.valueAsNumber || 200)) })}
                    className="mt-1 block w-full rounded-button border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100"
                  />
                </label>
                {farmSaveError && (
                  <p className="text-sm text-amber-700 dark:text-amber-300" role="alert">{farmSaveError}</p>
                )}
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleSaveFarm}
                    className="rounded-button bg-primary text-white px-4 py-2 text-sm font-medium hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                  >
                    Save
                  </button>
                  <button
                    type="button"
                    onClick={() => { setShowEditFarm(false); setEditFarm(null); setFarmSaveError(null); }}
                    className="rounded-button border border-stone-300 dark:border-stone-600 px-4 py-2 text-sm font-medium text-stone-700 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        ) : null)}
        <p className="mt-3 text-stone-600 dark:text-stone-300 text-base">
          Link multiple phones or tablets running the {APP_NAME} Android app. Sign in with the same account on each device and sync to see them here. The dashboard shows combined herd data from all linked devices.
        </p>
        <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">
          Sync: {fromApi ? "Connected" : "Not configured"}
          {devices.length > 0 && ` (${devices.length} device${devices.length === 1 ? "" : "s"} linked)`}
        </p>
        <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">
          Last synced: {lastSyncedLabel ?? "—"}
        </p>
        <div className="mt-4" aria-labelledby="linked-devices-heading">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h4 id="linked-devices-heading" className="text-sm font-medium text-stone-700 dark:text-stone-200">
              Linked devices ({devices.length})
            </h4>
            <button
              type="button"
              onClick={() => refetchDevices()}
              disabled={devicesLoading}
              className="text-sm text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded disabled:opacity-60"
              aria-label="Refresh device list"
            >
              Refresh
            </button>
          </div>
          {devicesError && (
            <p className="mt-2 text-sm text-amber-700 dark:text-amber-300" role="alert">
              Could not load devices.{" "}
              <button
                type="button"
                onClick={() => refetchDevices()}
                className="underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded"
              >
                Try again
              </button>
            </p>
          )}
          {!devicesError && devicesLoading && (
            <p className="mt-2 text-sm text-stone-500 dark:text-stone-400" aria-live="polite">
              Loading…
            </p>
          )}
          {!devicesError && !devicesLoading && devices.length === 0 && (
            <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">
              No devices linked yet. Open the {APP_NAME} Android app on each phone or tablet, sign in with the same account, and sync to link them to this dashboard.
            </p>
          )}
          {!devicesError && !devicesLoading && devices.length > 0 && (
            <ul className="mt-2 space-y-2 list-none" role="list">
              {devices.map((d) => (
                <li
                  key={d.id}
                  className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-stone-200 dark:border-stone-600 bg-stone-50 dark:bg-stone-700/50 px-3 py-2 text-sm"
                >
                  <span className="font-medium text-stone-900 dark:text-stone-100">{d.name}</span>
                  <span className="text-stone-500 dark:text-stone-400">
                    Last synced {formatRelativeTime(d.lastSyncAt)}
                    {d.platform ? ` · ${d.platform}` : ""}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section
        className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
        aria-labelledby="development-heading"
      >
        <h3 id="development-heading" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          Development
        </h3>
        <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
          Show sample herd data on the dashboard when sync is not configured.
        </p>
        {fromApi && user && (
          <div className="mt-4 rounded-lg border border-stone-200 dark:border-stone-600 bg-stone-50 dark:bg-stone-700/50 px-3 py-2">
            <h4 className="text-sm font-medium text-stone-700 dark:text-stone-200">Verify photo sync</h4>
            <p className="mt-1 text-sm text-stone-500 dark:text-stone-400">
              After syncing from the Android app (with at least one animal photo), run this to confirm Firestore photos and Storage URLs work.
            </p>
            <button
              type="button"
              onClick={runPhotoSyncCheck}
              disabled={photoSyncChecking}
              className="mt-2 text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded disabled:opacity-60"
            >
              {photoSyncChecking ? "Checking…" : "Check photo sync"}
            </button>
            {photoSyncResult && (
              <div className="mt-2 text-sm text-stone-700 dark:text-stone-300" role="status">
                {photoSyncResult.error ? (
                  <p className="text-amber-700 dark:text-amber-300">Error: {photoSyncResult.error}</p>
                ) : (
                  <>
                    <p>{photoSyncResult.total} photo(s) in Firestore, {photoSyncResult.withStorageUrl} with Storage URL.</p>
                    {photoSyncResult.sampleUrlLoads !== "none" && (
                      <p>Sample URL loads: {photoSyncResult.sampleUrlLoads === "ok" ? "OK" : "Failed"}.</p>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        )}
        {mounted && (
          <label className="mt-3 flex items-center gap-3 touch-target cursor-pointer">
            <input
              type="checkbox"
              checked={sampleEnabled}
              onChange={(e) => handleSampleToggle(e.target.checked)}
              className="rounded border-stone-300 text-primary focus:ring-primary"
            />
            <span className="text-stone-700 dark:text-stone-200">Use sample data</span>
          </label>
        )}
      </section>

      <section
        className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
        aria-labelledby="appearance-heading"
      >
        <h3 id="appearance-heading" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          Appearance
        </h3>
        <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
          Current: {isDark ? "Dark" : "Light"}
          {theme === "system" && " (following device)"}. You can also use “Toggle dark mode” in the menu for a quick switch.
        </p>
        {mounted && (
          <fieldset className="mt-3" aria-label="Theme">
            <legend className="sr-only">Choose theme</legend>
            <div className="flex flex-wrap gap-3">
              {THEME_OPTIONS.map((opt) => (
                <label key={opt.value} className="flex items-center gap-2 touch-target cursor-pointer">
                  <input
                    type="radio"
                    name="theme"
                    value={opt.value}
                    checked={theme === opt.value}
                    onChange={() => setTheme(opt.value)}
                    className="border-stone-300 text-primary focus:ring-primary"
                  />
                  <span className="text-stone-700 dark:text-stone-200">{opt.label}</span>
                </label>
              ))}
            </div>
          </fieldset>
        )}
      </section>

      <section
        className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
        aria-labelledby="shortcuts-heading"
      >
        <h3 id="shortcuts-heading" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          Keyboard shortcuts
        </h3>
        <ul className="mt-2 space-y-1 text-base text-stone-600 dark:text-stone-300 list-none">
          <li><kbd className="rounded bg-stone-200 dark:bg-stone-600 px-1.5 py-0.5 font-mono text-sm">1</kbd>–<kbd className="rounded bg-stone-200 dark:bg-stone-600 px-1.5 py-0.5 font-mono text-sm">5</kbd> — Switch tab (Home, Profiles, Alerts, Analytics, Settings); focus moves to main content</li>
          <li><kbd className="rounded bg-stone-200 dark:bg-stone-600 px-1.5 py-0.5 font-mono text-sm">Esc</kbd> — Close menu</li>
          <li>Menu: <strong>Copy link</strong> — Copy current page URL (including tab) to clipboard</li>
          <li><kbd className="rounded bg-stone-200 dark:bg-stone-600 px-1.5 py-0.5 font-mono text-sm">Tab</kbd> — Move focus (bottom nav, menu, links)</li>
          <li>First <kbd className="rounded bg-stone-200 dark:bg-stone-600 px-1.5 py-0.5 font-mono text-sm">Tab</kbd> on the page — Skip to main content</li>
          <li>Scroll to top — Button (↑) appears bottom-right when you scroll down</li>
          <li>Print — Header and bottom nav are hidden; only main content is printed</li>
        </ul>
      </section>

      {isAuthConfigured() && user && (
        <section
          className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
          aria-labelledby="account-heading"
        >
          <h3 id="account-heading" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
            Account
          </h3>
          <p className="mt-2 text-sm text-stone-600 dark:text-stone-400">
            {user.email}
          </p>
          <button
            type="button"
            onClick={() => signOut()}
            className="mt-3 rounded-lg border border-stone-300 dark:border-stone-600 px-4 py-2 text-sm font-medium text-stone-700 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            Sign out
          </button>
        </section>
      )}

      <section
        className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 shadow-card"
        aria-labelledby="about-heading"
      >
        <h3 id="about-heading" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          About
        </h3>
        <p className="mt-2 text-stone-600 dark:text-stone-300 text-base">
          {APP_NAME} — modern cattle herd management for the field. Analytics, alerts, and profiles in one place.
        </p>
        <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">
          Web dashboard v{APP_VERSION}
        </p>
        <p className="mt-2">
          <a
            href="/api/spec"
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm text-primary font-medium hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
          >
            API spec (OpenAPI 3)
          </a>
        </p>
      </section>
    </div>
  );
}
