# HerdManager – Next steps

Prioritized list of development steps after MVP closure (auth, sync, alerts, theme, polish). Use this with [ANDROID-ROADMAP.md](../android/ANDROID-ROADMAP.md) and [MVP-DEFINITION.md](architecture/MVP-DEFINITION.md).

**Status:** High- and medium-priority items are complete. Ongoing: keep [DESIGN-UI-AND-GAPS.md](DESIGN-UI-AND-GAPS.md) and [android/README.md](../android/README.md) in sync; see §3.3. **Phase Later:** instance-per-farm and software-update practices when scaling. Version-refresh banner and release/update policy are done. **Feature objectives vs product vision:** See [architecture/FEATURE-OBJECTIVES-REVIEW.md](architecture/FEATURE-OBJECTIVES-REVIEW.md) for a gap analysis (industry benchmarks, PLF, AI, native capabilities) and prioritized enhancements.

---

## 1. High priority

### 1.1 ~~Web dashboard – real data~~ ✅ Done

- **Done.** When the user is signed in, the web dashboard reads herd stats from Firestore (users/{uid}/animals, breeding_events, calving_events) via client-side SDK and shows From API with real synced data.
- **Fallback:** When Firebase is not configured or user is not signed in, the app uses /api/stats (sample) or mock data.
- **Options:**
  - **A.** ~~Add Firebase Admin SDK~~ **Done.** Firebase Admin in Next.js; `/api/stats` and `/api/devices` accept `Authorization: Bearer <idToken>`; when valid, server reads Firestore `users/{uid}/...` and returns real stats/devices; otherwise sample stats or empty devices. Set `FIREBASE_SERVICE_ACCOUNT_JSON` (or `GOOGLE_APPLICATION_CREDENTIALS`) for server-side reads. See [DEVELOPMENT-SETUP.md](DEVELOPMENT-SETUP.md) § Firebase step 4.
  - **B.** ~~Add a Cloud Function~~ **Done.** Callable Cloud Functions `getHerdStats` and `getDevices` in `functions/`; deploy with `firebase deploy --only functions`. Web sets `NEXT_PUBLIC_USE_STATS_VIA_CALLABLE=true` to use them when signed in (web stays static; no Next.js server or Firebase Admin required for stats/devices). See [functions/README.md](../functions/README.md).
- **Outcome:** “From API” on web reflects real synced data from Android.

### 1.2 ~~Web – Firebase Auth~~ ✅ Done

- **Done.** Firebase Auth (email/password) added: sign-in and create-account on a dedicated SignIn screen when Firebase is configured; dashboard is gated (show SignIn when not authenticated). Sign out in **Settings → Account**. Add `NEXT_PUBLIC_FIREBASE_*` to `.env.local` (see `web/.env.example`). Same Firebase project as Android so one account works on both.

---

## 2. Medium priority (polish & parity)

### 2.1 ~~Android – Herd list pull-to-refresh sync~~ ✅ Done

- **Done.** Profiles (Herd list): pull-to-refresh calls `syncNow()`; sync error shown with Dismiss. `HerdListViewModel` now has `SyncRepository`, `syncNow()`, `isSyncing`, `syncError`, `clearSyncError()`.

### 2.2 ~~Android – Loading / skeleton states~~ ✅ Done

- **Done.** Animal detail screen shows a skeleton (photo placeholder, detail lines, section blocks) while the animal is loading. **Goal (list):** list screens could show skeleton where applicable instead of a single “Loading…” line where applicable.
- **Ref:** [DESIGN-UI-AND-GAPS.md](DESIGN-UI-AND-GAPS.md) – loading/skeleton states.

### 2.3 ~~Android – Empty state CTA~~ ✅ Done

- **Done.** Alerts (Breeding) empty state has a **“View herd (Profiles)”** button that navigates to the Herd list so the user can open an animal to record service.

### 2.4 ~~Offline / sync indicator~~ ✅ Done

- **Done.** Compact sync status strip at top of Home, Herd list, Analytics, and Farm profile (Settings). Shows Synced / Syncing / Saved offline / Sync failed with Dismiss; tap to sync (`SyncStatusStrip`).

### 2.5 ~~Sync on app resume~~ ✅ Done

