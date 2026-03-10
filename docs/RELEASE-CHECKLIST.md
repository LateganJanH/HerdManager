# Pre-release checklist

Use this before cutting a versioned release (e.g. v1.0.0) or shipping to testers.

## Before release

- [ ] **CI green** – Web: `cd web && pnpm run ci && pnpm test:e2e`. Android: `cd android && ./gradlew :app:assembleDebug :app:testDebugUnitTest`. If you changed `scripts/`, Scripts CI runs on push (or run `cp scripts/solution-registry.example.json scripts/solution-registry.json && node scripts/validate-registry.js` from repo root). See [CONTRIBUTING.md](../CONTRIBUTING.md) § Full verification.
- [ ] **Changelog** – Move [Unreleased] entries in `web/CHANGELOG.md` into a new `[X.Y.Z]` section with release date. Optionally add an Android changelog if you maintain one.
- [ ] **Version** – Bump app version where applicable:
  - **Web:** `NEXT_PUBLIC_APP_VERSION` in `.env` or build env (default in code is 0.1.0); also in `web/public/manifest.json` / manifest generation if used.
  - **Android:** `versionName` / `versionCode` in `android/app/build.gradle.kts` (or equivalent).
- [ ] **Secrets** – No real API keys or `.env.local` committed; use [SECURITY.md](../SECURITY.md) practices.

## Tagging and deploy

- [ ] **Tag** – e.g. `git tag -a v1.0.0 -m "Release 1.0.0"` and push: `git push origin v1.0.0`.
- [ ] **Web deploy** – If you use Vercel/Netlify/etc., trigger deploy from `main` or from the new tag per your pipeline. Ensure `pnpm run prebuild` (or equivalent) runs so `public/openapi.yaml` is present for production.
- [ ] **Android** – Build release AAB/APK per [android/README.md](../android/README.md); distribute via Play Store internal track, direct APK, or your chosen channel.

## Optional

- **Release notes** – Copy changelog summary into GitHub Releases (or your release page) when creating the tag.
- **Smoke test** – After deploy, open the web app and run through: sign-in (if configured), Home, Profiles, Alerts, Analytics, Settings → About (API spec link). On Android: open app, sync, add/view an animal.
- **Multi-instance (Phase Later)** – When releasing across multiple instance-per-farm deployments: (1) For each solution in your registry, set web env (`NEXT_PUBLIC_SOLUTION_ID`, `NEXT_PUBLIC_SUPPORT_URL`) and deploy the web dashboard to that instance’s URL. (2) Deploy Cloud Functions and Firestore rules to each Firebase project: run `./scripts/deploy-all-instances.sh` (Unix) or `.\scripts\deploy-all-instances.ps1` (Windows) from repo root, or loop manually: `for projectId in $(node scripts/list-solutions.js --project-ids); do firebase use "$projectId" && firebase deploy --only functions,firestore:rules; done`. Optionally run `node scripts/validate-registry.js` before deploy. Use the provisioning checklist in [INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md) §3.1 when onboarding a new farm; see [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) §5 for scripts.

## Deprecation and EOL

For critical security or data-safety fixes, consider a short grace period after release, then reject older versions (e.g. block sign-in or sync) and show “Update required”. In CHANGELOG and release notes, mention breaking changes and required actions, for example:

- *“Update to 1.3.0 before 2026-05-01. Older versions will no longer be able to sync after this date.”*
- *“Firestore rules change in this release. Deploy the new rules before upgrading the app.”*

**Block sync after date (optional):** To disable sync for old app versions after a given date, set **`blockSyncAfterEpochMs`** (number, Unix ms) in Firestore `config/app` or `users/{uid}/config/app`. When current time ≥ that value, the Android app blocks sync and shows the sync-failure message ("Sync is disabled for this app version. Please update the app."). Set the field from the dashboard, a script, or a Cloud Function when you announce EOL.

See [MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md) § Software updates (min-version, deprecation).
