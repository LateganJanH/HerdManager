import Link from "next/link";
import { APP_NAME } from "../lib/version";

export const metadata = {
  title: `Support | ${APP_NAME}`,
  description: "Help, feedback, and support for HerdManager instance",
};

type SearchParams = { solutionId?: string; topic?: string };
type Props = { searchParams?: Promise<SearchParams> };

export default async function SupportPage(props: Props) {
  const searchParams: SearchParams = await (props.searchParams ?? Promise.resolve({} as SearchParams));
  const solutionId = typeof searchParams.solutionId === "string" ? searchParams.solutionId.trim() : "";
  const topic = typeof searchParams.topic === "string" ? searchParams.topic.trim().toLowerCase() : "";

  const isSuggest = topic === "suggest";
  const isReport = topic === "report";

  return (
    <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center gap-4">
        <Link
          href="/?tab=settings"
          className="text-sm font-medium text-primary hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded"
        >
          ← Back to dashboard
        </Link>
      </div>
      <h1 className="text-2xl font-semibold text-stone-900 dark:text-stone-100 mb-2">
        Help &amp; support
      </h1>
      <p className="text-stone-600 dark:text-stone-300 text-sm mb-6">
        {APP_NAME} — support and feedback for your instance.
      </p>

      {solutionId && (
        <p className="text-sm text-stone-500 dark:text-stone-400 mb-6" data-testid="support-solution-id">
          Instance: <strong>{solutionId}</strong>
        </p>
      )}

      {isSuggest && (
        <section className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 mb-6">
          <h2 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-2">Suggest a feature</h2>
          <p className="text-stone-600 dark:text-stone-300 text-sm">
            Share an idea or improvement. Your instance ID is included so we can prioritise feedback.
          </p>
          <p className="mt-3 text-sm text-stone-500 dark:text-stone-400">
            Contact your administrator or use the support channel provided with your deployment.
          </p>
        </section>
      )}

      {isReport && (
        <section className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 mb-6">
          <h2 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-2">Report a problem</h2>
          <p className="text-stone-600 dark:text-stone-300 text-sm">
            Describe the issue you encountered. Your instance ID is included to help us investigate.
          </p>
          <p className="mt-3 text-sm text-stone-500 dark:text-stone-400">
            Contact your administrator or use the support channel provided with your deployment.
          </p>
        </section>
      )}

      {!isSuggest && !isReport && (
        <section className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-5 mb-6">
          <h2 className="text-lg font-semibold text-stone-900 dark:text-stone-100 mb-2">Get help</h2>
          <p className="text-stone-600 dark:text-stone-300 text-sm mb-3">
            Use the links in the dashboard (Settings → About) to suggest a feature or report a problem. Your instance is identified automatically.
          </p>
          <p className="text-sm text-stone-500 dark:text-stone-400">
            For deployment-specific support, contact your administrator or refer to the documentation that came with your instance.
          </p>
        </section>
      )}
    </div>
  );
}
