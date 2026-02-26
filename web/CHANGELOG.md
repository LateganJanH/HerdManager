# Changelog

All notable changes to the HerdManager web dashboard will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed

- Web build/runtime: removed circular dependency between `mockHerdData` and `sampleStatsData` by introducing `herdStatsTypes.ts` (shared `HerdStats` type); avoids "Cannot read properties of undefined (reading 'call')" at runtime. If build fails with ENOENT on `.nft.json`, delete `.next` and run `pnpm run build` again.

### Added

- Phase 2 pedigree: Sire and dam on animal. **Android:** Animal detail shows Sire/Dam; Edit animal has optional Pedigree section (sire/dam dropdowns); new calves get damId; Animal/AnimalEntity/sync/backup include sireId/damId; Room DB version 12. **Web:** Profiles animal detail shows Sire/Dam when from Firestore (sireEarTag/damEarTag resolved from animals collection). **Shared:** OpenAPI and animal.schema.json include optional sireId, damId.
- Pre-release checklist: [docs/RELEASE-CHECKLIST.md](../docs/RELEASE-CHECKLIST.md) – CI, changelog, version bump, tagging, web/Android deploy; linked from README and CONTRIBUTING
- SECURITY.md: vulnerability reporting and secure development (secrets, dependencies); linked from README
- .gitignore: Playwright artifacts (web/test-results/, playwright-report/, blob-report/, playwright/.cache/); `_tmp_*`; `Videos/` (new video files not committed); DEVELOPMENT-SETUP note on large files and shallow clone
- API spec: GET /api/spec serves OpenAPI 3 YAML (from shared/api/openapi.yaml); link in Settings → About ("API spec (OpenAPI 3)"); prebuild copies spec to web/public for production; unit test for /api/spec
- Accessibility: skip link focuses main content when navigating to #main-content (hashchange listener in AppShellContent); E2E test for skip link
- E2E: first test passes with or without Firebase (dashboard or sign-in); keyboard shortcut 2 → Profiles tab test; Settings About section test (API spec link + GET /api/spec returns OpenAPI)
- CI: GitHub Actions web-ci.yml (lint, unit tests, build, Playwright E2E on web/ or shared/ changes); android-ci.yml (assembleDebug, testDebugUnitTest on android/ changes); Dependabot for npm, Gradle, and GitHub Actions (monthly)
- CONTRIBUTING: Full verification section (commands to run web and Android checks locally); CI section documents both workflows and Dependabot
- Docs: DESIGN-UI-AND-GAPS updated for accessibility (skip link, keyboard tabs) and API spec; NEXT-STEPS and README reference CI and verification
- Analytics: Export report (PDF) button alongside CSV; downloads herd summary, by status, by sex, and analytics series as PDF (jspdf)
- Home: “View full Analytics →” link in Analytics preview when herd data exists (navigates to Analytics tab)
- Settings Farm & sync: Sync status (Connected / Not configured) and Last synced from live stats (useHerdStats); shows relative time when API is connected
- formatRelativeTime: extended for "X hours ago", "yesterday", "X days ago", and short date for 7+ days; new unit tests
- Multiple linked field devices: Settings → Farm & sync shows a list of linked phones/tablets (name, last synced). Dashboard supports multiple Android devices syncing to one web portal; GET /api/devices and useLinkedDevices hook; mock devices when sample data is on
- README Features: Print bullet (header and nav hidden, main content only)
- Settings: Keyboard shortcuts list includes Print (header and nav hidden, main content only)
- Manifest test: manifest.test.ts asserts PWA manifest fields (name, short_name, start_url, display, icons, theme_color)
- Robots test: robots.test.ts asserts rules (userAgent, allow) and sitemap URL shape
- Health API test: assert Cache-Control no-store/max-age=0 for fresh health checks
- Sitemap test: sitemap.test.ts asserts non-empty entries with url and lastModified; base URL shape
- README: data source & refresh bullet for all tabs; Development uses pnpm dev with npm alternative
- Alerts: data source line and "Last updated" when from API; Refresh button (consistent with Home, Profiles, Analytics)
- **Weaning weight due on web:** Alerts tab shows weaning-weight-due items (calves with weaning date in next 14 days or up to 30 days overdue, no weight in window); filter “Weaning weight”; Home due-soon preview and label; Settings → Edit farm: Weaning age (days) 150–300; Firestore fetch uses farm settings + animals (DOB) + weight_records; real-time listeners for weight_records and settings/farm
- Profiles: use useHerdStats for herd count and data source; "Data: X · Last updated Y" and Refresh button (consistent with Home and Analytics)
- Analytics: "Last updated X" when data is from API; Refresh button to refetch stats (consistent with Home)
- GET /api/devices test: assert response devices pass filterValidDevices (contract for backend)
- DEVELOPMENT.md: document devices API response shape (id, name, lastSyncAt) and filterValidDevices for backend implementers
- README Features: mention Copy link in keyboard shortcuts
- linkedDevices: isValidLinkedDevice and filterValidDevices for API response validation; use in useLinkedDevices so malformed device entries are filtered out; unit tests
- not-found: document title set to "Page not found | HerdManager" for consistency with error page
- Home hero: comment updated (gradient/pattern; optional farm image when available)
- Settings linked devices: Loading state, error message with Try again, and Refresh button to refetch device list
- README: linked devices in Settings features; GET /api/devices in API line; lib description updated
- Theme tests: stub global localStorage so getTheme/setTheme use mock; all tests pass
- Unit tests for linkedDevices (getMockLinkedDevices shape, ids, lastSyncAt, platform)
- herdStatsValidation: byStatus/bySex must be records of numbers (reject string values and arrays); new tests
- APP_NAME constant (lib/version.ts) used in layout, error, not-found, loading, page, AppShell, manifest, Settings, Home (Connect CTA), Profiles (empty state)
- not-found: focus-visible ring offset for light/dark so focus ring is visible on both themes
- README Features: keyboard shortcuts (1–5, Esc) bullet; CONTRIBUTING: pnpm and lockfile note for web
- Unit tests for sampleStatsData (validation + expected values); GET /api/stats test asserts response equals SAMPLE_STATS_DATA
- Shared sample stats: `lib/sampleStatsData.ts` used by GET /api/stats and mock data (single source of truth)
- DEVELOPMENT.md: API routes (health, stats) and local dev URLs; lib list includes sampleStatsData
- Alerts: "Show all" button when filter (Calving/Pregnancy check) yields no results
- README Deployment: sitemap.xml and robots.txt noted for SEO
- formatRelativeTime lib (extracted from Home "last updated"); unit tests
- Home Connect CTA: "enable sample data in Settings" link when no data
- Profiles: "Clear search" button when search has no matches
- robots.txt via app/robots.ts (allow all, sitemap URL from NEXT_PUBLIC_BASE_URL)
- CONTRIBUTING: web verify step (pnpm run ci) noted; .env.example top comment (copy to .env.local, optional for local dev)
- Analytics and Profiles empty states: "View dashboard" link to Home tab
- DEVELOPMENT.md: lib (theme, analyticsSeries), testing list, focus-on-tab note in Accessibility
- Unit tests for theme.ts: getTheme (default/system, valid stored values), setTheme (localStorage write)
- Focus moves to main content when switching tabs (keyboard 1–5 or nav click) for screen reader users
- Alerts empty state: "View dashboard" link to Home tab when there are no alerts
- Home: "View Alerts →" link on Due soon card when dueSoon > 0 (navigates to Alerts tab)
- Profiles: "Showing X of Y animals" when search filter is active
- README: theme selector (Light/Dark/System in Settings → Appearance) noted in Features
- Profiles: sort by Ear tag (A–Z), Status, or Sex via dropdown
- analyticsSeries lib: `seriesFromStats()` and `STATUS_COLORS` extracted from Analytics; unit tests in analyticsSeries.test.ts
- Analytics tab uses live API data: `useHerdStats()` for stats and export; status/sex breakdown derived from API when connected
- Analytics: loading state, data source label (From API / Sample data / Connect app), empty state when no breakdown data
- Theme control in Settings: Light / Dark / System (follow device); persisted and applied before first paint to avoid flash
- Home Analytics preview: progress bar shows “data available” (100%) when herd stats exist, 0% otherwise; progressbar accessibility attributes
- URL tab deep-linking: `?tab=home|profiles|alerts|analytics|settings`; URL updates when switching tabs for sharing/bookmarks
- Refresh button on Home to re-fetch stats from API; loading state while fetching
- CSV export on Analytics: “Export report (CSV)” downloads herd summary, by-status, by-sex, and analytics series
- Keyboard shortcuts: 1–5 switch tabs (Home, Profiles, Alerts, Analytics, Settings); listed in Settings
- Herd stats via React Query: caching, 30s stale time, 2 retries with backoff
- Home: error banner when stats fetch fails (with Retry); skeleton loaders for stat cards while loading
- Profiles: mock animal list (ear tag, status, sex) in a card grid when sample data is on
- Alerts: list sorted by days until (soonest first); filter by type (All / Calving / Pregnancy check)
- Profiles: search input to filter by ear tag, status, or sex
- Alerts: “Export alerts (CSV)” downloads current (filtered) list
- Print-friendly: header and bottom nav hidden when printing; main content only
- Profiles: “Export (CSV)” downloads current (filtered) animal list (ear tag, status, sex)
- Home: “Last updated” (e.g. “2 min ago”) when stats are from API
- herdStatsValidation: extracted isValidStats for API response validation; unit tests in herdStatsValidation.test.ts
- API route tests: GET /api/stats (200 + valid herd stats), GET /api/health (200 + ok, service, version)
- Health API: response includes `version` (from NEXT_PUBLIC_APP_VERSION) for monitoring
- not-found: metadata with title and robots noindex/nofollow so 404s aren’t indexed
- error page: document title set to “Something went wrong | HerdManager”
- README: Deployment section (env vars, health check, hosting options)
- Menu: “Copy link” copies current URL (with ?tab=) to clipboard; listed in Settings → Keyboard shortcuts
- not-found and error pages: HerdManager branding and focus-visible styles
- CI script: `pnpm run ci` (test + build) for pipelines
- Layout: metadataBase and openGraph.url from NEXT_PUBLIC_BASE_URL for share URLs
- noscript: message and link when JavaScript is disabled
- API: Cache-Control no-store for /api/health; s-maxage=30, stale-while-revalidate=60 for /api/stats
- Scroll-to-top button when main content is scrolled down (fixed bottom-right; print hidden)
- Twitter Card metadata (summary, title, description) for link previews
- Permissions-Policy header (camera, microphone, geolocation disabled)
- Unit tests for mock data shapes (mockHerdData.test.ts)
- android/README.md: build, run, JAVA_HOME, project structure
- Root `.editorconfig`: charset, indent (2/4), trim trailing whitespace, Kotlin/Gradle 4 spaces
- PWA manifest: scope, orientation, categories (business, productivity)
- DEVELOPMENT.md: contributor guide (setup, scripts, structure, testing, accessibility)
- Dashboard: Home, Profiles, Alerts, Analytics tabs with bottom navigation
- Settings: Farm/sync placeholder, sample data toggle, appearance, keyboard shortcuts, about & version
- Dark mode with persisted preference
- Sample herd data for development (toggle in Settings)
- Accessibility: skip link, focus-visible styles, reduced-motion support, Escape to close menu
- API: `GET /api/health` health check
- 404 and error boundary pages
- Dynamic document title per tab
- Favicon, Open Graph metadata, theme color, viewport
- Sitemap at `/sitemap.xml`
- Web app manifest for Add to home screen
- Security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy)
- Unit tests (Vitest) and version tests

## [0.1.0] - Initial release

- Next.js 15, React 19, Tailwind CSS
- Card-based layout and agriculture-inspired design
