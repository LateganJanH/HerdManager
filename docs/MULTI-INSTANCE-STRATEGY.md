# Multi-instance strategy — documentation index and overview

This document is the **entry point** for all planning and documentation related to selling and operating HerdManager as **one dedicated instance per farm** (multi-instance). It summarises the strategy and points to the relevant docs.

**Audience:** Solution architects, product, ops, and implementers. **Status:** Planning base; **Phase 1 implementation** (solution ID, registry, support links) is in place — see §5 below.

---

## 1. Intent and purpose

| Aspect | Summary |
|--------|---------|
| **Model** | One **HerdManager Solution** = one sold instance = one farm (customer). Each instance gets its own Firebase project, web dashboard URL, and app config (or build). No shared multi-tenant database. |
| **Intent** | Sell HerdManager to many farming businesses; each customer gets a dedicated deployment, data isolation, and optional white-label. |
| **Purpose** | Trust and compliance (“your data in your instance”), per-farm pricing, clear support/ops boundaries, and a predictable income stream from recurring subscriptions. |

**Terminology:** **HerdManager Solution** = one instance = Firebase project + web dashboard + field app(s) + (future) IoT. See [architecture/AUTH-AND-IDENTITY.md](architecture/AUTH-AND-IDENTITY.md) for the solution boundary and future identity model.

---

## 2. What the strategy covers

The full strategy is defined in **[architecture/INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md)**. It includes:

| Topic | Description | Section in strategy doc |
|-------|-------------|-------------------------|
| **Product model** | Unit of sale (one instance = one farm), isolation, identity, optional branding. | §1 |
| **What one instance is** | Firebase project, web dashboard, Android app, config; single codebase, instance-specific values at build or runtime. | §2 |
| **Unique instance/solution ID** | Every provisioned solution has a stable, unique identifier (`solutionId` / `instanceId`) assigned at creation; used in registry, billing, support, and config. | §2.1 |
| **Provisioning** | Checklist for onboarding a new farm (create project, assign solution ID, register apps, deploy web, ship app, handoff); automation and solution registry. | §3 |
| **Branding (white-label)** | Optional per-instance app name, logo, colours, web domain. | §4 |
| **Software updates** | One codebase; deploy same version to all instances (or by cohort); min-version and In-App Update for Android; per-instance rollback. | §5 |
| **Operations and support** | Backups, monitoring, security, incidents per instance; support keyed by solution ID. | §6 |
| **Support, feedback, and improvement** | Instance owners/users: suggest improvements, report issues, access support (all tagged with solution ID); support portal instance-aware. **Future:** AI support agent as first-line support desk. | §6.1 |
| **Billing and procurement** | Recurring subscription per instance (keyed by solution ID); payment provider (e.g. Stripe); sign-up → provision → go-live → first invoice; automated renewals and dunning; optional setup fee and tiers. Effortless income stream. | §7 |
| **Relation to other docs** | Sync, multi-tenant option A, software-update details. | §8 |
| **Summary table** | One-page summary of all decisions. | §9 |

---

## 3. Documentation index (all multi-instance–related docs)

| Document | Path | What it covers |
|----------|------|----------------|
| **Instance-per-farm strategy (primary)** | [architecture/INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md) | Full product strategy: product model, unique solution ID, provisioning checklist, branding, updates, ops, support/feedback (incl. future AI agent), billing and procurement. |
| **Auth and identity** | [architecture/AUTH-AND-IDENTITY.md](architecture/AUTH-AND-IDENTITY.md) | Definition of “HerdManager Solution”; current Firebase end-user login vs. target (solution service account + separate user identity and RBAC). |
| **Software updates and min-version** | [architecture/MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md) | Staged rollouts, In-App Update (Android), min-version checks; applies per instance or across all instances. |
| **High-level architecture** | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) | HerdManager Solution (instance per farm) in the overall architecture; link to INSTANCE-PER-FARM-STRATEGY. |
| **Next steps (roadmap)** | [NEXT-STEPS.md](NEXT-STEPS.md) | §4 “Later”: instance-per-farm strategy with pointers to solution ID (§2.1), billing (§7), support/feedback (§6.1), and AI support agent; implementation checklist. |
| **Release checklist** | [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) | Optional “Multi-instance (Phase Later)” bullet: deploy functions and rules to each Firebase project; use provisioning checklist when onboarding a new farm. |
| **Cloud Functions** | [../functions/README.md](../functions/README.md) | Phase Later: when scaling to multiple farms, deploy the same functions to each Firebase project; link to INSTANCE-PER-FARM-STRATEGY and NEXT-STEPS. |
| **Billing implementation** | [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md) | Implementation guide (Phase Later): link solutionId to payment provider (e.g. Stripe), optional registry fields, webhooks, renewals/dunning, optional suspend access. Strategy: INSTANCE-PER-FARM-STRATEGY §7. |
| **Sync design** | [architecture/SYNC-DESIGN.md](architecture/SYNC-DESIGN.md) | Data and sync **within** one instance (multi-device merge); no change to data model for “one farm per instance.” |

