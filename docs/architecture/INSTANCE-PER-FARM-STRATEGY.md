# Product strategy: instance per farm (Option B)

**Audience:** Solution architect / vendor. **Goal:** Sell HerdManager to farming businesses as a **dedicated instance per farm** — each customer (farm) gets their own deployment, data, and optional branding. No shared multi-tenant database.

**Terminology:** **HerdManager Solution** = one sold instance = Firebase project + web dashboard (increasingly feature-rich) + field client(s) (today Android; future other platforms) + (future) IoT devices for precision farming. See [AUTH-AND-IDENTITY.md](AUTH-AND-IDENTITY.md) for definition and for the target auth model (solution service account + separate user identity and RBAC).

---

## 1. Product model

| Aspect | Choice |
|--------|--------|
| **Unit of sale** | One **instance** = one farm (customer). |
| **Isolation** | **Physical:** one Firebase project, one web deployment, one app build (or flavour) per instance. No cross-customer data sharing. |
| **Identity** | One app install = one farm. No “switch farm” in-app; each instance is configured for that farm only. |
| **Branding** | Optional **white-label** per instance: app name, logo, colours, web domain (e.g. `acmefarm.herdmanager.app` or custom domain). |

**Why this fits selling to farms**

- **Trust and compliance:** “Your data lives in your own instance.” Easy to explain and to satisfy data-residency or audit requirements.
- **Customisation:** Per-farm branding and (later) config without affecting other customers.
- **Pricing:** Natural **per-farm** or **per-instance** subscription; optional tiers by herd size or features.
- **Support and ops:** You can treat each instance as a clear boundary for backups, rollbacks, and support.

---

## 2. What one “instance” is

Each sold instance consists of:

| Component | Per instance |
|-----------|----------------|
| **Firebase project** | Auth, Firestore, Storage (and optional Cloud Functions) for that farm only. |
| **Web dashboard** | Deployed app (e.g. Next.js) pointing at that Firebase project. One URL per instance (e.g. subdomain or custom domain). |
| **Android app** | Same codebase; either **one app** with config (e.g. build-time or runtime config pointing to the instance’s Firebase), or **per-customer build** (flavour/white-label) with bundle ID and branding. |
| **Config** | Firebase config (`google-services.json` / `NEXT_PUBLIC_FIREBASE_*`) and any app name, logo, or API URLs are instance-specific. |

**Single codebase:** You maintain one HerdManager repo. Instance-specific values are injected at **build time** (e.g. product flavours, env files) or at **runtime** (e.g. config fetched from a manifest URL or from your provisioning system).

### 2.1 Unique instance/solution identifier

Every provisioned HerdManager Solution must have a **stable, unique identifier** (e.g. `solutionId` or `instanceId`) assigned at creation. This ID:

- **Uniquely identifies** the deployment across its lifetime (Firebase project, web URL, or bundle ID may change; the solution ID does not).
- **Keys the solution registry** (`solutions/{solutionId}`) and any ops, billing, or support systems.
- **Can be embedded** in instance config (e.g. `NEXT_PUBLIC_SOLUTION_ID` or in the app’s runtime config) so the app and support portal can send feedback, support requests, or telemetry tagged with this ID.
- **Simplifies automation** (scripts, CI, billing) that loop over “all instances” or look up a customer by solution ID.

Use a format that is URL- and DB-friendly (e.g. UUID, or a short slug like `acme-farm-01`). Generate and store it in the solution registry as soon as the solution is created (e.g. step 0 in the provisioning checklist).

---

## 3. Provisioning a new HerdManager Solution (onboarding a new farm)

When you sign a new customer (farm), you need a repeatable process to create their solution.

### 3.1 Checklist per new solution

1. **Create Firebase project** (or use a script/template).
   - **Assign a unique solution/instance ID** (e.g. UUID or slug) and record it in the solution registry. Use this ID everywhere (registry, billing, support).
   - Enable Auth (Email/Password).
   - Create Firestore DB; deploy security rules (same rules, different project).
   - Enable Storage if needed for photos.
