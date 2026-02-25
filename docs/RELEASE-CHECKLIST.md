# Pre-release checklist

Use this before cutting a versioned release (e.g. v1.0.0) or shipping to testers.

## Before release

- [ ] **CI green** – Web: `cd web && pnpm run ci && pnpm test:e2e`. Android: `cd android && ./gradlew :app:assembleDebug :app:testDebugUnitTest`. See [CONTRIBUTING.md](../CONTRIBUTING.md) § Full verification.
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
