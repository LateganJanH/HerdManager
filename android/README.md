# HerdManager Android

Field app for cattle herd management: animals, breeding, calving, health, and sync with the web dashboard.

## Prerequisites

- **JDK 17+** (bundled with Android Studio or install separately)
- **Android Studio** (Koala 2024.1+ or latest)
- **Android SDK** API 34+ (project uses compileSdk/targetSdk 35)
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
- **Transactions & expenses:** Transactions screen with tabs for **Sales**, **Purchases**, and **Expenses**; add/edit transaction form supports amount, date (date picker), notes, optional weight and price/kg, linked animal, buyer/seller contact, expense description, and expense category; empty states, validation (amount, required animal for sales/purchases, description or category for expenses), and delete-confirmation dialogs.
- **Expense categories:** Manage expense categories (create, rename, delete) from the Expenses tab; expense transactions can be assigned to a category; transactions list shows category labels for expenses and supports horizontal filter chips to filter by category.
- **Growth & weaning metrics:** Animal detail shows a recent growth summary when weight records are available (latest weight, date, days since, and average daily gain over the last interval). Herd analytics (Home “At a glance” and Herd summary) include average daily gain, average weaning weight, and **average BCS** (body condition) for the current herd; **By category** (Calves, Heifers, Cows, Bulls, Steers) and **By status / By sex** use the same current herd so totals add up. Headline herd-size metrics (`X head`) and due-soon/alerts exclude **SOLD**, **DECEASED**, and **CULLED** so counts reflect the active herd.
- **Tasks & reminders:** Farm-wide task board (Tasks screen from Home); summary (open / due today / overdue); status filter chips (All, Pending, In progress, Done); add/edit tasks (title, notes, due date, optional animal link); long-press delete with confirmation; backup/restore and sync include tasks.
- **Body condition scoring (BCS):** Condition section on animal detail (score history list, “Record score” with date picker and score 1–9, optional notes; edit/delete per record); Analytics shows average BCS for the current herd and optional BCS distribution (count per score 1–9); condition records sync and are included in backup/restore.
- **FCM due-soon notifications:** Push notifications for due-soon alerts; device token stored in Firestore; Cloud Function sends scheduled due-soon notifications with body breakdown (e.g. "2 calving due soon") and data payload (count, calvingDue, pregnancyCheckDue, etc.); **tapping the notification deep-links to the Alerts tab**; notification permission and messaging service registered.
- **At-risk / predictive insights:** Home "Attention needed" card shows open cows/heifers (no breeding in 60 days) and calves with weaning weight overdue; tap card to open Alerts or tap a line to open animal detail.
- Farm settings (name, address, multiple contacts, calving/pregnancy reminder days, gestation length, weaning age 150–300 days, herds); backup and restore (JSON export/import). Settings screen uses tabs (Farm, Operations, Herds, Sync, System, Data, About) via `HorizontalFilterChips`; a previous crash from nested horizontal scrolling was fixed by letting the chip row handle scrolling exclusively.
- CSV and PDF export of herd (Profiles → overflow → Export herd to CSV / Export herd to PDF); app version in Settings → About.
- **Voice & ML Kit:** **Voice input:** Mic button on ear tag and breed (Register animal, Edit animal) and on Herd list search; uses system speech recognition. **ML Kit text recognition:** When adding a photo on animal detail, text in the image is detected and shown in a snackbar with a **Copy** action to copy the text (e.g. for ear tag photos).
- **UI & branding:** **Home:** Top app bar title "Home"; a **HerdManager branding tile** (logo + "HerdManager") above the hero card; **"Your herd at a glance"** card shows **farm name** (from Farm settings), then "Your herd at a glance" and quick overview. Ferdinand-style bull logo on launcher icon; shared UI tokens (`UiDefaults`) for cards and spacing on Home, Profiles, and Alerts; **loading skeletons** on animal detail and herd list (and Alerts where applicable); compact filter dropdowns (Herd, Status, Category, Sort) on Profiles and horizontal chip filters on Alerts.
- **Updates:** Optional min-version check via Firestore `users/{uid}/config/app` (field `minVersionCode`). When update is required, the app tries Play In-App Update (immediate flow) first; if not available, "Open Play Store" is shown. In Settings → About, **Check for updates** runs the flexible in-app update flow when an update is available from Play.
- **Multi-instance:** When building for a specific solution (instance per farm), set Gradle properties `-PsolutionId=<id>` and `-PsupportBaseUrl=<url>`. Then Settings → About shows **Instance:** <var>solutionId</var> (when set) and **Help & support**, **Suggest a feature**, and **Report a problem** that open the support portal with the solution ID. Use `node scripts/create-solution.js` (in repo root) to create a new solution ID. See [MULTI-INSTANCE-STRATEGY.md](../docs/MULTI-INSTANCE-STRATEGY.md) §5.

The web dashboard ([../web](../web)) displays herd stats and alerts; sync connects the app to Firestore when configured. Web has **Tasks** tab with full CRUD (add/edit/delete tasks, status filters) and **Weight & weaning** on Profiles animal detail (log weight, edit, delete) in parity with the Android app.

## Roadmap

See [ANDROID-ROADMAP.md](ANDROID-ROADMAP.md) for current state. MVP items (auth, cloud sync, photo geo-tag, pregnancy-check alerts) and polish (sync indicator, herd list skeleton, instrumented tests) are done. Next: [NEXT-STEPS.md](../docs/NEXT-STEPS.md) (docs, then Phase 2: multi-device sync, web animal detail, etc.).
