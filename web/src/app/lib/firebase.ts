/**
 * Firebase client initialization for web dashboard.
 * Uses env: NEXT_PUBLIC_FIREBASE_* (see .env.example).
 * Firebase is loaded only when config is present (dynamic import), so the app
 * runs without the firebase package if env vars are not set.
 */

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

export function isFirebaseConfigured(): boolean {
  return !!(
    firebaseConfig.apiKey &&
    firebaseConfig.authDomain &&
    firebaseConfig.projectId &&
    firebaseConfig.appId
  );
}

// Cached after first successful init (client-side only)
let authCache: import("firebase/auth").Auth | null = null;
let dbCache: import("firebase/firestore").Firestore | null = null;
let initPromise: Promise<{ auth: import("firebase/auth").Auth; db: import("firebase/firestore").Firestore } | null> | null = null;

async function initFirebase(): Promise<{ auth: import("firebase/auth").Auth; db: import("firebase/firestore").Firestore } | null> {
  if (typeof window === "undefined" || !isFirebaseConfigured()) return null;
  if (authCache && dbCache) return { auth: authCache, db: dbCache };
  if (initPromise) return initPromise;

  initPromise = (async () => {
    try {
      const [{ getApp, getApps, initializeApp }, { getAuth, connectAuthEmulator }, { getFirestore, connectFirestoreEmulator }] = await Promise.all([
        import("firebase/app"),
        import("firebase/auth"),
        import("firebase/firestore"),
      ]);
      const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
      const auth = getAuth(app);
      const db = getFirestore(app);
      if (process.env.NEXT_PUBLIC_FIREBASE_USE_EMULATORS === "true") {
        try {
          connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
          connectFirestoreEmulator(db, "127.0.0.1", 8080);
        } catch {
          // ignore
        }
      }
      authCache = auth;
      dbCache = db;
      return { auth, db };
    } catch {
      return null;
    }
  })();

  return initPromise;
}

/** Returns Firebase Auth instance after async init. Null if not configured or init failed. */
export async function getFirebaseAuth(): Promise<import("firebase/auth").Auth | null> {
  const result = await initFirebase();
  return result?.auth ?? null;
}

/** Returns Firestore instance after async init. Null if not configured or init failed. */
export async function getFirebaseDb(): Promise<import("firebase/firestore").Firestore | null> {
  const result = await initFirebase();
  return result?.db ?? null;
}
