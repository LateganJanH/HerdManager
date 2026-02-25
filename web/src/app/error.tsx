"use client";

import { useEffect } from "react";
import { APP_NAME } from "./lib/version";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(`${APP_NAME} error:`, error);
  }, [error]);

  useEffect(() => {
    document.title = `Something went wrong | ${APP_NAME}`;
  }, []);

  return (
    <div
      className="min-h-screen flex flex-col items-center justify-center bg-stone-50 dark:bg-stone-900 px-4"
      role="alert"
      aria-live="assertive"
    >
      <p className="text-sm font-semibold text-primary uppercase tracking-wider">
        {APP_NAME}
      </p>
      <h1 className="mt-4 text-xl font-bold text-stone-900 dark:text-stone-100">
        Something went wrong
      </h1>
      <p className="mt-2 text-stone-600 dark:text-stone-300 text-center max-w-md">
        An error occurred. You can try again or return to the dashboard.
      </p>
      <div className="mt-6 flex gap-3">
        <button
          type="button"
          onClick={reset}
          className="rounded-button bg-primary px-4 py-2 text-white font-medium hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
        >
          Try again
        </button>
        <a
          href="/"
          className="rounded-button border border-stone-300 dark:border-stone-600 px-4 py-2 font-medium text-stone-700 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
        >
          Back to dashboard
        </a>
      </div>
    </div>
  );
}
