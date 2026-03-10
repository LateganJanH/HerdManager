import { NextResponse } from "next/server";

/**
 * GET /api/v1 – Versioned API discovery (Phase Later).
 * Returns the API version and relative paths to available endpoints.
 * Use this to discover the versioned REST surface; animals and breeding-events are stubs (501) until implemented.
 */
export async function GET() {
  return NextResponse.json(
    {
      version: "v1",
      description: "HerdManager versioned REST API. Use for third-party or mobile-backend integration.",
      endpoints: {
        animals: "/api/v1/animals",
        "breeding-events": "/api/v1/breeding-events",
        "billing-status": "/api/v1/billing-status",
      },
      openapi: "/api/spec",
    },
    {
      headers: { "Cache-Control": "public, max-age=300" },
    }
  );
}
