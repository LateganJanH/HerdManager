import { NextResponse } from "next/server";

/**
 * Linked field devices API. Returns devices that have been linked to this
 * dashboard and their last sync time. Until a real backend (e.g. Firebase)
 * is connected, returns an empty list. The web UI can show mock devices
 * when sample data is enabled for demo purposes.
 */
export async function GET() {
  return NextResponse.json(
    { devices: [] as { id: string; name: string; lastSyncAt: number }[] },
    {
      headers: {
        "Cache-Control": "private, s-maxage=30, stale-while-revalidate=60",
      },
    }
  );
}
