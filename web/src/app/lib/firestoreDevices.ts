/**
 * Fetch linked devices from Firestore (users/{uid}/devices).
 * Android writes a document per device on each sync; web shows them in Settings.
 */

import type { LinkedDevice } from "./linkedDevices";

export async function fetchLinkedDevicesFromFirestore(
  db: unknown,
  uid: string
): Promise<LinkedDevice[]> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.collection(d, "users", uid, "devices");
  const snap = await firestore.getDocs(ref);
  const devices: LinkedDevice[] = [];
  snap.docs.forEach((doc) => {
    const data = doc.data();
    const lastSyncAt = data.lastSyncAt;
    if (typeof lastSyncAt !== "number") return;
    devices.push({
      id: doc.id,
      name: (data.name as string) ?? "Android device",
      lastSyncAt,
      platform: (data.platform as string) ?? "Android",
    });
  });
  return devices.sort((a, b) => b.lastSyncAt - a.lastSyncAt);
}
