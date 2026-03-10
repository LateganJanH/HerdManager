# HerdManager Cloud Functions (Option B)

Callable Firebase Cloud Functions that aggregate herd stats and list devices. The web app can call these when the user is signed in, so the Next.js app can stay static/serverless (no Firebase Admin in Next.js required for stats/devices).

## Functions

- **getHerdStats** – Returns herd stats (totalAnimals, dueSoon, byStatus, bySex, byCategory, etc.) for the authenticated user. Reads `users/{uid}/animals`, `breeding_events`, `calving_events` and aggregates (same logic as web `herdStatsFromDocs`).
- **getDevices** – Returns `{ devices: [{ id, name, lastSyncAt }] }` for the authenticated user from `users/{uid}/devices`.
- **stripeWebhook** (Phase Later) – HTTP endpoint for Stripe webhooks. Verifies signature and updates Firestore `config/app` with `accessSuspended` when subscription is cancelled/unpaid or invoice payment fails. Set `STRIPE_WEBHOOK_SECRET` and `STRIPE_SECRET_KEY` in Firebase config. See [BILLING-IMPLEMENTATION.md](../docs/BILLING-IMPLEMENTATION.md) §3.1.

Callables require the client to be signed in (Firebase Auth); the callable SDK sends the ID token automatically.

## Prerequisites

- Node 18
- Firebase CLI: `npm install -g firebase-tools`
- Firebase project with Blaze plan (required for Cloud Functions)
- Same Firebase project as the web and Android apps (Auth, Firestore already configured)

## Setup and deploy

1. From the repo root, log in and select your project (if not already):
   ```bash
   firebase login
   firebase use <your-project-id>
   ```
   Or create `.firebaserc` in the repo root with your project id (see `.firebaserc.example` in the repo root: copy to `.firebaserc` and set `"default"` to your Firebase project id).

2. Install and build functions:
   ```bash
   cd functions
   npm install
   npm run build
   ```

3. Deploy:
   ```bash
   firebase deploy --only functions
   ```
   Or from repo root: `firebase deploy --only functions`.

4. Web: set `NEXT_PUBLIC_USE_STATS_VIA_CALLABLE=true` in `web/.env.local` to use these callables for stats and devices when the user is signed in (see web README).

## Local emulator (optional)

From repo root:

```bash
firebase emulators:start --only functions
```

When developing with callables locally, run the web app with `NEXT_PUBLIC_FIREBASE_USE_EMULATORS=true` and `NEXT_PUBLIC_USE_STATS_VIA_CALLABLE=true`. The web client will connect to the Functions emulator at `127.0.0.1:5001` (see `web/src/app/lib/firebase.ts`).

## Web usage

When `NEXT_PUBLIC_USE_STATS_VIA_CALLABLE` is set and the user is signed in, the web dashboard uses `httpsCallable(functions, 'getHerdStats')` and `httpsCallable(functions, 'getDevices')` instead of reading Firestore directly or calling Next.js API routes. This keeps the web app static: no server-side Firebase Admin needed for these endpoints.

## Phase Later (multi-instance)

When scaling to multiple farms (instance-per-farm), deploy the same functions to each Firebase project (one project per instance). Use the provisioning checklist in [INSTANCE-PER-FARM-STRATEGY.md](../docs/architecture/INSTANCE-PER-FARM-STRATEGY.md) §3.1 and see [NEXT-STEPS.md](../docs/NEXT-STEPS.md) §4.

**Deploy to all instances:** From the registry, for each solution that has a `firebaseProjectId` set, run:

```bash
firebase use <firebaseProjectId>
firebase deploy --only functions
```

From repo root you can list deployable solutions with:

```bash
node scripts/list-solutions.js --deployable
node scripts/list-solutions.js --ids
node scripts/list-solutions.js --project-ids   # one Firebase project ID per line for shell loops
```

Example (Bash): deploy functions to every instance:

```bash
for projectId in $(node scripts/list-solutions.js --project-ids); do
  firebase use "$projectId" && firebase deploy --only functions
done
```
