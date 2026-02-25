# HerdManager

**Modern Digital Cattle Herd Management System**

A science-driven, multi-platform cattle management system for farmers, breeders, veterinarians, and agribusiness operators. Enables individual animal intelligence with AI-powered identification, comprehensive datasheets, heritage tracking, gestation/calving alerts, and analytics.

## Platforms

| Platform | Purpose | Status |
|----------|---------|--------|
| **Android** | Primary field app (camera, GPS, voice, offline-first) | In Development |
| **Web** | Desktop dashboards, analytics, reporting | In Development |

MVP scope (auth, cloud sync, alerts, web–Firestore) is done. See [Next steps](docs/NEXT-STEPS.md) for current roadmap.

## Core Principles (from PRD)

- **Individual-Animal First** – Every cow and bull is a first-class entity
- **Photo-Centric Identification** – Visual recognition complements ear tags and brands
- **Offline-First** – Fully functional without signal, auto-sync when online
- **Scientific Accuracy** – Veterinary-aligned dates, cycles, genetics, metrics
- **Explainable AI** – Transparent insights, not black-box guesses

## Project Structure

```
HerdManager/
├── android/                 # Android app (Kotlin, Jetpack Compose)
├── web/                     # Web dashboard (Next.js, TypeScript)
├── shared/                  # Shared specs, API contracts, data schemas
├── docs/                    # Architecture, data model, guides
└── PRD/                     # Product Requirements Document
```

## Getting Started

### Prerequisites

- **Android**: JDK 17+, Android Studio (Koala 2024.1+), Android SDK 34+
- **Web**: Node.js 20+, pnpm/npm
- **Shared**: No runtime (specs only)

### Development Setup

1. **Clone and install**
   ```bash
   cd HerdManager
   cd web && pnpm install   # Web dependencies
   ```

2. **Android** — See [android/README.md](android/README.md) for build and run. Open `android/` in Android Studio or run `.\gradlew.bat :app:assembleDebug` (Windows; set `JAVA_HOME` first per [docs/DEVELOPMENT-SETUP.md](docs/DEVELOPMENT-SETUP.md)).

3. **Web**
   ```bash
   cd web
   pnpm dev
   ```
   Open [http://localhost:3000](http://localhost:3000). See [web/README.md](web/README.md) for features and build commands.  
   Health check: `GET /api/health`.

4. **Verify (optional)** — From repo root: `cd web && pnpm run ci` runs web tests and build. With browsers: `cd web && pnpm test:e2e` runs E2E. For Android: `cd android && ./gradlew :app:assembleDebug :app:testDebugUnitTest`. See [CONTRIBUTING.md](CONTRIBUTING.md) § Full verification and § CI.

See [docs/DEVELOPMENT-SETUP.md](docs/DEVELOPMENT-SETUP.md) for detailed setup instructions.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for how to report issues, submit changes, and run tests. Web contributors should also read [web/DEVELOPMENT.md](web/DEVELOPMENT.md).

## Documentation

- [Next steps](docs/NEXT-STEPS.md) – prioritized roadmap after MVP (web–Firestore, polish, tests)
- [Pre-release checklist](docs/RELEASE-CHECKLIST.md) – versioning, changelog, tagging, and deploy
- [Firestore sync walkthrough](docs/FIREBASE-SYNC-WALKTHROUGH.md) – connect Android and web to one Firebase project
- [GitHub sync](docs/GITHUB-SYNC.md) – connect this repo to GitHub and push/pull
- [Security](SECURITY.md) – reporting vulnerabilities and secure development
- [Architecture Overview](docs/architecture/ARCHITECTURE.md)
- [Data Model](docs/architecture/DATA-MODEL.md)
- [MVP Definition](docs/architecture/MVP-DEFINITION.md)
- [API Specification](shared/api/)

## License

Proprietary – HerdManager
