# HerdManager – Next steps

Prioritized list of development steps after MVP closure (auth, sync, alerts, theme, polish). Use this with [ANDROID-ROADMAP.md](../android/ANDROID-ROADMAP.md) and [MVP-DEFINITION.md](architecture/MVP-DEFINITION.md).

---

## 1. High priority

### 1.1 ~~Web dashboard – real data~~ ✅ Done

- **Done.** When the user is signed in, the web dashboard reads herd stats from Firestore (users/{uid}/animals, breeding_events, calving_events) via client-side SDK and shows From API with real synced data.
- **Fallback:** When Firebase is not configured or user is not signed in, the app uses /api/stats (sample) or mock data.
- **Options:**
  - **A.** Add Firebase Admin SDK in Next.js; protect API routes with session/Firebase ID token; read Firestore `users/{uid}/...` and aggregate stats for `/api/stats`, list devices for `/api/devices`.
  - **B.** Add a Cloud Function (or backend service) that aggregates stats and is called by the web with auth; web stays static/serverless.
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

- **Done.** Playwright E2E in `web/e2e/dashboard.spec.ts`: (1) open Home — shows main content (dashboard) or sign-in when Firebase configured; (2) skip link moves focus to main content; (3) Home: Refresh button and overview (Your herd at a glance, Overview region); (4) switch tabs (Profiles, Alerts, Analytics) via bottom nav; (5) keyboard shortcut 2 switches to Profiles tab; (6) open Settings from header menu and assert Settings/Appearance; (7) Settings About section: API spec link visible and GET /api/spec returns OpenAPI YAML; (8) toggle theme (Light/Dark radios) in Settings; (9) toggle “Use sample data” checkbox; (10) Profiles: open animal detail modal and close; (11) Profiles: herd list or empty state visible; (12) Analytics: Summary and Export; (13) Alerts: filters including Withdrawal; (14) Settings: when “Edit farm” is visible (Firestore + farm data), open form, assert Contacts section and “Add contact”, and Cancel closes it. Run: `pnpm test:e2e` (starts dev server automatically). For CI without Firebase: leave `NEXT_PUBLIC_FIREBASE_*` unset so the dashboard shows with sample data; the first test passes in both modes (dashboard or sign-in).

### 3.3 Documentation

- **Goal:** Keep READMEs and roadmaps accurate; add “How to connect web to Firestore” when implemented.
- **Done:** Android README has instrumented tests; web↔Firestore: see [FIREBASE-SYNC-WALKTHROUGH.md](FIREBASE-SYNC-WALKTHROUGH.md) and web `.env.local` with same Firebase project. Android: Gradle configuration cache enabled for faster builds; use `--no-configuration-cache` if a build misbehaves.
- **Done:** [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) – pre-release steps (CI, changelog, version, tag, deploy); [SECURITY.md](../SECURITY.md) for vulnerability reporting and secure dev.
- **Ongoing:** Update DESIGN-UI-AND-GAPS and android/README as new features land.
- **CI:** GitHub Actions: [web-ci.yml](../.github/workflows/web-ci.yml) (lint, unit tests, build, E2E when `web/` or `shared/` change); [android-ci.yml](../.github/workflows/android-ci.yml) (build and unit tests when `android/` change). No Firebase env in web CI; Android CI does not run instrumented tests (use a device/emulator locally).

### 3.4 Recent polish (post-Phase 2)

- **Android – Weight & health log CRUD:** Full CRUD for weight and health logs on animal detail: edit (pencil) in addition to add/delete; LogWeightDialog and AddHealthEventDialog support an optional `existing` record; ViewModels expose `updateWeightRecord` and `updateHealthEvent`; sync and backup include updates.
- **Android – Calving → herd:** When recording calving and creating a new calf, the calf is auto-assigned to the dam’s current herd; if the dam has a herd, `assignAnimalToHerd` is called for the calf after insert.
- **Android – Profiles list scroll:** Herd list (Profiles) uses `Modifier.weight(1f)` on the list block (loading skeleton, empty state wrapper, and PullToRefreshBox around LazyColumn) so the list gets a bounded height and scrolls correctly when many animals are present.
- **Web – Build stability:** Circular dependency between `mockHerdData` and `sampleStatsData` removed by introducing `herdStatsTypes.ts` (shared `HerdStats` type); avoids "Cannot read properties of undefined (reading 'call')" at runtime. If build fails with ENOENT on `.nft.json`, delete `.next` and run `pnpm run build` again.

---