2. **Register apps in Firebase:**
   - **Android:** Register app with a bundle ID (e.g. `com.herdmanager.app` for generic, or `com.yourcompany.herdmanager.farm_acme` for white-label). Download `google-services.json`.
   - **Web:** Add Web app; get `NEXT_PUBLIC_FIREBASE_*` for the dashboard.
3. **Configure and deploy web dashboard:**
   - Set env (e.g. `.env.production`) with this instance’s Firebase config and optional app name/URL.
   - Deploy to a URL (e.g. `acme.yourproduct.com` or customer subdomain).
4. **Android:** Either ship the **generic** app (user enters instance URL or you ship one build per instance) or produce a **customer build** with that instance’s `google-services.json` and branding.
5. **Handoff:** Send customer their web URL, Android APK or Play Store link (if per-customer track), and sign-in instructions. Create first user (or invite) when you adopt the target auth model (solution service account + user store and RBAC); see [AUTH-AND-IDENTITY.md](AUTH-AND-IDENTITY.md).

### 3.2 Automation (recommended)

- **Runbook:** For a repeatable manual flow (create project → register → deploy rules/functions → web env → deploy web), see [PROVISIONING-RUNBOOK.md](../PROVISIONING-RUNBOOK.md). Use `provision-instance.js <solutionId> [--deploy-rules] [--deploy-functions]` after the Firebase project exists.
- **GCP project creation:** To create a *new* GCP project from scratch (no Console step), use **Option C** in the runbook: `create-gcp-project.js <solutionId> --project-id <id> --name "Farm" (--org-id N | --folder-id N) [--billing-account ...] [--deploy]`. This uses the Cloud Resource Manager API; the service account needs `resourcemanager.projects.create` on the parent org/folder. With `--deploy`, the script then runs `create-firebase-project.js` and `provision-instance.js` so the full chain is automated.
- **Full onboarding scripts:** `onboard-farm.sh` (Unix) and `onboard-farm.ps1` (Windows) chain create-solution → create-gcp-project [--deploy] so one command can onboard a new farm when org/folder and credentials are in place. See runbook § “Full automated onboarding”.
- **Script or IaC** (e.g. Firebase CLI + scripts, or Terraform/Pulumi for GCP/Firebase) to create project, enable APIs, deploy rules and indexes.
- **Solution registry:** Your own small DB or config store (e.g. `solutions/{solutionId}` with `solutionId` (unique instance ID), `firebaseProjectId`, `webUrl`, `bundleId`, `farmName`, `createdAt`) for support, ops, and billing. Not used at runtime by the app for core behaviour; can be referenced by support portal or config endpoint. See §2.1.
- **Build pipeline:** From one codebase, build Android variants (flavours) or web envs per solution (or per cohort) so releases are reproducible.

---

## 4. Branding (white-label) per instance

To sell “their” product to each farm:

| Element | How to make it instance-specific |
|---------|-----------------------------------|
| **App name** | Build-time: product flavour or env (e.g. `HerdManager` vs `Acme Cattle`). Web: env (e.g. `NEXT_PUBLIC_APP_NAME`). |
| **Logo / icon** | Android: flavour resources (e.g. `src/acme/res/mipmap`). Web: env or static asset per deploy. |
| **Colours / theme** | Build-time or runtime: theme file or config URL per instance. |
| **Web domain** | Deploy to instance-specific URL (subdomain or custom domain). Optional: show “Powered by HerdManager” in footer for vendor branding. |

Keep a **default/generic** build for trials or for customers who don’t need custom branding; and **white-label** builds for those who do.

---

## 5. Software updates across solutions

You have one codebase and many HerdManager Solutions. Updates should be consistent and controllable.