- **Done.** When the user returns to the app (activity `ON_RESUME`), sync is triggered automatically so herd data stays fresh. Implemented in `AppNavigation` via `DisposableEffect` and `HerdSummaryViewModel.syncNow()`.
---

## 3. Lower priority (tests & docs)

### 3.1 ~~Android – Instrumented tests~~ ✅ Done

- **Done.** `androidTest` added with Hilt + Compose UI: (1) **withFakeAuth_showsMainApp** – with `TestAuthModule` (fake signed-in user), main app with "Home" is shown; (2) **addAnimalFlow_navigateFillSave_returnsToHerdList** – Profiles → Register animal → fill ear tag, breed, DOB → Save → assert back on herd list (app auto-navigates after success); (3) **deleteAnimalFlow_swipeShowsConfirmDialog_cancelKeepsAnimal** – add animal, swipe left, cancel dialog, assert animal still in list. Uses `FakeAuthRepository`; test runner uses `AndroidJUnitRunner` + `HiltTestApplication`. Add Animal screen auto-navigates back after save (snackbar shown in background so it does not block navigation).

### 3.2 ~~Web – E2E tests~~ ✅ Done

- **Done.** Playwright E2E in `web/e2e/dashboard.spec.ts`: (1) open Home — shows main content (dashboard) or sign-in when Firebase configured; (2) skip link moves focus to main content; (3) Home: Refresh button and overview (Your herd at a glance, Overview region); (4) switch tabs (Profiles, Alerts, Analytics) via bottom nav; (5) keyboard shortcut 2 switches to Profiles tab; (6) open Settings from header menu and assert Settings/Appearance; (7) Settings About: API spec button opens in-app modal; Close returns to Settings; GET /api/spec returns OpenAPI YAML; (8) toggle theme (Light/Dark radios) in Settings; (9) toggle “Use sample data” checkbox; (10) Profiles: open animal detail modal and close; (11) Profiles: herd list or empty state visible; (12) Analytics: Summary and Export; (13) Alerts: filters including Withdrawal; (14) Settings: when “Edit farm” is visible (Firestore + farm data), open form, assert Contacts section and “Add contact”, and Cancel closes it; (15) keyboard shortcut 5 → Settings tab; (16) Changelog link from Settings About → /changelog; (17) Support page with solutionId+topic; (18) Support page without query params (Get help). Run: `pnpm test:e2e` (starts dev server automatically). For CI without Firebase: leave `NEXT_PUBLIC_FIREBASE_*` unset so the dashboard shows with sample data; the first test passes in both modes (dashboard or sign-in).

### 3.3 Documentation

- **Goal:** Keep READMEs and roadmaps accurate; add “How to connect web to Firestore” when implemented.
- **Done:** [FIREBASE-SYNC-WALKTHROUGH.md](FIREBASE-SYNC-WALKTHROUGH.md) (connect web to Firestore); [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) (includes Deprecation and EOL subsection with changelog/release-note examples); [SECURITY.md](../SECURITY.md). **Release and update policy:** [DEVELOPMENT-SETUP.md](DEVELOPMENT-SETUP.md) § Release and update policy; [MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md). Web: "New version available" banner (GET /api/version; refresh prompt when deployed version differs from client). **Android docs:** `android/README.md` now documents the Transactions & Expenses features (Sales/Purchases/Expenses tabs, validation, expense categories, category filters) to match the latest implementation.
- **Ongoing:** Keep DESIGN-UI-AND-GAPS and android/README in sync with implementation, especially after Android UI or domain-model changes (e.g. new transaction fields or filters).
- **CI:** [web-ci.yml](../.github/workflows/web-ci.yml), [android-ci.yml](../.github/workflows/android-ci.yml), [scripts-ci.yml](../.github/workflows/scripts-ci.yml) (validates solution registry when `scripts/` change). No Firebase in web CI; Android CI does not run instrumented tests locally.

### 3.4 Recent polish (post-Phase 2)

