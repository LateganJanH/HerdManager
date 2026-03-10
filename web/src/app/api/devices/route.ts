import { NextResponse } from "next/server";
import { getAdminFirestore, verifyIdToken } from "../../lib/firebaseAdmin";

export type DeviceItem = { id: string; name: string; lastSyncAt: number };

/**
 * Linked field devices API. With valid Bearer ID token, returns devices from
 * Firestore (users/{uid}/devices). Otherwise returns an empty list.
 */
export async function GET(request: Request) {
  const authHeader = request.headers.get("Authorization");
  const decoded = await verifyIdToken(authHeader);

  if (!decoded?.uid) {
    return NextResponse.json(
      { devices: [] as DeviceItem[] },
      {
        headers: {
          "Cache-Control": "private, s-maxage=30, stale-while-revalidate=60",
        },
      }
    );
  }

  try {
    const db = getAdminFirestore();
    const snap = await db
      .collection("users")
      .doc(decoded.uid)
      .collection("devices")
      .get();

    const devices: DeviceItem[] = snap.docs.map((doc) => {
      const data = doc.data();
      const lastSyncAt = typeof data.lastSyncAt === "number" ? data.lastSyncAt : 0;
      return {
        id: doc.id,
        name: (data.name as string) ?? doc.id,
        lastSyncAt,
      };
    });

    return NextResponse.json(
      { devices },
      {
        headers: {
          "Cache-Control": "private, s-maxage=30, stale-while-revalidate=60",
        },
      }
    );
  } catch {
    return NextResponse.json(
      { devices: [] as DeviceItem[] },
      {
        headers: {
          "Cache-Control": "private, s-maxage=30, stale-while-revalidate=60",
        },
      }
    );
  }
}
