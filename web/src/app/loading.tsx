import { APP_NAME } from "./lib/version";

export default function Loading() {
  return (
    <div
      className="min-h-screen flex items-center justify-center bg-stone-50 dark:bg-stone-900"
      aria-live="polite"
      aria-busy="true"
    >
      <div className="flex flex-col items-center gap-4">
        <p className="text-sm font-semibold text-primary uppercase tracking-wider">
          {APP_NAME}
        </p>
        <div
          className="h-10 w-10 rounded-full border-2 border-primary border-t-transparent animate-spin"
          aria-hidden
        />
        <p className="text-sm text-stone-600 dark:text-stone-400">Loading…</p>
      </div>
    </div>
  );
}