| Layer | Strategy |
|-------|----------|
| **Codebase** | Single repo; versioned releases (e.g. semver). Changelog and release notes per version. |
| **Firebase (per instance)** | Same Firestore rules and indexes for all instances. Deploy rules/index changes to **all** projects (script or CI that loops over your instance list). Prefer backward-compatible changes; use a **minimum client version** in Firestore or Cloud Function if you need to phase out old app behaviour. |
| **Web** | Build once per version (or per instance if env differs). Deploy to each instance’s host (e.g. Vercel per project, or one server with multiple envs). Optionally: “New version available – refresh” via a version endpoint. |
| **Android** | **Option 1 – Single Play Store listing:** One app; at first launch (or in Settings) user selects or enters their “instance” (e.g. web URL or instance code). App then uses that instance’s Firebase config (fetched from your manifest). Updates are one rollout for all customers. **Option 2 – Per-customer builds:** Separate Play Store track or internal distribution per customer; you push the same version to each track. More control per customer, more ops. |
| **Rollout** | Staged by instance cohort if needed (e.g. pilot customers first, then rest). Per-instance rollback: redeploy previous web version or point app to previous backend if you use a config server. |

**Best practice:** Define a **minimum supported version** (e.g. in Firestore `config/minVersion` or in your manifest). Old app versions that are below min get “Please update the app” and a link to the right store/build. Gives you a clean way to drop support for very old clients after a grace period.

---

## 6. Operations and support

| Area | Recommendation |
|------|----------------|
| **Backups** | Firestore backups (scheduled exports or Point-in-Time Recovery) per Firebase project. Retain per your SLA. |
| **Monitoring** | Per-project metrics (Auth, Firestore, Storage) and alerts. Optional: central dashboard that aggregates health across instances (you only). |
| **Support** | Each customer has a known instance (solution ID + URL + project ID). Support can look at that instance’s data (with access controls) without touching others. |
| **Security** | Same Firestore rules everywhere. Rotate any shared admin or CI keys; prefer per-project or least-privilege service accounts. |
| **Incidents** | Isolate impact: an issue in one instance doesn’t affect others. Rollback or fix can be per-instance. |

---

## 6.1 Support, feedback, and improvement channels (instance owner/user)

Instance owners and users need clear ways to **suggest improvements**, **report issues**, and **access solution support**. This keeps the product aligned with real use and reduces friction for paying customers.

| Need | Approach |
|------|----------|
| **Suggest improvements** | In-app or web: “Suggest a feature” or “Feedback” link (e.g. in Settings → About or footer) that opens a form or redirects to a support portal. Submissions are tagged with the instance’s **solution ID** (and optionally user/contact) so you can prioritise and track by customer. |
| **Report issues** | “Report a problem” or “Contact support” in the app and web dashboard, again keyed by solution ID. Optionally capture app version, platform, and a short description so support (or an AI agent) can triage. |
| **Access support** | Single entry point: e.g. “Help & support” in the app and web that leads to a **support portal** or help centre. The portal URL can be instance-aware (e.g. `support.yourproduct.com?solutionId=...`) so context is pre-filled. |

**Future: AI support agent / support desk.** The intention is to introduce an **AI-powered support agent** for the solution — a first-line support desk that can answer common questions, guide users through flows, and collect structured issue reports, all scoped to the instance (solution ID). The agent can hand off to human support when needed. Design support flows and data (e.g. FAQs, runbooks, conversation logs) so they can be wired to an AI support layer later without changing the instance model.

---

## 7. Billing and procurement strategy (effortless income stream)

A clear billing and procurement model turns each instance into a **predictable, low-friction income stream** and makes scaling manageable.

### 7.1 Subscription as the core income

- **Recurring subscription** per instance (per farm), billed monthly or annually. This is the primary revenue; one active instance = one paying subscription.
- **Tiers** (optional): e.g. **Basic** (single user, core features), **Team** (multiple users, full features), **Enterprise** (white-label, SLA, dedicated support). Price by herd size, number of users, or feature set.
- **One-off setup fee** (optional): Covers provisioning and optional white-label; paid at onboarding. Keeps base subscription focused on software and support.

### 7.2 Procurement flow (sign-up → provision → billing)

