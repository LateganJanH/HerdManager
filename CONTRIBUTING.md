# Contributing to HerdManager

Thanks for your interest in contributing. This document gives a short overview; platform-specific details live in each repo.

## Getting started

1. **Clone** the repository and follow [Getting Started](README.md#getting-started) for prerequisites (Node.js, JDK, etc.).
2. **Web**: From `web/` use **pnpm** (lockfile is `pnpm-lock.yaml`). Run `pnpm install` and `pnpm dev`. See [web/DEVELOPMENT.md](web/DEVELOPMENT.md) for scripts, structure, testing, and data flow.
3. **Android**: Open `android/` in Android Studio, sync Gradle, and run on an emulator or device.

## How to contribute

- **Bug reports and features**: Open an issue describing the problem or proposal.
- **Code changes**: Prefer a branch and pull request. Keep changes focused; link to any related issue.
- **Web**: Run `pnpm lint` and `pnpm test:run` before submitting (or `pnpm run ci` in `web/` to run tests and build). E2E: `pnpm test:e2e` (Playwright). Follow the patterns in [web/DEVELOPMENT.md](web/DEVELOPMENT.md) (e.g. accessibility, React Query for data).
- **Android**: Follow existing Kotlin/Compose style; ensure the project builds and key flows work. Unit tests: `.\gradlew.bat :app:testDebugUnitTest` (Windows) or `./gradlew :app:testDebugUnitTest` (Unix). Instrumented tests: `.\gradlew.bat :app:connectedDebugAndroidTest` (requires device/emulator). If you see a file-lock error on Windows, run `.\gradlew.bat --stop` then `.\gradlew.bat clean :app:testDebugUnitTest --no-daemon`.

## Code style

The repo has an [.editorconfig](.editorconfig) for consistent indentation and line endings. IDEs that support EditorConfig will pick it up automatically.

## CI (GitHub Actions)

On push or pull request to `main`/`master` that touch `web/` or `shared/`, the **Web CI** workflow runs:

- **test-and-build:** lint, unit tests, and production build (from `web/`).
- **e2e:** Playwright E2E tests (Chromium). The app runs without Firebase env so the dashboard loads with sample data; see [web/e2e/dashboard.spec.ts](web/e2e/dashboard.spec.ts).

On push or PR that touch `android/`, the **Android CI** workflow runs:

- **build-and-test:** JDK 17, Android SDK (platform 35, build-tools 35.0.0), Gradle cache, `assembleDebug`, and `testDebugUnitTest`. Instrumented tests (`connectedDebugAndroidTest`) need an emulator and are not run in CI; run them locally.

Workflow files: [.github/workflows/web-ci.yml](.github/workflows/web-ci.yml), [.github/workflows/android-ci.yml](.github/workflows/android-ci.yml). [Dependabot](.github/dependabot.yml) is enabled for monthly dependency update PRs (web npm, Android Gradle, GitHub Actions).

## Full verification (before PR)

To run the same checks as CI locally:

- **Web:**  
  `cd web && pnpm run ci` (lint + unit tests + build).  
  Optionally `pnpm test:e2e` (Playwright; install Chromium first with `pnpm exec playwright install chromium` if needed).

- **Android:**  
  `cd android && ./gradlew :app:assembleDebug :app:testDebugUnitTest` (Unix/macOS). On Windows: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest`.  
  Set `JAVA_HOME` first on Windows; see [docs/DEVELOPMENT-SETUP.md](docs/DEVELOPMENT-SETUP.md).

When preparing a versioned release, see [docs/RELEASE-CHECKLIST.md](docs/RELEASE-CHECKLIST.md) (changelog, version bump, tagging, deploy).

## Project layout

- **`web/`** — Next.js dashboard (TypeScript, Tailwind). Entry point for dashboard work.
- **`android/`** — Kotlin/Jetpack Compose field app. Entry point for mobile and offline-first features.
- **`docs/`** — Architecture, data model, MVP. Useful for understanding scope and APIs.
- **`shared/`** — API contracts and specs (no runtime code).

If you’re unsure where to put something or how to run tests, ask in an issue or in your PR description.
