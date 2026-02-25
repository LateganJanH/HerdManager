import { NextResponse } from "next/server";
import { APP_VERSION } from "../../lib/version";

/**
 * Health check for the web dashboard.
 * Use for load balancers, monitoring, or future sync/backend readiness.
 */
export async function GET() {
  return NextResponse.json(
    {
      ok: true,
      service: "herdmanager-web",
      version: APP_VERSION,
    },
    {
      headers: {
        "Cache-Control": "no-store, max-age=0",
      },
    }
  );
}