---

## 4. Quick reference: solution ID, billing, support

- **Solution ID:** Assign a unique, stable `solutionId` (or `instanceId`) at provisioning; store in solution registry; use in billing, support, and (optionally) app config so feedback and support requests are tagged.
- **Billing:** Recurring subscription per instance; payment provider (e.g. Stripe); one billing customer per solution ID; automate renewals and dunning. **Implementation guide:** [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md).
- **Support and feedback:** In-app/web: “Suggest a feature,” “Report a problem,” “Help & support” — all keyed by solution ID; support portal can be instance-aware. Plan for an AI support agent as first-line support desk later.

For full detail, use [architecture/INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md).

---

## 5. Phase 1 implementation (current)

The following is implemented in the repo and ready to use when provisioning or configuring an instance.

### 5.1 Solution registry and create script

| Item | Location | Description |
|------|----------|-------------|
| **Registry example** | `scripts/solution-registry.example.json` | Schema and one example solution entry. Optional billing fields: `billingCustomerId`, `plan`, `billingStatus` (see [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md)). Copy to `solution-registry.json` (or create from script); `solution-registry.json` is in `.gitignore` so real project IDs are not committed. |
| **Create script** | `scripts/create-solution.js` | Run `node scripts/create-solution.js [--name "Farm Name"]` to generate a new `solutionId`, append an entry to `scripts/solution-registry.json`, and print next steps (create Firebase project, set env, deploy web, etc.). Use `--print-solution-id` to output only the ID for scripting. |
| **GCP project creation** | `scripts/create-gcp-project.js` | Run `node scripts/create-gcp-project.js <solutionId> --project-id <id> --name "Farm" (--org-id N | --folder-id N) [--billing-account ...] [--deploy]` to create a new GCP project via Cloud Resource Manager API, optionally link billing, and with `--deploy` run create-firebase-project and provision-instance. Requires `GOOGLE_APPLICATION_CREDENTIALS`. See [PROVISIONING-RUNBOOK.md](PROVISIONING-RUNBOOK.md) Option C. |
| **Update script** | `scripts/update-solution.js` | Run `node scripts/update-solution.js <solutionId> [--firebase-project-id <id>] [--web-url <url>] [--billing-customer-id <id>] [--plan <plan>] [--billing-status <status>]` to set `firebaseProjectId`, `webUrl`, and/or optional billing fields on an existing registry entry. |
| **Env-for-solution helper** | `scripts/env-for-solution.js` | Run `node scripts/env-for-solution.js <solutionId> [--support-url <url>]` to print ready-to-paste web env lines (NEXT_PUBLIC_SOLUTION_ID, NEXT_PUBLIC_SUPPORT_URL) and the Android Gradle command for that solution. Optionally validates `solutionId` against the registry. |
| **List solutions** | `scripts/list-solutions.js` | Run `node scripts/list-solutions.js` for a table; `--ids` for one solutionId per line; `--project-ids` for one firebaseProjectId per line (for deploy loops); `--json` for JSON array; `--deployable` to list only entries with a non-empty `firebaseProjectId`. |
| **Validate registry** | `scripts/validate-registry.js` | Run `node scripts/validate-registry.js` to check for duplicate `solutionId`, required fields (`solutionId`, `farmName`). Exit 0 if valid, 1 otherwise. Use in CI or before deploy. |
| **Link billing (Phase Later)** | `scripts/link-billing.js` | Run `node scripts/link-billing.js <solutionId>` to print steps (or, if `STRIPE_SECRET_KEY` is set and `stripe` is installed, create the Stripe customer and update the registry in one step). Run with `--customer-id cus_xxx` to only update the registry. See [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md). |
| **Deploy to all instances** | `scripts/deploy-all-instances.sh`, `scripts/deploy-all-instances.ps1` | Run from repo root to validate the registry and deploy Cloud Functions and Firestore rules to every solution with a non-empty `firebaseProjectId`. See [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) § Multi-instance. |
| **Full onboarding** | `scripts/onboard-farm.sh`, `scripts/onboard-farm.ps1` | One-command onboarding: create solution, then create GCP project (and with `--deploy` / `-Deploy` add Firebase and provision rules/functions). See [PROVISIONING-RUNBOOK.md](PROVISIONING-RUNBOOK.md) § Full automated onboarding. |

