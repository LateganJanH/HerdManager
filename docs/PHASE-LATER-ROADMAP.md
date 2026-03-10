# Phase Later – Next steps roadmap

Concrete next steps for **Phase Later** work after MVP and current polish. Use with [NEXT-STEPS.md](NEXT-STEPS.md) §4 and [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md).

---

## 1. Billing (multi-instance)

| Step | Description | Effort |
|------|-------------|--------|
| **1.1 ~~Webhook handler~~** | **Done (skeleton).** Cloud Function **stripeWebhook** in `functions/` verifies Stripe signature and updates Firestore `config/app.accessSuspended` on `customer.subscription.updated`/`deleted` and `invoice.payment_failed`. Android reads global `config/app` and user `users/{uid}/config/app`. Set `STRIPE_WEBHOOK_SECRET` and `STRIPE_SECRET_KEY`; add webhook URL in Stripe. See BILLING-IMPLEMENTATION §3.1, `functions/README.md`. | — |
| **1.2 ~~GET /api/v1/billing-status~~** | **Done.** `web/src/app/api/v1/billing-status/route.ts` returns `solutionId`, `plan`, `billingStatus`, `billingCustomerId` from server env (`BILLING_PLAN`, `BILLING_STATUS`, `BILLING_CUSTOMER_ID`). Returns 501 when no billing env is set. See `web/.env.example`. | — |
| **1.3 ~~Optional: suspend access~~** | **Done.** Firestore `users/{uid}/config/app` **`accessSuspended: true`** is read by Android; app shows **SubscriptionLapsedScreen** (“Subscription lapsed. Contact support.” + Contact support button when `SUPPORT_BASE_URL` set). See BILLING-IMPLEMENTATION §3.4, `firestore.rules.example`. | — |
| **1.4 ~~Optional: list-solutions billingCustomerId~~** | **Done.** `list-solutions.js` table includes `billingCustomerId`, `plan`, and `billingStatus` columns. | — |

---

## 2. Sync (Android – optional refinement)

| Step | Description | Effort |
|------|-------------|--------|
| **2.1 ~~updatedAt on more Room entities~~** | **Done.** `updatedAt` added to **HerdEntity** and **BreedingEventEntity**; DB version 13. Repos set `updatedAt` on insert and on pregnancy-check update. Sync upload sends entity `updatedAt` (fallback to `createdAt` when 0); download merge compares remote vs local `updatedAt` and keeps newer. Backup export/restore includes `updatedAt`. | — |
| **2.2 ~~Optional: updatedAt on event-style entities~~** | **Done.** `updatedAt` added to **CalvingEventEntity**, **HealthEventEntity**, **WeightRecordEntity**, **HerdAssignmentEntity**, **PhotoEntity**; DB version 14. **Migration 13→14** in `Migrations.kt` adds the columns (default 0) so existing user data is retained. Repos set `updatedAt` on insert/update. Sync and backup handle `updatedAt`. | — |

---

## 3. Instance-per-farm operations

| Step | Description | Effort |
|------|-------------|--------|
| **3.1 ~~Deploy to all instances~~** | **Done.** `scripts/deploy-all-instances.sh` and `scripts/deploy-all-instances.ps1` run `validate-registry`, then loop over `list-solutions.js --project-ids` and run `firebase use <id>` and `firebase deploy --only functions,firestore:rules` per project. | — |
| **3.2 ~~Provisioning automation~~** | **Done.** `scripts/create-firebase-project.js <solutionId> --project-id <gcp-id> [--deploy]` adds Firebase to an existing GCP project via Firebase Management API, updates registry, and optionally runs provision-instance (--deploy). Requires GOOGLE_APPLICATION_CREDENTIALS with Firebase Admin permissions. [PROVISIONING-RUNBOOK.md](PROVISIONING-RUNBOOK.md). Manual project creation in Console or GCP still required; script automates addFirebase + registry + deploy. | — |
| **3.3 ~~Optional: Stripe in link-billing.js~~** | **Done.** When `STRIPE_SECRET_KEY` is set and the `stripe` package is installed (`npm install stripe`), `link-billing.js <solutionId>` creates a Stripe customer with `metadata: { solutionId }` and runs `update-solution.js` to set `billingCustomerId`. Otherwise it prints the same instructions as before. | — |

---

## 4. Software updates and policy

| Step | Description | Effort |
|------|-------------|--------|
| **4.1 ~~Staged rollouts~~** | **Done.** [MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md) §2.4 documents staged rollouts process: build → validate → Play Console staged rollout → optional per-instance minVersionCode → CHANGELOG. | — |
| **4.2 ~~Deprecation / EOL~~** | Documented in [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) § Deprecation and EOL (grace period, CHANGELOG examples, block-after-date). Optional: add “minimum supported version” or block-sync-after-date check in app or backend when needed. | — |

---

## 5. Shared API and integrations

| Step | Description | Effort |
|------|-------------|--------|
| **5.1 ~~/api/v1 REST endpoints~~** | **Done (read-only).** GET `/api/v1` (discovery); GET `/api/v1/billing-status`; GET `/api/v1/animals` and GET `/api/v1/breeding-events` (read-only, Bearer auth) implemented. [API-V1.md](API-V1.md). Optional: CRUD for animals/breeding-events when needed. | — |
| **5.2 ~~GraphQL (optional)~~** | **Done.** POST `/api/graphql` (GraphQL Yoga) with schema `Query { animals, breedingEvents }`. Requires `Authorization: Bearer <Firebase ID token>`; resolvers read from Firestore for the authenticated user. See [API-V1.md](API-V1.md) § GraphQL. | — |

---

## Quick reference

- **Scripts run today:** `validate-registry.js`, `list-solutions.js`, `link-billing.js <id>`, `env-for-solution.js <id>`, `create-solution.js`, `update-solution.js`, `create-firebase-project.js <id> --project-id <gcp-id> [--deploy]`, `provision-instance.js <id> [--deploy-rules] [--deploy-functions]`, `deploy-all-instances.sh` / `deploy-all-instances.ps1`. **Runbook:** [PROVISIONING-RUNBOOK.md](PROVISIONING-RUNBOOK.md). **Web API:** GET `/api/v1` (discovery), GET `/api/v1/billing-status`, GET `/api/v1/animals` and `/api/v1/breeding-events` (read-only, Bearer), POST `/api/graphql` (GraphQL: `animals`, `breedingEvents`). See [API-V1.md](API-V1.md).
- **Billing:** Registry, webhook skeleton (stripeWebhook), GET billing-status, suspend access (Android), and link-billing.js are done.
- **Sync:** Merge-by-document done; updatedAt on Herd, Breeding, Calving, Health, Weight, HerdAssignment, Photo entities (DB 14).
- **Docs:** [API-V1.md](API-V1.md), [PROVISIONING-RUNBOOK.md](PROVISIONING-RUNBOOK.md), [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md), [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md), [architecture/SYNC-DESIGN.md](architecture/SYNC-DESIGN.md), [architecture/MULTI-FARM-AND-UPDATES.md](architecture/MULTI-FARM-AND-UPDATES.md).

---

## Remaining / future (optional)

| Item | Description |
|------|-------------|
| **GCP project creation** | Create a *new* GCP project from scratch via Cloud Resource Manager API (e.g. from a parent org/folder). Today: create the project in Console, then use `create-firebase-project.js` to add Firebase. |