- **Branding – Ferdinand bull logo:** Shared bull-head logo on Android (Home hero, launcher icon `ic_launcher_foreground`) and web (header, `favicon.svg`, `herdmanager-logo.svg`). Central UI tokens (`UiDefaults.CardShape`, `CardInnerPadding`) used on Herd list cards, Alerts cards, and alert banners.
- **Android – Material You (dynamic color):** On Android 12+ (API 31), `HerdManagerTheme` uses `dynamicLightColorScheme` / `dynamicDarkColorScheme` so surfaces and secondary colors follow the system accent; primary and primaryContainer stay HerdManager green. Disable with `HerdManagerTheme(dynamicColor = false)` if needed.
- **Android – Weight & health log CRUD:** Full CRUD for weight and health logs on animal detail: edit (pencil) in addition to add/delete; LogWeightDialog and AddHealthEventDialog support an optional `existing` record; ViewModels expose `updateWeightRecord` and `updateHealthEvent`; sync and backup include updates.
- **Android – Calving → herd:** When recording calving and creating a new calf, the calf is auto-assigned to the dam’s current herd; if the dam has a herd, `assignAnimalToHerd` is called for the calf after insert.
- **Android – Profiles list scroll:** Herd list (Profiles) uses `Modifier.weight(1f)` on the list block (loading skeleton, empty state wrapper, and PullToRefreshBox around LazyColumn) so the list gets a bounded height and scrolls correctly when many animals are present.
- **Android – Castration sync & CASTRATION health event:** Animal `isCastrated` is always uploaded as a boolean so it syncs reliably; download parses it safely. Health event type **CASTRATION** added; logging a castration event for a male sets the animal’s castrated flag; health event type parsing is defensive for unknown values from Firestore.
- **Web – Build stability:** Circular dependency between `mockHerdData` and `sampleStatsData` removed by introducing `herdStatsTypes.ts` (shared `HerdStats` type); avoids "Cannot read properties of undefined (reading 'call')" at runtime. If build fails with ENOENT on `.nft.json`, delete `.next` and run `pnpm run build` again.
- **Web – Chunk errors:** Playwright excluded from client bundle (next.config) to fix ChunkLoadError; `global-error.tsx` and `error.tsx` detect chunk load/timeout errors and show a “Refresh the page” CTA.
- **Web – Analytics by category:** “By category” section (Calves, Heifers, Cows, Bulls, Steers) on Analytics tab; sample and Firestore stats include `byCategory`; CSV/PDF export includes category breakdown.
- **Web – API spec viewer:** Settings → About: "API spec (OpenAPI 3)" opens an in-app modal with the OpenAPI YAML; Close or Escape returns to Settings; "Open in new tab" link for raw spec.
- **Web – Excel export:** Analytics → Export report (Excel): .xlsx with sheets Summary, By status, By sex, By category, Analytics, Calvings by month, Breeding by month (when data available).
- **Web – List loading skeletons:** Profiles (herd list) and Alerts show skeleton placeholders (pulse animation) while data is loading instead of a single “Loading…” line. Home "By status" and "Reproduction" cards show skeletons while stats are loading. Analytics tab shows a "By status & sex" skeleton card while stats are loading.
- **Web – Changelog:** Settings → About includes a “Changelog” link to `/changelog` (release notes for the web dashboard). Sitemap includes `/changelog` for discovery.
- **Web – E2E:** Changelog link test (Settings About → Changelog → /changelog page with "Back to Settings"). Keyboard shortcut 5 switches to Settings tab; Analytics Export test asserts CSV, Excel, and PDF buttons visible.
- **Multi-instance Phase 1:** Solution registry (`scripts/solution-registry.example.json`) and `node scripts/create-solution.js [--name "Farm"]` to create a new solution ID; web env `NEXT_PUBLIC_SOLUTION_ID` and `NEXT_PUBLIC_SUPPORT_URL`; Android BuildConfig `SOLUTION_ID` and `SUPPORT_BASE_URL` (via `-PsolutionId` / `-PsupportBaseUrl`); Settings → About shows Help & support, Suggest a feature, Report a problem when support URL is set (links include solutionId). **Phase 1b:** `node scripts/env-for-solution.js <solutionId> [--support-url <url>]` prints web env snippet and Android gradle command; `GET /api/instance-config` returns `{ solutionId, supportBaseUrl }`. See [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) §5.
- **Multi-instance support page:** `GET /support?solutionId=...&topic=suggest|report` — minimal support landing page; deploy same web app to support subdomain and set `NEXT_PUBLIC_SUPPORT_URL` so in-app links open it. E2E: GET /api/instance-config and /support page with solutionId and topic. create-solution.js suggests running env-for-solution.js after creating a solution. **update-solution.js:** `node scripts/update-solution.js <solutionId> [--firebase-project-id <id>] [--web-url <url>]` updates registry after provisioning; sitemap includes `/support`. **list-solutions.js:** `node scripts/list-solutions.js [--ids|--project-ids|--json|--deployable]` for deploy loops and ops (see functions/README.md § Deploy to all instances). **validate-registry.js:** `node scripts/validate-registry.js` checks registry for duplicate solutionIds and required fields (CI/pre-deploy). **Android:** Settings → About shows Instance: solutionId when BuildConfig.SOLUTION_ID is set (parity with web).
- **Shared – OpenAPI:** Spec description documents current /api routes and future /api/v1 versioning for new REST endpoints.
- **Android – Settings screen:** Nested horizontal scroll on Farm profile (Settings) was causing crashes; the outer `Row(horizontalScroll)` was removed so `HorizontalFilterChips` is the only horizontal scroller. `findActivity()` retained for Play In-App Update.

