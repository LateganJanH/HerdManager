import { NextResponse } from "next/server";
import { SAMPLE_STATS_DATA } from "../../lib/sampleStatsData";

/**
 * Herd stats API. Returns sample data until sync/backend is connected.
 * Frontend can use this for server-rendered or server-fetched stats.
 */
export async function GET() {
  return NextResponse.json(SAMPLE_STATS_DATA, {
    headers: {
      "Cache-Control": "public, s-maxage=30, stale-while-revalidate=60",
    },
  });
}
