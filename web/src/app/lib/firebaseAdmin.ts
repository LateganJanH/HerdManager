/**
 * Firebase Admin SDK — server-only. Used to verify ID tokens and read Firestore
 * in API routes (/api/stats, /api/devices). Do not import in client code.
 */

import { cert, getApps, initializeApp, type App } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

let adminApp: App | null = null;

function getAdminApp(): App {
  if (adminApp) return adminApp;
  const existing = getApps();
  if (existing.length > 0) {
    adminApp = existing[0] as App;
    return adminApp;
  }
  const key = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (key) {
    try {
      const serviceAccount = JSON.parse(key);
      adminApp = initializeApp({ credential: cert(serviceAccount) });
    } catch {
      adminApp = initializeApp();
    }
  } else {
    adminApp = initializeApp();
  }
  return adminApp;
}

/** Verify Bearer ID token; returns decoded token with uid, or null if invalid/missing. */
export async function verifyIdToken(
  authHeader: string | null
): Promise<{ uid: string } | null> {
  if (!authHeader?.startsWith("Bearer ")) return null;
  const idToken = authHeader.slice(7).trim();
  if (!idToken) return null;
  try {
    const auth = getAuth(getAdminApp());
    const decoded = await auth.verifyIdToken(idToken);
    return decoded?.uid ? { uid: decoded.uid } : null;
  } catch {
    return null;
  }
}

export function getAdminFirestore() {
  return getFirestore(getAdminApp());
}
