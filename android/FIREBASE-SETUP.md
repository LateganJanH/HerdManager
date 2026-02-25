# Firebase setup (required for auth)

The app uses **Firebase Authentication** (email/password) to gate access. You need a Firebase project and a config file before the app can build and run.

## Steps

1. **Create a Firebase project**  
   Go to [Firebase Console](https://console.firebase.google.com/) → Create project (or use an existing one).

2. **Register the Android app**  
   In the project overview, add an Android app with package name:  
   `com.herdmanager.app`

3. **Download `google-services.json`**  
   Download the config file from the console and place it in the **app** module root:
   ```
   android/app/google-services.json
   ```
   Do not commit real config with sensitive data to public repos; use environment-specific files or CI secrets if needed.

4. **Enable Email/Password sign-in**  
   In Firebase Console → Build → Authentication → **Get started** (if shown), then Sign-in method → Enable **Email/Password**.

5. **Enable Identity Toolkit API**  
   In [Google Cloud Console](https://console.cloud.google.com/apis/library) (same project as Firebase), search **Identity Toolkit API** and click **Enable**. Required for Auth; "configuration_not_found" often means this or step 4 was missed.

6. **Create a Firestore database**  
   Build → Firestore Database → Create database (test mode for dev; then add rules so users can only access `users/{userId}`).

7. **Sync and run**  
   Sync Gradle and run the app. You’ll see the sign-in screen; use **Create account** to register, then sign in.

**If sync from the device doesn't show on the web** (same account on both), see [SYNC-AND-FIREBASE.md](../docs/SYNC-AND-FIREBASE.md) for Firestore rules, emulator setup, and troubleshooting. **Firebase Dynamic Links** deprecation does not affect this app (we use email/password only).

## Firestore sync

Data is stored under `users/{userId}/` with subcollections: `animals`, `herds`, `herd_assignments`, `breeding_events`, `calving_events`, `health_events`, `weight_records`, `photos`, and `settings`. In **Settings → Sync** use **Sync now** to upload and download; **Last synced** shows when it last succeeded. Use security rules so `request.auth.uid == userId` for the path.

**Sync strategy:** Each sync **uploads** this device’s full data to Firestore, then **downloads** and **replaces** local with what’s in Firestore. So sync is bidirectional (device ↔ cloud), but **last writer wins** — there is no merge of changes from multiple devices. Use one primary device to avoid overwriting data, or sync devices in sequence so the same dataset is on all. For a target design (multi-device merge, central farm data), see [SYNC-DESIGN.md](../docs/architecture/SYNC-DESIGN.md).

## Auth errors (e.g. configuration_not_found)

See [TROUBLESHOOTING-AUTH.md](TROUBLESHOOTING-AUTH.md) for step-by-step fixes.

## Without Firebase

The Gradle build will **fail** if `google-services.json` is missing (the Google services plugin requires it). To build without Firebase you would need to remove the `com.google.gms.google-services` plugin and Firebase dependencies and optionally hide the auth gate (not covered here).
