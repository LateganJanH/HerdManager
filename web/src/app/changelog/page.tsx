import { readFileSync } from "fs";
import { join } from "path";
import Link from "next/link";
import { APP_NAME } from "../lib/version";

export const metadata = {
  title: `Changelog | ${APP_NAME}`,
  description: "Release notes and changelog for the HerdManager web dashboard",
};

export default function ChangelogPage() {
  let content: string;
  try {
    content = readFileSync(join(process.cwd(), "CHANGELOG.md"), "utf-8");
  } catch {
    content = "Changelog is not available.";
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center gap-4">
        <Link
          href="/?tab=settings"
          className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
        >
          ← Back to Settings
        </Link>
      </div>
      <h1 className="text-2xl font-semibold text-stone-900 dark:text-stone-100 mb-4">
        Changelog
      </h1>
      <p className="text-stone-600 dark:text-stone-300 text-sm mb-6">
        Release notes for the {APP_NAME} web dashboard. For Android version and release steps, see the repository docs.
      </p>
      <pre className="whitespace-pre-wrap font-sans text-sm text-stone-700 dark:text-stone-300 bg-stone-50 dark:bg-stone-800/50 border border-stone-200 dark:border-stone-600 rounded-card p-5 overflow-x-auto">
        {content}
      </pre>
    </div>
  );
}
