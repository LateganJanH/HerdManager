"use client";

import { useCallback, useEffect, useState } from "react";
import { APP_VERSION } from "../lib/version";

const POLL_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

export function VersionRefreshBanner() {
  const [newVersionAvailable, setNewVersionAvailable] = useState(false);

  const checkVersion = useCallback(async () => {
    try {
      const res = await fetch("/api/version", { cache: "no-store" });
      if (!res.ok) return;
      const data = (await res.json()) as { version: string };
      if (data.version && data.version !== APP_VERSION) {
        setNewVersionAvailable(true);
      }
    } catch {
      // Ignore network errors; user may be offline
    }
  }, []);

  useEffect(() => {
    checkVersion();
    const id = setInterval(checkVersion, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [checkVersion]);

  if (!newVersionAvailable) return null;

  return (
    <div
      className="print:hidden border-b border-primary/30 bg-primary/10 dark:bg-primary/20 px-4 py-2 flex items-center justify-center gap-3 flex-wrap"
      role="status"
      aria-live="polite"
    >
      <p className="text-sm text-stone-800 dark:text-stone-200 font-medium">
        New version available. Refresh to update.
      </p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="rounded-button px-3 py-1.5 text-sm font-medium bg-primary text-white hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
      >
        Refresh
      </button>
    </div>
  );
}