---

## 4. Later (post-MVP / Phase 2)

For a **prioritized list of Phase Later steps** and current status, see [PHASE-LATER-ROADMAP.md](PHASE-LATER-ROADMAP.md). Most items are done; remaining optional work: full provisioning automation (create Firebase project via API) and optional GraphQL.

- **Sync – multi-device, same info for everyone:** ~~Current sync is last-writer-wins~~ **Phase 2b done:** Download now merges by document (newer wins per doc; local-only docs kept). Upload remains full push. See [architecture/SYNC-DESIGN.md](architecture/SYNC-DESIGN.md). **Phase 2a (timestamps):** All Firestore uploads include `createdAt`/`updatedAt`. **Phase 2b:** Merge-by-document on Android: animals/herds/breeding use timestamp compare; other collections apply all remote. Optional: add updatedAt to more Room entities (see SYNC-DESIGN § Optional later).
- **Android:** ~~ML Kit photo recognition, voice input~~ **Done.** **Voice input:** Mic button on ear tag and breed fields (Register animal, Edit animal); uses system speech recognizer (RECORD_AUDIO). **ML Kit text recognition:** When a photo is added (camera or gallery) on animal detail, text is detected and shown in a snackbar (“Detected text: …”); dependency `com.google.mlkit:text-recognition:16.0.1`. Optional `onTextDetected` callback on PhotosSection. ~~Configurable gestation days~~ Done (Settings → Farm profile: Gestation length 250–320 days; due dates use it). ~~Sire/bull on breeding UI~~ Done (Record breeding dialog: optional multi-select sires). **Register animal as bottom sheet on large screens** done: when width ≥600dp, Profiles → Register animal opens in a ModalBottomSheet; on phones, full-screen navigation unchanged. **Pedigree (Phase 2):** Sire and dam on animal: Animal detail shows Sire/Dam; Edit animal has optional Pedigree section (sire/dam dropdowns); new calves get damId; sync and backup include sireId/damId. Web Profiles detail shows Sire/Dam when from Firestore.
- **Web:** ~~Real-time Firestore listeners for herd stats and Profiles list~~ Done: dashboard subscribes via Firestore `onSnapshot` for stats (`useHerdStats`), Profiles list (`useAnimalProfiles`), Alerts (`useAlerts`), Analytics by month (`useAnalyticsByMonth`), animal detail modal (`useAnimalDetail`), and farm settings (`useFarmSettings`). ~~Withdrawal-period alerts~~ Done: Alerts tab (filter, CSV), Home due-soon; health_events with withdrawalPeriodEnd in next 14 days. **Weaning weight due:** Alerts tab and Home “due soon” show calves whose weaning date (DOB + farm weaning age) is in the next 14 days or up to 30 days overdue, with no weight recorded in the window; Settings → Edit farm: Weaning age (days) 150–300 (default 200). **Full animal detail on Profiles** done: when signed in and synced, clicking an animal loads full detail from Firestore (breed, DOB, herd, breeding events, calving events, health events, photo count); photos are listed by count (view in app). **Analytics report** done: Summary shows total animals, due soon, open/pregnant, breeding events this year, calvings this year; CSV, **Excel**, and PDF export includes all metrics and “This year by month” sheets when data available; **“This year by month”** bar charts for calvings and breeding events (from Firestore). PDF herd export done on Android (Profiles → overflow → Export herd to PDF). **Excel export** done on web (Analytics → Export report (Excel): Summary, By status, By sex, By category, Analytics, Calvings by month, Breeding by month).
- **Instance-per-farm product strategy:** Sell one dedicated instance per farm (customer); provisioning, white-label, updates across instances; see [architecture/INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md). **Doc index:** [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) lists all multi-instance strategy docs and topics. **Implementation:** Use the provisioning checklist in that doc §3.1 when onboarding a new farm; automate with scripts or IaC (§3.2). **Strategy also covers:** unique solution/instance ID per deployment (§2.1); billing and procurement (subscription, payment provider, renewals; §7); support and feedback channels for instance owners (§6.1), with future AI support agent. **Billing implementation guide:** [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md) — link solutionId to Stripe (or equivalent), optional registry fields, webhooks, renewals/dunning, optional suspend access. **Phase Later billing tooling:** `create-solution.js` adds empty `billingCustomerId`/`plan`/`billingStatus` to new entries; `list-solutions.js` table shows plan and billingStatus; `scripts/link-billing.js` prints steps to create a Stripe customer with solutionId metadata and update the registry (or pass `--customer-id cus_xxx` to write the ID). OpenAPI defines a future `GET /api/v1/billing-status` response schema for when billing is implemented.
- **Software updates:** Staged rollouts, in-app updates (Android), min-version checks; see [architecture/MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md) § Software updates. **Android min-version:** Optional Firestore `users/{uid}/config/app` with `minVersionCode` (integer); if set and app’s versionCode &lt; min, user sees “Update required” and Open Play Store. See [firestore.rules.example](../firestore.rules.example). **Android In-App Update:** When "Update required" is shown, the app tries Play In-App Update (immediate flow) first; if available, user updates without leaving the app; otherwise "Open Play Store" is used.
- **Shared:** OpenAPI-backed REST or GraphQL for third-party integrations. **API spec discovery:** Web dashboard serves `GET /api/spec` (OpenAPI 3 YAML from `shared/api/openapi.yaml`); link in Settings → About. Prebuild copies spec to `web/public/openapi.yaml` for production. **API versioning:** Spec description notes current /api routes (stats, devices, health, version) and that future REST may use /api/v1 base path.

