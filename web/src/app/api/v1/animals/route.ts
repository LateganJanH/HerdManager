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
 * GET /api/v1/animals – List animals for the authenticated user (versioned REST).
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
          "Provide a valid Firebase ID token in Authorization: Bearer <token> to list animals.",
      },
      { status: 401, headers: { "Cache-Control": "no-store, max-age=0" } }
    );
  }

  try {
    const db = getAdminFirestore();
    const uid = decoded.uid;
    const animalsSnap = await db
      .collection("users")
      .doc(uid)
      .collection("animals")
      .get();

    const animals = animalsSnap.docs.map((doc) => {
      const data = doc.data();
      const dobEpoch = toEpochDay(data.dateOfBirth);
      const dateOfBirth =
        dobEpoch != null ? epochDayToDateString(dobEpoch) : "";
      return {
        id: doc.id,
        earTagNumber: (data.earTagNumber as string) ?? "",
        rfid: (data.rfid as string) || null,
        name: (data.name as string) || null,
        sex: (data.sex as string) ?? "FEMALE",
        breed: (data.breed as string) ?? "",
        dateOfBirth,
        farmId: (data.farmId as string) ?? "",
        coatColor: (data.coatColor as string) || null,
        hornStatus: (data.hornStatus as string) || null,
        status: (data.status as string) ?? "ACTIVE",
        sireId: (data.sireId as string) || null,
        damId: (data.damId as string) || null,
        isCastrated: data.isCastrated === true,
      };
    });

    return NextResponse.json(
      { animals },
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
          "Firebase Admin is not configured or Firestore read failed. Set FIREBASE_SERVICE_ACCOUNT_JSON for server-side animal list.",
      },
      { status: 503, headers: { "Cache-Control": "no-store, max-age=0" } }
    );
  }
}
