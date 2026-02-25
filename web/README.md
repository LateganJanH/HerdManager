# HerdManager Web

Web dashboard for HerdManager — modern cattle herd management. Built with Next.js 15, React 19, and Tailwind CSS.

## Features

- **Home** — Overview with quick stats, status breakdown, and reproduction summary
- **Profiles** — Herd list (ear tag, status, sex); sample data or sync from Android app
- **Alerts** — Calving, pregnancy check, and withdrawal-period due dates (filter and CSV export)
- **Analytics** — Bar chart and summary stats
- **Settings** — Farm/sync (farm name, address, **multiple contacts** (name, phone, email), alert days), linked devices list (multiple phones/tablets), sample data toggle, appearance, about
- **Dark mode** — Toggle from the menu or set Light / Dark / System in **Settings → Appearance**; preference stored in `localStorage`
- **Sample data** — Optional demo data when sync is not configured (toggle in Settings)
- **Data source & refresh** — Home, Profiles, Alerts, and Analytics each show data source (From API / Sample data) and **Last updated** when connected; **Refresh** refetches stats. If the stats API fails, each tab shows an alert: *"Couldn't load latest stats. Showing cached or sample data."* with a **Retry** button.
- **Keyboard shortcuts** — 1–5 switch tabs; Esc closes menu; menu **Copy link** for current tab URL; full list in Settings
- **Print** — Header and bottom nav are hidden when printing; only main content is included

## Prerequisites

- Node.js 18+ (20 recommended; see `.nvmrc`)
- **pnpm** (recommended; repo has `pnpm-lock.yaml`) or npm

## Setup

```bash
cd web
pnpm install
```

If you use npm instead, run `npm install`. The project’s `.npmrc` enables `legacy-peer-deps` so install completes despite peer dependency conflicts (e.g. with ESLint/knip).

Copy `.env.example` to `.env.local`. To enable **sign-in and live herd data** from the Android app, add your Firebase project keys (`NEXT_PUBLIC_FIREBASE_API_KEY`, `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`, `NEXT_PUBLIC_FIREBASE_PROJECT_ID`, etc.). Use the same Firebase project as the Android app so one account works on both. Without these, the dashboard runs with sample data and no auth gate. For a step-by-step (Firestore rules, Auth, web env), see [FIREBASE-SYNC-WALKTHROUGH.md](../docs/FIREBASE-SYNC-WALKTHROUGH.md).

## Development

```bash
pnpm dev
# or: npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The app uses sample data by default; turn it off in **Settings → Development → Use sample data**.  
API: `GET /api/health`, `GET /api/stats` (herd stats), `GET /api/devices` (linked field devices), `GET /api/spec` (OpenAPI 3 spec, YAML). Settings → About includes a link to the API spec.

## Build

```bash
pnpm build
pnpm start
```

## Deployment

For production, set `NEXT_PUBLIC_BASE_URL` (and optionally `NEXT_PUBLIC_APP_VERSION`) in your environment so the sitemap, robots.txt, and health API use the correct origin. The app can be deployed to Vercel, any Node host, or static export (if you configure Next.js for static output). Health check: `GET /api/health` returns `{ ok, service, version }`. SEO: `/sitemap.xml` and `/robots.txt` are generated from the same base URL.

## Test

```bash
pnpm test           # unit tests (watch mode)
pnpm test:run      # unit tests, single run
pnpm test:e2e      # E2E (Playwright): starts dev server, runs dashboard tests
pnpm test:e2e:ui   # E2E with Playwright UI
pnpm run ci        # test + build (for CI)
```

E2E tests cover: open Home (dashboard or sign-in), skip link, Home Refresh and overview, switch tabs, keyboard shortcut 2 → Profiles, open Settings, Settings About and API spec link (/api/spec returns OpenAPI YAML), toggle theme and sample data, Alerts Withdrawal filter, Edit farm form (when visible) including Contacts. Run without `NEXT_PUBLIC_FIREBASE_*` set so the dashboard loads with sample data (no sign-in). For CI with browsers: `pnpm ci:e2e` (installs Chromium and runs E2E).

See [DEVELOPMENT.md](DEVELOPMENT.md) for a contributor-focused guide (scripts, structure, testing, data).

## Project structure

- `src/app/` — Next.js App Router (layout, page, globals)
- `src/app/components/` — Shell, dashboard tabs, Settings
- `src/app/lib/` — Data and hooks (mock herd data, stats, linked devices; Firestore when Firebase is configured)

## Design

UI follows the award-inspired agriculture layout: bottom navigation (Home, Profiles, Alerts, Analytics), hamburger menu for Settings, card-based dashboard, high-contrast typography, and dark mode for field use. The app respects **prefers-reduced-motion** and provides keyboard focus styles and a skip link for accessibility.
