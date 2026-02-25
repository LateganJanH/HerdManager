# Sync and Firebase – troubleshooting

**New to this?** Follow the step-by-step guide first: **[FIREBASE-SYNC-WALKTHROUGH.md](FIREBASE-SYNC-WALKTHROUGH.md)** (Firestore rules → sync on device → refresh web → full checklist).

---

## Firebase Dynamic Links message

Firebase may show:

> The following authentication features will stop working when Firebase Dynamic Links shuts down: **email link authentication for mobile apps**, and Cordova OAuth for web apps.

**HerdManager does not use these features.** The app uses **email/password** sign-in only (no “email link” magic links, no Cordova). You can ignore this message for HerdManager; no code changes are required for the Dynamic Links shutdown.

---

## Sync from device not showing on web

If you’re signed in with the same account on the Android app and the web dashboard but data synced on the device doesn’t appear on the web, check the following.

### 1. Firestore database and rules

- In [Firebase Console](https://console.firebase.google.com) → your project → **Firestore Database**, ensure a database exists (Create database if needed).
- **Rules** must allow authenticated users to read/write their own `users/{userId}/...` path. If rules are too strict or miswritten, the Android app can’t upload and the web can’t read.

Example rules that match the app’s layout:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Paste (or adapt) this in Firestore → **Rules** → **Edit rules** → Publish.

### 2. Same account and same project

- **Web:** Sign in at the dashboard (e.g. `http://localhost:3000`) with your Firebase **email/password**.
- **Android:** In the app (e.g. Farm Settings), sign in with the **exact same** email and password.
- Both must use the **same Firebase project** (same `google-services.json` on Android, same `NEXT_PUBLIC_FIREBASE_*` in `web/.env.local`). Same project ⇒ same `uid` for that user ⇒ same Firestore path `users/{uid}/...`.

### 3. Sync actually ran on the device

- On the **Android device**, open **Home** or **Herd** and trigger **Sync** (or pull-to-refresh).
- If sync fails, the app should show an error (e.g. “Sync failed” with Dismiss). Fix any network or Firestore permission errors first.
- After a successful sync, **Last synced** in Settings should update. Then refresh or reopen the web dashboard; it reads from the same `users/{uid}/animals`, `breeding_events`, `calving_events`, etc.

### 4. Virtual device / emulator

- Use an **emulator image with Google Play** (not “Google APIs” only) so Firebase Auth and Firestore work correctly.
- In Firebase Console → Project settings → **Your apps** → Android app: add the **SHA-1** of your **debug keystore** (Android Studio: Build → Generate signed bundle / APK → debug; or run `keytool -list -v -keystore ~/.android/debug.keystore` and use the SHA-1). Without the correct SHA-1, Auth can fail or behave oddly on the emulator.
- Ensure the emulator has **internet** (no firewall blocking Firebase).

### 5. Web dashboard is reading Firestore

- The web shows **real** herd stats only when:
  - Firebase is configured in `web/.env.local` (all required `NEXT_PUBLIC_FIREBASE_*` set, including `NEXT_PUBLIC_FIREBASE_APP_ID` from the **Web** app in Firebase Console).
  - You’re **signed in** on the web with the same account as on the device.
- If the web shows “sample” or mock data, either you’re not signed in or Firestore returned no data (empty collections or rules blocking read).

### Quick checklist

| Check | Where |
|-------|--------|
| Firestore DB created | Firebase Console → Firestore |
| Rules allow `users/{userId}/...` for `request.auth.uid == userId` | Firestore → Rules |
| Email/password sign-in enabled | Authentication → Sign-in method |
| Same email/password on Android and web | App + browser |
| Sync run on device and “Last synced” updated | Android app |
| Web env has `NEXT_PUBLIC_FIREBASE_APP_ID` (Web app) | `web/.env.local` |
| Emulator has Google Play + SHA-1 in Firebase | Project settings → Android app |

Once these are in place, syncing on the device should make the same data appear on the web dashboard for that account.
