# Development guide

Short guide for contributors to the HerdManager web dashboard.

## Prerequisites

- **Node.js** 18+ (20 recommended; see [.nvmrc](.nvmrc))
- **pnpm** (recommended; lockfile is `pnpm-lock.yaml`) or npm

## Setup

```bash
cd web
pnpm install
```

Copy [.env.example](.env.example) to `.env.local` and set `NEXT_PUBLIC_APP_VERSION`, `NEXT_PUBLIC_BASE_URL`, and Firebase keys when needed.

## Scripts

| Command           | Description                    |
|-------------------|--------------------------------|
| `pnpm dev`        | Start dev server (Next.js)     |
| `pnpm dev:turbo`  | Dev server with Turbopack      |
| `pnpm build`      | Production build               |
| `pnpm start`      | Run production server          |
| `pnpm lint`       | Run ESLint (Next.js config)    |
| `pnpm test`       | Vitest in watch mode           |
| `pnpm test:run`   | Vitest single run              |
| `pnpm run ci`     | Tests + build (for CI pipelines) |

## Project structure

- **`src/app/`** — App Router: `layout.tsx`, `page.tsx`, `globals.css`, `providers.tsx`
- **`src/app/components/`** — Shell (`AppShell*`), dashboard views (`Dashboard*`), shared UI
- **`src/app/lib/`** — Data and hooks: `mockHerdData.ts`, `sampleStatsData.ts` (shared sample stats for API and mock), `useHerdStats.ts`, `useLinkedDevices.ts`, `linkedDevices.ts` (types and mock devices), `version.ts`, `theme.ts` (Light/Dark/System), `analyticsSeries.ts` (chart series from stats), `herdStatsValidation.ts`, `formatRelativeTime.ts` (e.g. "2 min ago")
- **`src/app/api/`** — Route handlers: `health`, `stats`, `devices`
- **`public/`** — Static assets (favicon, etc.)

## Data and API

- **Mock data** — `src/app/lib/mockHerdData.ts` provides sample herd stats, alerts, analytics, and animal profiles. Toggle “Use sample data” in Settings to enable/disable.
- **Stats API** — `GET /api/stats` returns herd stats JSON. The dashboard uses React Query (`useHerdStats`) and falls back to mock data when the API is unavailable or returns invalid data.
- **Devices API** — `GET /api/devices` returns linked field devices (id, name, lastSyncAt). Multiple mobile devices can be linked to one dashboard; the route returns an empty list until a real backend is connected. When sample data is enabled, the UI shows mock devices for demo. The web app validates each device with `filterValidDevices` (see `linkedDevices.ts`); backend must return objects with `id` (string), `name` (string), and `lastSyncAt` (number).
- **Local dev** — With `pnpm dev`, `GET /api/health`, `GET /api/stats`, and `GET /api/devices` are available at `http://localhost:3000/api/...`.
- When the backend is connected, replace mock usage with API calls; keep the same shapes (e.g. `HerdStats`, `AlertItem`) for minimal UI changes.

## Testing

- **Vitest** — Unit tests live next to code or in `*.test.ts` (e.g. `version.test.ts`, `herdStatsValidation.test.ts`, `api/stats/route.test.ts`).
- Run `pnpm test:run` before pushing to confirm tests pass.
- API route tests: `api/stats/route.test.ts` (herd stats shape), `api/health/route.test.ts` (ok, service, Cache-Control), `api/devices/route.test.ts` (devices array). App: `sitemap.test.ts`, `robots.test.ts`, `manifest.test.ts` (PWA manifest shape).
- Lib tests: `lib/mockHerdData.test.ts`, `lib/herdStatsValidation.test.ts`, `lib/analyticsSeries.test.ts`, `lib/theme.test.ts`, `lib/formatRelativeTime.test.ts`, `lib/sampleStatsData.test.ts`, `lib/linkedDevices.test.ts`.

## Code style

- **ESLint** — `pnpm lint` uses `eslint-config-next`. Fix auto-fixable issues with `pnpm lint --fix` if configured.
- **TypeScript** — Strict mode; types are in `mockHerdData.ts` and component props.

## Accessibility

- Skip link, focus-visible outlines, reduced-motion support, and keyboard shortcuts (1–5 for tabs, Esc to close menu) are built in. When you switch tabs (keyboard or click), focus moves to the main content area for screen reader users. Preserve these when changing the shell or navigation.
