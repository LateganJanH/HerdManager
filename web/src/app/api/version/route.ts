import { NextResponse } from "next/server";
import { APP_VERSION } from "../../lib/version";

/**
 * Returns the deployed app version for "New version available" checks.
 * Client compares this to built-in APP_VERSION and shows a refresh prompt when they differ.
 */
export async function GET() {
  return NextResponse.json(
    { version: APP_VERSION },
    {
      headers: {
        "Cache-Control": "no-store, max-age=0",
      },
    }
  );
}
