# Provisioning runbook – new HerdManager instance

Step-by-step flow to onboard a new farm (solution) when using the instance-per-farm model. Use with [INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md) §3 and [PHASE-LATER-ROADMAP.md](PHASE-LATER-ROADMAP.md).

---

## 1. Create solution and register it

From repo root:

```bash
node scripts/create-solution.js --name "Acme Farm"
```

Note the printed `solutionId` (e.g. `acme-farm-xyz`). The script adds the entry to `scripts/solution-registry.json`.

---

## 2. Create Firebase project

**Option A – Manual (Console)**  
1. Open [Firebase Console](https://console.firebase.google.com/) and **Create project** (or use an existing Google Cloud project).  
2. **Enable**: Authentication (Email/Password), Firestore Database, Storage (if needed).  
3. Copy the **Project ID**.

**Option B – Add Firebase to an existing GCP project (script)**  
If you already have a Google Cloud project ID (e.g. from Cloud Console):

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
node scripts/create-firebase-project.js <solutionId> --project-id <gcp-project-id> [--deploy]
```

The service account must have Firebase Admin SDK Administrator (or equivalent) and the Firebase Management API enabled. The script adds Firebase to the project, updates the registry with `firebaseProjectId`, and optionally runs `provision-instance.js --deploy-rules --deploy-functions` when `--deploy` is set. See script header for details.

**Option C – Automated GCP project creation (script)**  
To create a **new** GCP project from scratch (no Console step), use the Cloud Resource Manager API via `create-gcp-project.js`. You must have an organization or folder to create the project under.

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
node scripts/create-gcp-project.js <solutionId> \
  --project-id <gcp-project-id> \
  --name "Farm Name" \
  (--org-id <orgNumericId> | --folder-id <folderNumericId>) \
  [--billing-account billingAccounts/XXXX-XXXXXX-XXXXXX] \
  [--deploy]
```

- **Required:** `--project-id` (6–30 chars, lowercase letters/numbers/hyphens, globally unique), `--name` (display name), and either `--org-id` (numeric organization ID) or `--folder-id` (numeric folder ID).  
- **Optional:** `--billing-account` (e.g. `billingAccounts/012345-6789AB-CDEF01`) to link billing; `--deploy` to run `create-firebase-project.js <solutionId> --project-id <id> --deploy` after the project is created (add Firebase and provision rules/functions in one go).  
- **Service account:** Must have `resourcemanager.projects.create` and `resourcemanager.projects.get` on the parent; Cloud Resource Manager (and optionally Cloud Billing) API enabled. For `--deploy`, the same account is used for Firebase (see Option B).  

After Option C (without `--deploy`), run Option B’s `create-firebase-project.js` command to add Firebase to the new project.

---

## 3. Register the project in the solution registry

```bash
node scripts/update-solution.js <solutionId> --firebase-project-id <project-id>
```

Example:

```bash
node scripts/update-solution.js acme-farm-xyz --firebase-project-id acme-farm-12345
```

---

## 4. Deploy Firestore rules and Cloud Functions

**Option A – use the provisioning script (recommended):**

```bash
node scripts/provision-instance.js <solutionId> --deploy-rules --deploy-functions
```

This runs `firebase use <projectId>` then deploys rules and functions. Omit `--deploy-functions` if you do not use Cloud Functions yet.

**Option B – manual:**

```bash
firebase use <project-id>
firebase deploy --only firestore:rules
firebase deploy --only functions
```

Copy `firestore.rules.example` to your project’s `firestore.rules` (or use the repo’s rules) before deploying.

---

## 5. Register web and Android apps in Firebase (if needed)

- **Web:** Project settings → Your apps → Add app → Web. Copy the `firebaseConfig` (apiKey, authDomain, projectId, etc.) for `NEXT_PUBLIC_FIREBASE_*` in the dashboard’s env.
- **Android:** Add app → Android; use bundle ID (e.g. `com.herdmanager.app`). Download `google-services.json` for customer builds.

---

## 6. Set web env and deploy dashboard

Get the env snippet for this solution:

```bash
node scripts/env-for-solution.js <solutionId> [--support-url https://support.example.com]
```

Set the printed vars in your web app’s production env (e.g. Vercel/Netlify), plus the Firebase config from step 5. Deploy the web app to the instance’s URL (e.g. subdomain or custom domain).

---

## 7. Update registry with web URL

After the dashboard is live:

```bash
node scripts/update-solution.js <solutionId> --web-url https://acme.yourproduct.com
```

---

## 8. (Optional) Link billing

If you use Stripe and the solution registry for billing:

```bash
node scripts/link-billing.js <solutionId>
```

With `STRIPE_SECRET_KEY` set and the `stripe` package installed, this creates a Stripe customer and updates the registry. Otherwise the script prints manual steps.

---

## Full automated onboarding (create solution + GCP project + Firebase + deploy)

When you have a parent **organization** or **folder** and a service account with the right permissions, you can onboard a new farm in one flow:

1. **Create solution** and capture `solutionId`:
   ```bash
   node scripts/create-solution.js --name "Acme Farm"
   # Note the printed solutionId (e.g. acme-farm-xyz), or use --print-solution-id when chaining:
   SOLUTION_ID=$(node scripts/create-solution.js --name "Acme Farm" --print-solution-id)
   ```

2. **Create GCP project, add Firebase, and provision** in one step:
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
   node scripts/create-gcp-project.js "$SOLUTION_ID" \
     --project-id acme-farm-prod \
     --name "Acme Farm" \
     --org-id 123456789 \
     [--billing-account billingAccounts/XXXX-XXXXXX-XXXXXX] \
     --deploy
   ```
   With `--deploy`, after the GCP project is created, the script runs `create-firebase-project.js <solutionId> --project-id <id> --deploy`, which adds Firebase, updates the registry, and runs `provision-instance.js --deploy-rules --deploy-functions`.

Alternatively use the helper scripts (from repo root):

- **Unix:** `./scripts/onboard-farm.sh "Farm Name" <gcp-project-id> <parent-numeric-id> org|folder [--billing billingAccounts/...] [--deploy]`
- **Windows:** `.\scripts\onboard-farm.ps1 -FarmName "Farm Name" -ProjectId <gcp-project-id> -ParentId <numeric-id> -ParentType org|folder [-BillingAccount ...] [-Deploy]`

These run create-solution, then create-gcp-project (with optional `--deploy`). Set `GOOGLE_APPLICATION_CREDENTIALS` first. See script headers for full usage.

---

## Quick reference

| Step | Command or action |
|------|-------------------|
| Create solution | `node scripts/create-solution.js --name "Farm Name"` |
| Create GCP project (optional) | `node scripts/create-gcp-project.js <id> --project-id <gcp-id> --name "Farm" (--org-id N \| --folder-id N) [--billing-account ...] [--deploy]` (Option C; requires GOOGLE_APPLICATION_CREDENTIALS) |
| Add Firebase to GCP project (optional) | `node scripts/create-firebase-project.js <id> --project-id <gcp-id> [--deploy]` (requires GOOGLE_APPLICATION_CREDENTIALS) |
| Register Firebase project | `node scripts/update-solution.js <id> --firebase-project-id <project-id>` |
| Deploy rules + functions | `node scripts/provision-instance.js <id> --deploy-rules [--deploy-functions]` |
| Web env snippet | `node scripts/env-for-solution.js <id> [--support-url <url>]` |
| Register web URL | `node scripts/update-solution.js <id> --web-url <url>` |
| Link billing | `node scripts/link-billing.js <id>` |
| Deploy to all instances | `./scripts/deploy-all-instances.sh` or `.\scripts\deploy-all-instances.ps1` |

---

## See also

- [INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md) §3.1–3.2 – checklist and automation notes (incl. GCP project creation and full onboarding scripts)
- [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) §5 – solution registry, create-gcp-project.js, onboard-farm scripts, deploy to all instances
- [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) – multi-instance release steps
