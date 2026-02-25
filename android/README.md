# HerdManager Android

Field app for cattle herd management: animals, breeding, calving, health, and sync with the web dashboard.

## Prerequisites

- **JDK 17+** (bundled with Android Studio or install separately)
- **Android Studio** (Koala 2024.1+ or latest)
- **Android SDK** API 34+
- **Firebase:** Add `google-services.json` to `app/` and enable Email/Password auth. See [FIREBASE-SETUP.md](FIREBASE-SETUP.md).

On **Windows**, if you build from the command line, set `JAVA_HOME` first. From the `android` folder run `.\set-java-home.ps1` (or set it manually). See [docs/DEVELOPMENT-SETUP.md](../docs/DEVELOPMENT-SETUP.md) for details.

## Build and run

**From Android Studio (recommended)**

1. Open the `android` folder (File → Open).
2. Sync Gradle (File → Sync Project with Gradle Files).
3. Run on a device or emulator (Run → Run 'app').

**From command line**

```bash
cd android
./gradlew :app:assembleDebug    # Unix/macOS
.\gradlew.bat :app:assembleDebug # Windows PowerShell
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Gradle configuration cache is enabled (`org.gradle.configuration-cache=true` in `gradle.properties`) for faster incremental builds. If a build misbehaves, use `--no-configuration-cache`.

### Unit tests

```bash
.\gradlew.bat :app:testDebugUnitTest   # Windows
./gradlew :app:testDebugUnitTest        # Unix/macOS
```

### Instrumented tests (E2E on device/emulator)

Requires a running emulator or connected device. Uses a fake signed-in user (no Firebase).

```bash
.\gradlew.bat :app:connectedDebugAndroidTest   # Windows
./gradlew :app:connectedDebugAndroidTest      # Unix/macOS
```

Tests cover: main app visible with fake auth; Add Animal flow (Profiles → Register animal → fill ear tag, breed, DOB → Save → back to list); herd list swipe-to-delete confirmation (swipe → “Remove animal?” dialog → Cancel keeps animal). See `app/src/androidTest/` and [NEXT-STEPS.md](../docs/NEXT-STEPS.md) §3.1.

### If you see a file-lock error (e.g. `Couldn't delete ... R.jar` on Windows), run `.\gradlew.bat --stop` then `.\gradlew.bat clean :app:testDebugUnitTest --no-daemon` and retry.

### If you see "Unable to initialize main class org.gradle.wrapper.GradleWrapperMain" or "NoClassDefFoundError: org/gradle/wrapper/IDownload"

The wrapper JAR may be missing. From the `android` folder run:

```powershell
.\fix-gradle-wrapper.ps1
```

Then run `.\gradlew.bat :app:assembleDebug` again. Alternatively, if you have Gradle installed (e.g. `winget install Gradle.Gradle`), run `gradle wrapper --gradle-version 9.2.1` in the `android` folder to regenerate the wrapper files.

## Project structure

- `app/src/main/java/com/herdmanager/app/` — App code
  - `ui/screens/` — Compose screens (Home, Herd list, Animal detail, Farm settings, etc.)
  - `data/` — Room DB, repositories
  - `domain/` — Models, repository interfaces
  - `navigation/` — Nav graph
  - `di/` — Hilt modules

## Features

- **Bottom nav (5 tabs):** Home, Profiles, Alerts (reproduction & calving), Analytics, Settings — aligned with web dashboard.
- **Auth:** Sign-in/sign-up (Firebase Email/Password); sign-out with confirmation in Settings.
- **Sync:** One-tap “Sync” and pull-to-refresh on Home and Analytics; “Last synced” and error + Dismiss when sync fails. Full sync and backup/restore in Settings.
- **Theme:** Dark / Light / System in Settings → Appearance (Farm profile).
- Herd list and animal profiles (ear tag, status, sex); add/edit animals (Save disabled until ear tag, breed, and date of birth are set); breeding events, calving, pregnancy checks, health events, photos.
- Farm settings (name, address, multiple contacts, calving/pregnancy reminder days, gestation length, herds); backup and restore (JSON export/import).
- CSV and PDF export of herd (Profiles → overflow → Export herd to CSV / Export herd to PDF); app version in Settings → About.

The web dashboard ([../web](../web)) displays herd stats and alerts; sync connects the app to Firestore when configured.

## Roadmap

See [ANDROID-ROADMAP.md](ANDROID-ROADMAP.md) for current state. MVP items (auth, cloud sync, photo geo-tag, pregnancy-check alerts) and polish (sync indicator, herd list skeleton, instrumented tests) are done. Next: [NEXT-STEPS.md](../docs/NEXT-STEPS.md) (docs, then Phase 2: multi-device sync, web animal detail, etc.).
