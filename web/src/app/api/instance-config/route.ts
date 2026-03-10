import { NextResponse } from "next/server";
import { SOLUTION_ID, SUPPORT_BASE_URL } from "../../lib/version";

/**
 * Returns the current instance (solution) config for multi-instance deployments.
 * Used by the dashboard and support tooling to know solutionId and support URL
 * without relying only on client env. Safe to call unauthenticated (no secrets).
 */
export async function GET() {
  return NextResponse.json(
    {
      solutionId: SOLUTION_ID || "",
      supportBaseUrl: SUPPORT_BASE_URL || "",
    },
    {
      headers: {
        "Cache-Control": "no-store, max-age=0",
      },
    }
  );
}
