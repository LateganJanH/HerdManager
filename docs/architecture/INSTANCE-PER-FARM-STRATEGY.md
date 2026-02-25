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

---

## 3. Provisioning a new HerdManager Solution (onboarding a new farm)

When you sign a new customer (farm), you need a repeatable process to create their solution.

### 3.1 Checklist per new solution

1. **Create Firebase project** (or use a script/template).
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

- **Script or IaC** (e.g. Firebase CLI + scripts, or Terraform/Pulumi for GCP/Firebase) to create project, enable APIs, deploy rules and indexes.
- **Solution registry:** Your own small DB or config store (e.g. `solutions/{solutionId}` with `firebaseProjectId`, `webUrl`, `bundleId`, `farmName`) for support and ops. Not used at runtime by the app; for you only.
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
| **Support** | Each customer has a known instance (URL + project ID). Support can look at that instance’s data (with access controls) without touching others. |
| **Security** | Same Firestore rules everywhere. Rotate any shared admin or CI keys; prefer per-project or least-privilege service accounts. |
| **Incidents** | Isolate impact: an issue in one instance doesn’t affect others. Rollback or fix can be per-instance. |

---

## 7. Pricing and packaging (vendor view)

- **Per–HerdManager Solution subscription** (per farm) is the natural unit. Tiers can be based on herd size, number of users, or feature set (e.g. basic vs. premium support or extra modules).
- **One-off setup fee** can cover provisioning and optional white-label.
- **Updates and support:** Included in subscription; you push one version to all solutions (or to a cohort) and support one codebase.

---

## 8. Relation to other docs

- **Data and sync within one instance:** Current app and [SYNC-DESIGN.md](SYNC-DESIGN.md) apply **inside** each instance (multi-device merge, central vs field data). No change to data model for “one farm per instance” — you already have a single logical farm per app.
- **Multi-tenant Option A:** Only relevant if a **single customer** (e.g. large enterprise) asks for multiple farms in one instance with farm switcher. Then you’d add tenant/farm ID and membership to that instance only. Default product remains: one instance per farm.
- **Software update details:** Concrete practices (versioning, In-App Update, min version, changelog) are in [MULTI-FARM-AND-UPDATES.md](MULTI-FARM-AND-UPDATES.md) § Software updates; apply them per instance or across all instances as above.

---

## 9. Summary

| Decision | Choice for “sell instance per farm” |
|----------|-------------------------------------|
| **Unit of sale** | One instance = one farm (customer). |
| **Isolation** | One Firebase project + one web deploy + one app config (or build) per instance. |
| **Codebase** | Single HerdManager repo; instance-specific config at build or runtime. |
| **Provisioning** | Repeatable checklist + automation (Firebase + web deploy + optional Android flavour). |
| **Branding** | Optional white-label per instance (name, logo, domain). |
| **Updates** | One version; deploy to all instances (or by cohort); min-version and In-App Update for Android. |
| **Ops** | Backups, monitoring, and support per instance; same rules and code everywhere. |

This document is the **primary architecture** for selling HerdManager to farming businesses as a dedicated instance per farm.