## 4. Later (post-MVP / Phase 2)

- **Sync – multi-device, same info for everyone:** ~~Current sync is last-writer-wins~~ **Phase 2b done:** Download now merges by document (newer wins per doc; local-only docs kept). Upload remains full push. See [architecture/SYNC-DESIGN.md](architecture/SYNC-DESIGN.md). **Phase 2a (timestamps):** All Firestore uploads include `createdAt`/`updatedAt`. **Phase 2b:** Merge-by-document on Android: animals/herds/breeding use timestamp compare; other collections apply all remote.
- **Android:** ML Kit photo recognition, voice input. ~~Configurable gestation days~~ Done (Settings → Farm profile: Gestation length 250–320 days; due dates use it). ~~Sire/bull on breeding UI~~ Done (Record breeding dialog: optional multi-select sires). **Register animal as bottom sheet on large screens** done: when width ≥600dp, Profiles → Register animal opens in a ModalBottomSheet; on phones, full-screen navigation unchanged. **Pedigree (Phase 2):** Sire and dam on animal: Animal detail shows Sire/Dam; Edit animal has optional Pedigree section (sire/dam dropdowns); new calves get damId; sync and backup include sireId/damId. Web Profiles detail shows Sire/Dam when from Firestore.
- **Web:** ~~Real-time Firestore listeners for herd stats and Profiles list~~ Done: dashboard subscribes via Firestore `onSnapshot` for stats (`useHerdStats`), Profiles list (`useAnimalProfiles`), Alerts (`useAlerts`), Analytics by month (`useAnalyticsByMonth`), animal detail modal (`useAnimalDetail`), and farm settings (`useFarmSettings`). ~~Withdrawal-period alerts~~ Done: Alerts tab (filter, CSV), Home due-soon; health_events with withdrawalPeriodEnd in next 14 days. **Weaning weight due:** Alerts tab and Home “due soon” show calves whose weaning date (DOB + farm weaning age) is in the next 14 days or up to 30 days overdue, with no weight recorded in the window; Settings → Edit farm: Weaning age (days) 150–300 (default 200). **Full animal detail on Profiles** done: when signed in and synced, clicking an animal loads full detail from Firestore (breed, DOB, herd, breeding events, calving events, health events, photo count); photos are listed by count (view in app). **Analytics report** done: Summary shows total animals, due soon, open/pregnant, breeding events this year, calvings this year; CSV and PDF export includes all metrics; **“This year by month”** bar charts for calvings and breeding events (from Firestore). PDF herd export done on Android (Profiles → overflow → Export herd to PDF). Excel export optional later. **Farm settings editing on web** done: Settings shows farm profile from Firestore; “Edit farm” opens form (name, address, **multiple contacts** (name, phone, email), calving/pregnancy/gestation/**weaning age** days); save writes to Firestore and syncs to linked devices. Web as **central** editor for farm/config data when multi-device sync is implemented.
- **Instance-per-farm product strategy:** Sell one dedicated instance per farm (customer); provisioning, white-label, updates across instances; see [architecture/INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md).
- **Software updates:** Staged rollouts, in-app updates (Android), min-version checks; see [architecture/MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md) § Software updates.
- **Shared:** OpenAPI-backed REST or GraphQL for third-party integrations. **API spec discovery:** Web dashboard serves `GET /api/spec` (OpenAPI 3 YAML from `shared/api/openapi.yaml`); link in Settings → About. Prebuild copies spec to `web/public/openapi.yaml` for production.

---

## Quick reference

| Area        | Next step summary                                      |
|------------|----------------------------------------------------------|
| **Web**    | ~~Firebase Auth + Firestore stats; linked devices; full animal detail; real-time listeners; withdrawal alerts (Alerts tab, Home due-soon); photo display when Storage URLs available~~ Done. **Weaning weight due:** Alerts tab and Home due soon; Settings → Edit farm: Weaning age (days) 150–300. |
| **Android**| ~~Herd list pull-to-refresh sync; empty-state CTA; loading/skeleton; offline/sync indicator; sync on app resume; withdrawal alerts~~ Done. **Weaning weight due:** Alerts + Home due soon; Settings → Weaning age (days). **CRUD** for weight/health logs; **calving** auto-assigns calf to dam’s herd; **Profiles list** scroll fix (bounded LazyColumn). |
| **Tests**  | ~~Android instrumented test; web E2E for dashboard~~ Done. |
| **Docs**   | Keep DESIGN-UI-AND-GAPS and roadmaps in sync with implementation. |