1. **Lead / sign-up:** Customer signs up (web form, sales, or partner). Capture contact, farm name, and chosen plan.
2. **Provision:** Create the solution (assign **solution ID**, create Firebase project, deploy web, ship app) using the checklist in §3.1. Link the solution ID to the customer and plan in your billing system.
3. **Go-live and first invoice:** Hand off credentials and URLs; start subscription billing from go-live (or after a short trial). Invoice by solution ID so each instance maps to one billing account.
4. **Renewals:** Automatic renewal (monthly or annual) with payment retry and dunning (e.g. email reminders, then suspend access if unpaid). Minimise manual steps so revenue continues without per-customer effort.

### 7.3 Payment and billing systems

- **Payment provider:** Use a subscription-aware provider (e.g. **Stripe**, Paddle, or your region’s equivalent) for cards, direct debit, or invoices. Stripe (or similar) gives you products/prices, customer per solution ID, and automatic recurring billing.
- **Billing entity:** One billing customer per solution ID (or per legal entity if one customer has multiple farms). Keeps reporting simple: revenue per instance, MRR/ARR, churn per solution.
- **Usage-based add-ons (optional):** If you add usage-based features later (e.g. extra storage, API calls), meter by solution ID and add to the same subscription or invoice. Enables effortless upsell without new contracts.

### 7.4 Effortless income in practice

- **Automate provisioning** so new sign-ups become billable instances with minimal manual work (§3.2).
- **Automate renewals and dunning** so you don’t chase payments by hand; failed payments trigger emails and, after a grace period, suspend access (e.g. via min-version or auth).
- **Single source of truth:** Solution registry + billing system both keyed by solution ID. One place to see “this instance is on Plan X, paid until Y.”
- **Scaling:** More instances = more subscriptions; same codebase and same billing logic. Optional: partner or reseller channel where partners bring customers and you bill the partner or the end customer; solution ID still identifies each deployment.

---

## 8. Relation to other docs

- **Multi-instance documentation index:** [MULTI-INSTANCE-STRATEGY.md](../MULTI-INSTANCE-STRATEGY.md) (in `docs/`) is the entry point for all multi-instance strategy docs; it summarises this strategy and lists every related document.
- **Data and sync within one instance:** Current app and [SYNC-DESIGN.md](SYNC-DESIGN.md) apply **inside** each instance (multi-device merge, central vs field data). No change to data model for “one farm per instance” — you already have a single logical farm per app.
- **Multi-tenant Option A:** Only relevant if a **single customer** (e.g. large enterprise) asks for multiple farms in one instance with farm switcher. Then you’d add tenant/farm ID and membership to that instance only. Default product remains: one instance per farm.
- **Software update details:** Concrete practices (versioning, In-App Update, min version, changelog) are in [MULTI-FARM-AND-UPDATES.md](MULTI-FARM-AND-UPDATES.md) § Software updates; apply them per instance or across all instances as above.

---

## 9. Summary

| Decision | Choice for “sell instance per farm” |
|----------|-------------------------------------|
| **Unit of sale** | One instance = one farm (customer). |
| **Instance ID** | Unique, stable identifier per solution (solutionId/instanceId); assigned at creation; used in registry, billing, support, and config. |
| **Isolation** | One Firebase project + one web deploy + one app config (or build) per instance. |
| **Codebase** | Single HerdManager repo; instance-specific config at build or runtime. |
| **Provisioning** | Repeatable checklist + automation (assign solution ID, Firebase + web deploy + optional Android flavour). |
| **Branding** | Optional white-label per instance (name, logo, domain). |
| **Billing** | Recurring subscription per instance (keyed by solution ID); payment provider (e.g. Stripe); automated renewals and dunning; optional setup fee and tiers. |
| **Support & feedback** | In-app/web: suggest improvements, report issues, access support (all tagged with solution ID). Future: AI support agent as first-line support desk. |
| **Updates** | One version; deploy to all instances (or by cohort); min-version and In-App Update for Android. |
| **Ops** | Backups, monitoring, and support per instance; same rules and code everywhere. |

This document is the **primary architecture** for selling HerdManager to farming businesses as a dedicated instance per farm.
