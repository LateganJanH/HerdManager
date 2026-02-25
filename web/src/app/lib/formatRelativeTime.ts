/**
 * Human-readable relative time for "last updated" / "last synced" labels.
 * @param ms - Timestamp in milliseconds (e.g. dataUpdatedAt)
 * @param now - Optional reference time for tests (default Date.now())
 */
export function formatRelativeTime(ms: number, now: number = Date.now()): string {
  const sec = (now - ms) / 1000;
  if (sec < 60) return "just now";
  if (sec < 3600) return `${Math.floor(sec / 60)} min ago`;
  const hours = sec / 3600;
  if (hours < 24) return `${Math.floor(hours)} ${Math.floor(hours) === 1 ? "hour" : "hours"} ago`;
  const days = hours / 24;
  if (days < 2) return "yesterday";
  if (days < 7) return `${Math.floor(days)} days ago`;
  const includeYear = days >= 365;
  return new Date(ms).toLocaleDateString(undefined, { month: "short", day: "numeric", ...(includeYear && { year: "numeric" }) });
}
