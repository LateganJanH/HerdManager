# Multi-farm (multi-tenant) strategy and software updates

## 1. Product strategy: instance per farm (Option B)

**Chosen product model for selling to farming businesses:** **One instance per farm.** Each customer (farm) gets a **dedicated deployment**: own Firebase project, own web dashboard URL, and app configured (or built) for that instance only. No shared multi-tenant database; strong isolation and optional white-label per customer.

**Primary design doc for this model:** [INSTANCE-PER-FARM-STRATEGY.md](INSTANCE-PER-FARM-STRATEGY.md). It covers provisioning, branding, updates across instances, ops, and pricing from the vendor/architect perspective.

Below, **Option B** is summarised; **Option A** (single app, multi-tenant) is retained only for the case where a **single customer** (e.g. one enterprise) wants multiple farms inside one instance with a farm switcher.

---

### 1.1 Option B: Separate instance per farm (product strategy)

**Idea:** Each farm is a **sold instance**: own Firebase project, own web deploy, own app config or build. Data and auth are not shared across instances.

**Data model**

- No tenant ID in the app; “this instance = this farm” is implicit.
- Firestore structure stays as today (`users/{uid}/...`) per project.

**Auth**

- Per-project Firebase Auth; no cross-instance identity.

**Why this is the preferred design for selling to farms**

- **Trust:** “Your data lives in your own instance.”
- **Isolation:** Compliance and data residency are easy to explain; one customer’s issue doesn’t affect others.
- **Branding:** Optional white-label (app name, logo, web domain) per instance.
- **Pricing:** Natural per-farm / per-instance subscription.

**Ops**

- One codebase; instance-specific config at build or runtime. Provision new instance = new Firebase project + web deploy + optional app flavour. Updates = deploy same version to all instances (or by cohort). See [INSTANCE-PER-FARM-STRATEGY.md](INSTANCE-PER-FARM-STRATEGY.md).

---

### 1.2 Option A: Single app, multi-tenant (only if one customer wants multiple farms in one instance)

**Idea:** One app and one Firebase project; multiple **farms** (tenants) in the same DB; data scoped by **farm ID**; user can switch “current farm”. Use only when a **single customer** (e.g. enterprise) requires multiple farms in one instance.

**Data model**

- Tenant dimension everywhere: e.g. `farms/{farmId}/users/{uid}/...` or `users/{uid}/...` with `farmId` on documents and a **membership** store (`farms/{farmId}/members/{uid}` or `users/{uid}/farm_memberships`).
- Android/Web: “current farm” selection; all queries and sync filtered by that farm.

**Auth**

- One account can have access to several farms (membership); in-app farm switcher.

**Pros:** One deployment for that customer; one app to update.  
**Cons:** Tenant isolation must be enforced in every query and in Firestore rules.

**Best for:** Single customer (e.g. one large enterprise) that wants multiple farms in one instance. Not the default product model.

---

### 1.3 Firestore structure sketch (Option A only, when used)

Two common patterns:

**Pattern 1 – Farm at root (good if farm is the main boundary)**

```
farms/{farmId}
  /settings/farm     (name, contact, alert days)
  /members/{uid}     (role: ADMIN, MANAGER, WORKER)
  /animals/{animalId}
  /herds/{herdId}
  /breeding_events/{id}
  /calving_events/{id}
  ...
```

Security rule: allow read/write only if `get(/databases/$(database)/documents/farms/$(farmId)/members/$(request.auth.uid)).data.role` exists.

**Pattern 2 – Keep users at root, add farmId to data (minimal change from today)**

```
users/{uid}/
  /farm_memberships/{farmId}   (role)
  /animals/{animalId}          (each doc has farmId)
  /breeding_events/{id}        (each doc has farmId)
  ...
```

Rule: allow if `request.auth.uid == uid` and document’s `farmId` is in the user’s memberships. (Requires storing memberships under the user or in a separate collection and checking in rules.)

For **Option B (instance per farm)** no Firestore structure change is needed: each project has a single farm’s data; current `users/{uid}/...` layout is used as-is.

---

## 2. Software update best practices

