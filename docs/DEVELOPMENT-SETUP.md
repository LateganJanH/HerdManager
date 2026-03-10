# HerdManager Development Setup

## Prerequisites

### All Platforms

- **Git** – Version control
- **IDE** – VS Code (web) or Android Studio (Android)

### Android

- **JDK 17+** – Bundled with Android Studio or install OpenJDK
- **Android Studio** – Koala 2024.1+ (or latest)
- **Android SDK** – API 34+ (Android 14)
- **Physical device or emulator** – Android 10+ (API 29+)

**Windows (command line):** Gradle requires `JAVA_HOME` to be set. Two options:

**Option A – script (recommended)**  
From the `android` folder in PowerShell:
```powershell
cd android
.\set-java-home.ps1
```
This sets `JAVA_HOME` for the current session using a JDK it finds (Android Studio’s `jbr`, Microsoft JDK, etc.). To only print the path: `.\set-java-home.ps1 -ShowOnly`.

**Option B – set manually**  
In PowerShell (once per session):
```powershell
# Android Studio bundled JDK (most common)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Or Microsoft OpenJDK 17 (if installed)
# $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.13"
```
To find the path: in Android Studio go to **File → Settings → Build, Execution, Deployment → Build Tools → Gradle** and check **Gradle JDK**.  
To run Gradle from the project directory use `.\gradlew.bat` (not `gradlew.bat`).

### Web

- **Node.js** – 20.x LTS
- **pnpm** – `npm install -g pnpm`

## Environment Setup

### 1. Clone and Open

```bash
git clone <repo-url> HerdManager
cd HerdManager
```

**Large files:** The repo may contain PRD PDFs and a `Videos/` folder in history. `Videos/` is in `.gitignore` so new video files are not committed. To shrink a clone you can shallow-clone or add more patterns to `.gitignore` and use `git rm -r --cached <path>` in a follow-up commit (see CONTRIBUTING).

### 2. Android Setup

```bash
cd android
```

1. Open `android/` in Android Studio (File → Open)
2. Android Studio will sync Gradle and create the wrapper if needed
3. Accept SDK licenses when prompted: `sdkmanager --licenses`
4. Sync Gradle (File → Sync Project with Gradle Files)
4. Create `local.properties` with:

   ```
   sdk.dir=/path/to/your/Android/sdk
   ```

5. Add Firebase config (create project at console.firebase.google.com):
   - Download `google-services.json` → `android/app/`
   - Add Firebase dependencies (already in build.gradle)

6. Run on device/emulator: **Run** → Select device

### 3. Web Setup

```bash
cd web
pnpm install
pnpm dev
```

Open http://localhost:3000

### 4. Firebase (Shared Backend)

1. Create project at [Firebase Console](https://console.firebase.google.com)
2. Enable:
   - Authentication (Email, Google)
   - Firestore
   - Storage
   - (Optional) Cloud Functions
3. Copy config to:
   - Android: `google-services.json`
   - Web: `.env.local` (see `web/.env.example`)
4. **Optional (web API):** For `GET /api/stats` and `GET /api/devices` to return real data when the client sends a Bearer ID token, set `FIREBASE_SERVICE_ACCOUNT_JSON` in `web/.env.local` to the full JSON of a Firebase service account key (or use `GOOGLE_APPLICATION_CREDENTIALS`). See `web/.env.example`.
5. **Optional (Option B – Cloud Functions):** Deploy callable functions for stats and devices so the web can stay static/serverless. From repo root: `cd functions && npm install && npm run build`, then `firebase deploy --only functions`. In `web/.env.local` set `NEXT_PUBLIC_USE_STATS_VIA_CALLABLE=true` to use them when signed in. See [functions/README.md](../functions/README.md).

### 5. Multi-instance provisioning (optional)

When running or provisioning **one instance per farm** (multi-instance), use the solution registry and scripts in repo root `scripts/`:

- **Create a solution:** `node scripts/create-solution.js [--name "Farm Name"]` — generates `solutionId`, adds entry to `scripts/solution-registry.json`, prints next steps.
- **Web env for a solution:** `node scripts/env-for-solution.js <solutionId> [--support-url <url>]` — prints `NEXT_PUBLIC_SOLUTION_ID` and `NEXT_PUBLIC_SUPPORT_URL` and the Android Gradle command.
- **Update registry after deploy:** `node scripts/update-solution.js <solutionId> --firebase-project-id <id> --web-url <url>`.
- **List solutions (deploy loops):** `node scripts/list-solutions.js [--ids | --project-ids | --json | --deployable]`.
- **Validate registry:** `node scripts/validate-registry.js` — checks for duplicate `solutionId` and required fields; exit 1 if invalid.

Set `NEXT_PUBLIC_SOLUTION_ID` and `NEXT_PUBLIC_SUPPORT_URL` in `web/.env.local` or build env to see support links and instance ID in Settings → About. Full flow: [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) §5.

## Project Commands

### Android

```bash
cd android
./gradlew build          # Build (Unix/macOS)
./gradlew assembleDebug  # Debug APK
```

**Windows (PowerShell):** Use the wrapper in the current directory:

```powershell
cd android
.\gradlew.bat build
.\gradlew.bat :app:assembleDebug
```

Ensure `JAVA_HOME` is set (see Prerequisites → Android → Windows) before running these.

### Web

```bash
cd web
pnpm dev     # Dev server
pnpm build   # Production build
pnpm lint    # Lint
```

## Coding Standards

- **Android**: Kotlin, [Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- **Web**: TypeScript, ESLint + Prettier
- **Commits**: Conventional Commits (feat:, fix:, docs:, etc.)

## Testing

- **Web:** Unit tests: `pnpm test:run` (or `pnpm test` for watch). E2E: `pnpm test:e2e` (Playwright; starts dev server). See [web/README.md](web/README.md) and [CONTRIBUTING.md](CONTRIBUTING.md).
- **Android:** Unit: `.\gradlew.bat :app:testDebugUnitTest` (Windows). Instrumented (device/emulator): `.\gradlew.bat :app:connectedDebugAndroidTest`. See [android/README.md](android/README.md).

## Release and update policy

Versioning, changelog, and deploy steps: [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md). For software-update practices (Android version code, In-App Update, web "New version available" refresh, min-version checks), see [MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md) § Software updates. Changelog: [web/CHANGELOG.md](../web/CHANGELOG.md).

## CI and full verification

On GitHub, push/PR to `main` triggers [Web CI](.github/workflows/web-ci.yml) (when `web/` or `shared/` change), [Android CI](.github/workflows/android-ci.yml) (when `android/` change), and [Scripts CI](.github/workflows/scripts-ci.yml) (when `scripts/` change; validates solution registry). To run the same checks locally, see [CONTRIBUTING.md](CONTRIBUTING.md) § Full verification.
