import { NextResponse } from "next/server";
import { getAdminFirestore, verifyIdToken } from "../../../lib/firebaseAdmin";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/** Normalize Firestore value (Timestamp, ms, or epoch day) to epoch day. */
function toEpochDay(value: unknown): number | null {
  if (value == null) return null;
  if (typeof value === "number") {
    if (Number.isNaN(value)) return null;
    if (value > 1e12) return Math.floor(value / 86400_000);
    return Math.floor(value);
  }
  if (
    typeof value === "object" &&
    value !== null &&
    "toDate" in value &&
    typeof (value as { toDate: () => Date }).toDate === "function"
  ) {
    const date = (value as { toDate: () => Date }).toDate();
    return Math.floor(date.getTime() / 86400_000);
  }
  return null;
}

/** Format epoch day as YYYY-MM-DD. */
function epochDayToDateString(epochDay: number): string {
  const ms = epochDay * 86400_000;
  return new Date(ms).toISOString().slice(0, 10);
}

/**
 * GET /api/v1/breeding-events – List breeding events for the authenticated user (versioned REST).
 * Requires Authorization: Bearer <Firebase ID token>. Returns 401 when missing or invalid.
 * When Firebase Admin is not configured, returns 503.
 */
export async function GET(request: Request) {
  const authHeader = request.headers.get("Authorization");
  const decoded = await verifyIdToken(authHeader);

  if (!decoded?.uid) {
    return NextResponse.json(
      {
        error: "Unauthorized",
        message:
          "Provide a valid Firebase ID token in Authorization: Bearer <token> to list breeding events.",
      },
      { status: 401, headers: { "Cache-Control": "no-store, max-age=0" } }
    );
  }

  try {
    const db = getAdminFirestore();
    const uid = decoded.uid;
    const snap = await db
      .collection("users")
      .doc(uid)
      .collection("breeding_events")
      .get();

    const breedingEvents = snap.docs.map((doc) => {
      const data = doc.data();
      const serviceDateEpoch = toEpochDay(data.serviceDate);
      const serviceDate =
        serviceDateEpoch != null
          ? epochDayToDateString(serviceDateEpoch)
          : "";
      const pregnancyCheckDateEpoch = toEpochDay(data.pregnancyCheckDateEpochDay);
      const pregnancyCheckDate =
        pregnancyCheckDateEpoch != null
          ? epochDayToDateString(pregnancyCheckDateEpoch)
          : null;
      const sireIds = Array.isArray(data.sireIds)
        ? (data.sireIds as unknown[]).map(String).filter(Boolean)
        : [];
      return {
        id: doc.id,
        animalId: (data.animalId as string) ?? "",
        sireIds,
        eventType: (data.eventType as string) ?? "AI",
        serviceDate,
        notes: (data.notes as string) || null,
        pregnancyCheckDateEpochDay:
          (data.pregnancyCheckDateEpochDay as number) ?? null,
        pregnancyCheckDate,
        pregnancyCheckResult: (data.pregnancyCheckResult as string) || null,
        createdAt: (data.createdAt as number) ?? null,
        updatedAt: (data.updatedAt as number) ?? null,
      };
    });

    return NextResponse.json(
      { breedingEvents },
      {
        headers: {
          "Cache-Control": "private, s-maxage=30, stale-while-revalidate=60",
        },
      }
    );
  } catch {
    return NextResponse.json(
      {
        error: "Service unavailable",
        message:
          "Firebase Admin is not configured or Firestore read failed. Set FIREBASE_SERVICE_ACCOUNT_JSON for server-side breeding events list.",
      },
      { status: 503, headers: { "Cache-Control": "no-store, max-age=0" } }
    );
  }
}