### 5.2 Web dashboard

| Item | Description |
|------|-------------|
| **Env vars** | `NEXT_PUBLIC_SOLUTION_ID` — unique instance ID for this deploy. `NEXT_PUBLIC_SUPPORT_URL` — base URL for support portal (e.g. `https://support.yourproduct.com`). See `web/.env.example`. |
| **Support links** | When `NEXT_PUBLIC_SUPPORT_URL` is set, Settings → About shows **Help & support**, **Suggest a feature**, and **Report a problem** links. Each URL includes `solutionId` (and optional `topic=suggest` / `topic=report`) so the support portal can scope requests to the instance. When `NEXT_PUBLIC_SOLUTION_ID` is set, About also shows **Instance:** <var>solutionId</var> for operator confirmation. |
| **Instance-config API** | `GET /api/instance-config` returns `{ solutionId, supportBaseUrl }` from env (no auth). Use from support tooling or the dashboard to resolve the current instance config. See `shared/api/openapi.yaml` description. |
| **Support page** | `GET /support?solutionId=...&topic=suggest|report` — minimal support landing page that shows instance ID and topic-specific copy (suggest a feature / report a problem). Deploy the same web app to a support subdomain (e.g. `support.yourproduct.com`) and set `NEXT_PUBLIC_SUPPORT_URL` to that origin so in-app links open this page with context. |

### 5.3 Android app

| Item | Description |
|------|-------------|
| **BuildConfig** | `BuildConfig.SOLUTION_ID` and `BuildConfig.SUPPORT_BASE_URL` (default empty). Set per build via Gradle properties: `./gradlew -PsolutionId=acme-farm-01 -PsupportBaseUrl=https://support.example.com :app:assembleDebug`. For product flavours, override these in the flavour's `buildConfigField`. |
| **Support links** | When `SUPPORT_BASE_URL` is not blank, Settings → About shows **Help & support**, **Suggest a feature**, and **Report a problem** buttons that open the support URL with `solutionId` and `topic` query params. When `SOLUTION_ID` is set, About also shows **Instance:** <var>solutionId</var> for operator confirmation (parity with web). |

### 5.4 Provisioning flow (using Phase 1)

1. Run `node scripts/create-solution.js --name "Acme Farm"` to get `solutionId` and next steps.
2. **Create Firebase project** — choose one:
   - **Manual:** Create project in [Firebase Console](https://console.firebase.google.com/); enable Auth, Firestore, Storage.
   - **Add Firebase to existing GCP project:** `node scripts/create-firebase-project.js <solutionId> --project-id <gcp-id> [--deploy]` (see [PROVISIONING-RUNBOOK.md](PROVISIONING-RUNBOOK.md) Option B).
   - **Create GCP project then add Firebase (automated):** `node scripts/create-gcp-project.js <solutionId> --project-id <id> --name "Farm" (--org-id N | --folder-id N) [--deploy]` (runbook Option C). With `--deploy`, this also runs create-firebase-project and provision-instance.
3. Register Android and Web apps in Firebase; get config.
4. **Web:** Set `NEXT_PUBLIC_SOLUTION_ID` and `NEXT_PUBLIC_SUPPORT_URL` in `.env.production` (or build env); deploy.
5. **Android:** Build with `-PsolutionId=<id> -PsupportBaseUrl=<url>` (or set in product flavour); ship APK/AAB.
6. Update the registry entry with Firebase project and web URL: `node scripts/update-solution.js <solutionId> --firebase-project-id <id> --web-url <url>`.

For **full automated onboarding** (solution + GCP project + Firebase + deploy in one flow), use the runbook § “Full automated onboarding” or the `onboard-farm.sh` / `onboard-farm.ps1` scripts.

Billing integration, full IaC, and support portal backend are **not** implemented yet; they are Phase Later per the strategy doc. When you add billing, use [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md) to link solutionId to a payment provider (e.g. Stripe) and optionally store billing fields in the registry.