---

## Quick reference

| Area        | Next step summary                                      |
|------------|----------------------------------------------------------|
| **Web**    | ~~Firebase Auth + Firestore stats; linked devices; full animal detail; real-time listeners; withdrawal alerts (Alerts tab, Home due-soon); photo display when Storage URLs available~~ Done. **Weaning weight due:** Alerts tab and Home due soon; Settings → Edit farm: Weaning age (days) 150–300. **Multi-instance Phase 1:** solution ID, support URL, instance-config API, /support page, Settings About instance ID + support links; scripts: create/update/env-for-solution, list-solutions, validate-registry. |
| **Android**| ~~Herd list pull-to-refresh sync; empty-state CTA; loading/skeleton; offline/sync indicator; sync on app resume; withdrawal alerts~~ Done. **Weaning weight due:** Alerts + Home due soon; Settings → Weaning age (days). **CRUD** for weight/health logs; **calving** auto-assigns calf to dam’s herd; **Profiles list** scroll fix (bounded LazyColumn). **Transactions & expenses:** Transactions screen with tabs for Sales, Purchases, and Expenses; add/edit transaction form with validation, date picker, animal picker, contact fields, optional weight and price/kg, notes, and expense categories; expense categories screen for managing categories; transactions list shows categories, supports category filter chips, empty states, and delete confirmations. **Multi-instance:** BuildConfig SOLUTION_ID/SUPPORT_BASE_URL; Settings → About shows instance ID and support links when set. **Min-version check:** optional `users/{uid}/config/app` minVersionCode → “Update required” + Play Store. |
| **Tests**  | ~~Android instrumented test; web E2E for dashboard~~ Done. |
| **Docs**   | Keep DESIGN-UI-AND-GAPS and roadmaps in sync. Release/update policy: [DEVELOPMENT-SETUP.md](DEVELOPMENT-SETUP.md); web "New version available" banner via GET /api/version. **API:** GET /api/stats and GET /api/devices return real data with Bearer ID token when Firebase Admin is configured. |


