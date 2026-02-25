import Link from "next/link";
import type { Metadata } from "next";
import { APP_NAME } from "./lib/version";

export const metadata: Metadata = {
  title: `Page not found | ${APP_NAME}`,
  robots: { index: false, follow: false },
};

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-stone-50 dark:bg-stone-900 px-4">
      <p className="text-sm font-semibold text-primary uppercase tracking-wider">
        {APP_NAME}
      </p>
      <h1 className="mt-4 text-2xl font-bold text-stone-900 dark:text-stone-100">
        Page not found
      </h1>
      <p className="mt-2 text-stone-600 dark:text-stone-300 text-center max-w-sm">
        The page you’re looking for doesn’t exist or has been moved.
      </p>
      <Link
        href="/"
        className="mt-6 rounded-button bg-primary px-4 py-2 text-white font-medium hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-stone-50 dark:focus-visible:ring-offset-stone-900"
      >
        Back to dashboard
      </Link>
    </div>
  );
}