Goals: ship fixes and features safely, keep clients on a supported version, and avoid broken behaviour when backend and clients must stay in sync.

---

### 2.1 Android (Play Store app)

| Practice | Description |
|----------|-------------|
| **Version code / version name** | Bump `versionCode` (integer) every release; use `versionName` (e.g. `1.2.3`) for display. In `build.gradle.kts`: `versionCode = 42`, `versionName = "1.2.3"`. |
| **Staged rollout** | Use Play Console’s staged rollout (e.g. 10% → 50% → 100%) to catch regressions before full release. |
| **In-app updates (Google Play)** | Use the [In-App Update API](https://developer.android.com/guide/playcore/in-app-updates) so users can update without leaving the app. Prefer “flexible” for non-critical updates; use “immediate” for critical or breaking changes. |
| **Minimum supported version (backend)** | If you have a backend or Firestore rules that depend on client behaviour, define a **minimum app version**. Expose it from an endpoint or a Firestore doc (e.g. `config/minVersion`). On app start, if current `versionCode` &lt; min, show “Please update the app” and deep-link to Play Store. |
| **Deprecation / EOL** | For critical security or data-safety fixes, consider a short grace period after release, then reject older versions (e.g. block sign-in or sync) and show “Update required”. |
| **Changelog** | Keep a `CHANGELOG.md` or release notes in the repo; mention breaking changes and required actions (e.g. “Update to 1.3.0 before 2026-05-01”). |

---

### 2.2 Web dashboard

| Practice | Description |
|----------|-------------|
| **Version in UI / build** | Set a version (e.g. from `package.json` or env) and show it in Settings or footer (e.g. “HerdManager Web 1.2.3”). Helps support and confirms which build is running. |
| **Cache busting** | Use build hashes in asset filenames (Next.js does this by default). Ensure `Cache-Control` headers for HTML are short (e.g. `no-cache` or short `max-age`) so users get the latest bundle after deploy. |
| **Deploy strategy** | Prefer blue-green or rolling deploys; run smoke tests after deploy. Use a single production URL; avoid long-lived “versioned” URLs (e.g. `v1.example.com`) unless you need to support multiple API versions. |
| **Minimum version (optional)** | If web talks to a versioned API, expose a “minimum web version” or “supported until” and show a banner when the build is too old: “Refresh the page to get the latest version.” |
| **Refresh prompt** | For long-lived sessions, optionally detect when a new version is deployed (e.g. periodic check of a `/api/version` or `version.json`) and show “New version available. Refresh to update.” |

---

### 2.3 Backend / Firebase (Cloud Functions, Firestore rules)

| Practice | Description |
|----------|-------------|
| **Version rules and indexes** | When changing Firestore rules or indexes, deploy in a backward-compatible way. Avoid breaking existing clients still on an older app version until they’ve had time to update. |
| **Feature flags / min version** | Store minimum required version (and optionally feature flags) in Firestore (e.g. `config/app`) or in a Cloud Function. Clients read this on launch and block or adapt behaviour. |
| **API versioning** | If you add REST or Callable APIs, version the contract (e.g. `/v1/...` or `callable("v1_doSomething")`) so you can evolve without breaking old clients. |

---

### 2.4 Summary table

| Area | Recommended practice |
|------|------------------------|
| **Android** | Version code + name in build; staged rollout; In-App Update API; optional min-version check and “Update required” for old versions. |
| **Web** | Version in UI; cache busting; short cache for HTML; optional “New version available – refresh” when deploy is detected. |
| **Backend** | Backward-compatible rule/index changes; optional `config/minVersion` (and feature flags) for clients. |
| **Releases** | Changelog; communicate breaking changes and EOL dates; consider grace period before blocking old versions. |

---

## 3. Where this lives in the repo

- **Multi-farm:** Extend [DATA-MODEL.md](DATA-MODEL.md) with the chosen tenant model and Firestore layout; implement membership and `farmId` scoping in sync and UI when you add multi-farm.
- **Updates:** Add a short “Release and update policy” to the main README or to [DEVELOPMENT-SETUP.md](../DEVELOPMENT-SETUP.md); implement min-version and In-App Update when you need to enforce or encourage updates.
