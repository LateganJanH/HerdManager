# Firestore sync – step-by-step walkthrough

Follow these steps in order. Stop and fix any step that fails before continuing.

---

## Part A: Set Firestore rules (do this first)

1. Open **[Firebase Console](https://console.firebase.google.com)** in your browser and select your project (e.g. **herdmanager-b06d8**).

2. In the left sidebar, click **Build** → **Firestore Database**.
   - If you see “Create database”: click it, choose a location (e.g. `nam5` or your region), start in **test mode** for now, then Create. Wait until the database is ready.
   - If you already have a database, continue.

3. In the Firestore section, open the **Rules** tab (top of the page).

4. You’ll see a code editor. **Replace everything** in the editor with the rules below (copy the block from `rules_version` through the closing `}`):

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

5. Click **Publish**. Wait until you see a success message.

6. **Firebase Storage (for photos):** In the left sidebar, click **Build** → **Storage**. If you see “Get started”, click it and choose a location (same as Firestore if possible), then **Done**. Open the **Rules** tab and set:

   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /users/{userId}/{allPaths=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```
   Click **Publish**. This lets the app upload and read photo files under `users/{uid}/photos/`.

Rules are now set. Next: sync on the device and check the web.

---

## Part B: Sync on device and refresh web

7. On your **Android emulator/device**:
   - Open the HerdManager app.
   - Make sure you’re **signed in** (e.g. Farm Settings shows your email). If not, sign in with the same email/password you use on the web.
   - Go to **Home** (or **Herd**).
   - Trigger **Sync**: tap the **Sync** button and/or **pull down to refresh**.
   - If you see “Sync failed” or an error, stop and fix that first (often Firestore rules or network).
   - If sync succeeds, go to **Settings** and confirm **Last synced** shows a recent time.

8. On your **computer**:
   - Open the web dashboard (e.g. `http://localhost:3000`).
   - **Sign in** with the **same** email and password as on the device (if not already).
   - **Refresh the page** (F5 or Ctrl+R).
   - Check the **Home** tab: numbers (total animals, due soon, etc.) should reflect what’s on the device if you have data there.

If the web still shows zeros or “sample” data, go to Part C.

---

## Part C: Checklist (if sync still doesn’t show on web)

Work through each item. Fix anything that’s wrong or missing.

### C1. Firestore database exists

### C1b. Storage is enabled (for photos)

- Firebase Console → **Build** → **Storage**. If Storage is not set up, get started and add the rules from Part A step 6. Photos sync only when Storage is enabled and rules allow read/write for `users/{userId}/**`.

- Firebase Console → **Build** → **Firestore Database**.
- You should see “Cloud Firestore” and a **Data** tab (and **Rules**). If not, create the database (see Part A, step 2).

### C2. Rules are exactly as above

- Firestore → **Rules** tab. The rules must include the `match /users/{userId}/{document=**}` block and `request.auth.uid == userId`. Re-paste the rules from Part A step 4 and click **Publish** again.

### C3. Email/Password sign-in is enabled

- Firebase Console → **Build** → **Authentication** → **Sign-in method**.
- **Email/Password** must be **Enabled**. If not, enable it and Save.

### C4. Same account on Android and web

- **Android:** Farm Settings (or account section) should show the same email as the web.
- **Web:** After sign-in, Settings (or account) should show that same email.
- Use the **exact same** email and password on both. Same Firebase project (same `google-services.json` and same `NEXT_PUBLIC_FIREBASE_*` in `web/.env.local`).

### C5. Sync really ran and “Last synced” updated

- On the device, run **Sync** again. Wait until there’s no “Syncing…” and no error.
- In app **Settings**, confirm **Last synced** shows a time from a few seconds/minutes ago.

### C6. Web has Firebase Web app ID

- Open `web/.env.local`. You must have a line like:
  - `NEXT_PUBLIC_FIREBASE_APP_ID=1:835662868459:web:xxxxxxxx`
- If it’s empty: Firebase Console → **Project settings** (gear) → **Your apps** → **Add app** → **Web** (</>) → register the app → copy the **App ID** into `NEXT_PUBLIC_FIREBASE_APP_ID` in `.env.local`.
- Restart the web dev server (`pnpm dev`) after changing `.env.local`.

### C7. Emulator has Google Play and SHA-1 (for virtual devices)

- **Google Play:** In Android Studio → Device Manager, your virtual device should use an image that includes **Google Play** (e.g. “Pixel 6 API 34” with “Google Play” in the name). If it says “Google APIs” only, create a new AVD with a **Google Play** system image.

- **SHA-1 for debug keystore:**  
  Firebase needs the SHA-1 of the keystore you use to run the app (usually the debug keystore).

  1. Get your debug keystore path. On Windows it’s usually:
     - `C:\Users\<YourUsername>\.android\debug.keystore`  
     (replace `<YourUsername>` with your Windows login name, e.g. `C:\Users\Jane\.android\debug.keystore`)
  2. Open PowerShell or Command Prompt and run (use your actual path):
     ```bash
     keytool -list -v -keystore C:\Users\YOUR_USERNAME\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
     If the keystore is in a different place, use that path. `keytool` is included with the JDK (Android Studio installs one).
  3. In the output, find the line **SHA1:** and copy the hex value (e.g. `A1:B2:C3:...`).
  4. In Firebase Console → **Project settings** → **Your apps** → your **Android** app → **Add fingerprint** → paste the SHA-1 → Save.

- **Internet:** The emulator must have network access (no special firewall blocking Firebase).

### C8. Web is signed in and reading Firestore

- On the web, you must be **signed in** (not just the app loaded). If you see a sign-in screen, sign in with the same account.
- After signing in, the dashboard reads from Firestore `users/<your-uid>/animals`, etc. If rules are correct and the device synced, data should appear after a refresh.

---

## Part D: Test photo sync and Storage

Use this to confirm that animal photos sync from Android to Firestore and that image files are stored in Firebase Storage so the web (and other devices) can display them.

### D1. Prerequisites

- Firebase Storage is set up (Part A step 6) and rules are published.
- You are on the **Blaze** plan (required for Storage); use a bucket in **us-central1**, **us-east1**, or **us-west1** for no-cost tier.
- Firestore and Auth are working (Parts B and C).

### D2. On Android

1. Open the HerdManager app and **sign in**.
2. Go to **Profiles** (Herd list) and open an animal (or create one).
3. On the animal detail screen, add at least one **photo** (camera or gallery).
4. Go back to **Home** or **Profiles** and trigger **Sync** (pull to refresh or tap Sync). Wait until you see “Synced” (no error).
5. If sync fails, check Storage rules and that the app has network access.

### D3. Verify in Firebase Console (optional)

- **Firestore:** Build → Firestore Database → **Data** → `users` → your user ID → **photos**. You should see documents; each should have `animalId`, `angle`, `capturedAt`, and ideally **storageUrl** (download URL).
- **Storage:** Build → Storage → **Files** → `users` → your user ID → **photos**. You should see `.jpg` files. If Firestore has `storageUrl` but Storage is empty, the Android app may not have uploaded (check sync again and app logs).

### D4. Verify on the web

1. Open the web dashboard and **sign in** with the same account.
2. Go to **Profiles** and click the animal you added a photo to. The detail modal should show a **Photos** section with thumbnail(s) if `storageUrl` is present.
3. In **Settings**, scroll to **Development** → **Verify photo sync** and click **Check photo sync**. You should see something like:  
   **“X photo(s) in Firestore, X with Storage URL. Sample URL loads: OK.”**  
   If “with Storage URL” is 0, sync from Android again and ensure Storage is enabled and rules are correct.

### D5. Second device (optional)

On another Android device (or emulator), sign in with the same account, sync, then open the same animal. The photo(s) should appear (loaded from Storage URL).

### D6. If Firestore has photos but “0 with Storage URL”

- The app uploads each photo file to Firebase Storage using a **content URI** (FileProvider). If uploads fail, check:
  - **Storage rules** are published and allow `read, write` for `users/{userId}/{allPaths=**}` when `request.auth.uid == userId`.
  - **Blaze plan** and a bucket in a supported region (e.g. us-central1).
- On Android, after syncing, check **Logcat** (filter by `SyncRepository`). You’ll see `Storage upload failed for photo …` with the error if the upload failed (e.g. permission denied, network).

---

## Summary order

1. Set Firestore and Storage rules (Part A) and Publish.
2. Sync on device, refresh web (Part B).
3. If it still doesn’t show, go through Part C (C1–C8) and fix any missing or incorrect item.
4. To confirm photos: add a photo on Android, sync, then use Part D (web Settings → Verify photo sync and/or Profiles → animal detail).

The most common fix is **correct Firestore rules** (Part A) and **same account + successful sync** (Part B). Use Part C when you need to double-check everything. Use Part D to test photo sync and Storage.
