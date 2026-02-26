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

## CI and full verification

On GitHub, push/PR to `main` triggers [Web CI](.github/workflows/web-ci.yml) (when `web/` or `shared/` change) and [Android CI](.github/workflows/android-ci.yml) (when `android/` change). To run the same checks locally, see [CONTRIBUTING.md](CONTRIBUTING.md) § Full verification.
