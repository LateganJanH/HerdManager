import { NextResponse } from "next/server";
import {
  computeHerdStatsFromDocs,
  type AnimalDoc,
  type BreedingDoc,
  type CalvingDoc,
} from "../../lib/herdStatsFromDocs";
import { getAdminFirestore, verifyIdToken } from "../../lib/firebaseAdmin";
import { SAMPLE_STATS_DATA } from "../../lib/sampleStatsData";

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

/**
 * Herd stats API. With valid Bearer ID token, returns stats from Firestore for that user.
 * Without token or on error, returns sample data.
 */
export async function GET(request: Request) {
  const authHeader = request.headers.get("Authorization");
  const decoded = await verifyIdToken(authHeader);

  if (!decoded?.uid) {
    return NextResponse.json(SAMPLE_STATS_DATA, {
      headers: {
        "Cache-Control": "public, s-maxage=30, stale-while-revalidate=60",
      },
    });
  }

  try {
    const db = getAdminFirestore();
    const uid = decoded.uid;
    const [animalsSnap, breedingSnap, calvingSnap] = await Promise.all([
      db.collection("users").doc(uid).collection("animals").get(),
      db.collection("users").doc(uid).collection("breeding_events").get(),
      db.collection("users").doc(uid).collection("calving_events").get(),
    ]);

    const animals: AnimalDoc[] = animalsSnap.docs.map((doc) => {
      const data = doc.data();
      const dob = toEpochDay(data.dateOfBirth);
      return {
        id: doc.id,
        status: (data.status as string) || "ACTIVE",
        sex: (data.sex as string) || "FEMALE",
        dateOfBirth: dob != null ? dob : null,
        isCastrated: data.isCastrated as boolean | undefined,
      };
    });

    const breeding: BreedingDoc[] = breedingSnap.docs.map((doc) => {
      const data = doc.data();
      const serviceDate = toEpochDay(data.serviceDate);
      return {
        id: doc.id,
        pregnancyCheckResult: data.pregnancyCheckResult as string | undefined,
        serviceDate: serviceDate ?? undefined,
      };
    });

    const calving: CalvingDoc[] = calvingSnap.docs.map((doc) => {
      const data = doc.data();
      const actualDate = toEpochDay(data.actualDate);
      return {
        id: doc.id,
        breedingEventId: data.breedingEventId as string | undefined,
        actualDate: actualDate ?? undefined,
      };
    });

    const stats = computeHerdStatsFromDocs(animals, breeding, calving);
    return NextResponse.json(stats, {
      headers: {
        "Cache-Control": "private, s-maxage=30, stale-while-revalidate=60",
      },
    });
  } catch {
    return NextResponse.json(SAMPLE_STATS_DATA, {
      headers: {
        "Cache-Control": "public, s-maxage=30, stale-while-revalidate=60",
      },
    });
  }
}
