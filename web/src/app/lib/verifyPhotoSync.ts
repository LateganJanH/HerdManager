/**
 * Verify photo sync: read Firestore photos for the current user and check
 * how many have Storage URLs and whether a sample URL loads.
 * Used by Settings "Verify photo sync" to test that Android upload + Storage work.
 */

export type PhotoSyncResult = {
  total: number;
  withStorageUrl: number;
  sampleUrlLoads: "ok" | "fail" | "none";
  error?: string;
};

export async function verifyPhotoSync(db: unknown, uid: string): Promise<PhotoSyncResult> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.collection(d, "users", uid, "photos");

  try {
    const snap = await firestore.getDocs(ref);
    const docs = snap.docs;
    let withStorageUrl = 0;
    let sampleUrl: string | null = null;

    docs.forEach((doc) => {
      const data = doc.data();
      const url = (data.storageUrl as string) || ((data.uri as string)?.startsWith("http") ? (data.uri as string) : null);
      if (url) {
        withStorageUrl++;
        if (!sampleUrl) sampleUrl = url;
      }
    });

    let sampleUrlLoads: "ok" | "fail" | "none" = "none";
    if (sampleUrl) {
      try {
        const res = await fetch(sampleUrl, { method: "GET", mode: "cors" });
        sampleUrlLoads = res.ok ? "ok" : "fail";
      } catch {
        sampleUrlLoads = "fail";
      }
    }

    return {
      total: docs.length,
      withStorageUrl,
      sampleUrlLoads,
    };
  } catch (e) {
    return {
      total: 0,
      withStorageUrl: 0,
      sampleUrlLoads: "none",
      error: e instanceof Error ? e.message : String(e),
    };
  }
}
