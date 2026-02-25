import { NextResponse } from "next/server";
import { readFileSync, existsSync } from "fs";
import { join } from "path";

/**
 * Serves the HerdManager OpenAPI 3 spec (shared/api/openapi.yaml).
 * In development reads from ../shared; in production uses public/openapi.yaml (copied at build).
 */
export async function GET() {
  const cwd = process.cwd();
  const candidates = [
    join(cwd, "..", "shared", "api", "openapi.yaml"),
    join(cwd, "public", "openapi.yaml"),
  ];
  for (const p of candidates) {
    if (existsSync(p)) {
      try {
        const body = readFileSync(p, "utf-8");
        return new NextResponse(body, {
          headers: {
            "Content-Type": "application/x-yaml",
            "Cache-Control": "public, max-age=3600",
          },
        });
      } catch {
        break;
      }
    }
  }
  return NextResponse.json(
    { error: "OpenAPI spec not found" },
    { status: 404 }
  );
}
