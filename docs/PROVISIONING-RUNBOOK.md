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

## Quick reference

| Step | Command or action |
|------|-------------------|
| Create solution | `node scripts/create-solution.js --name "Farm Name"` |
| Add Firebase to GCP project (optional) | `node scripts/create-firebase-project.js <id> --project-id <gcp-id> [--deploy]` (requires GOOGLE_APPLICATION_CREDENTIALS) |
| Register Firebase project | `node scripts/update-solution.js <id> --firebase-project-id <project-id>` |
| Deploy rules + functions | `node scripts/provision-instance.js <id> --deploy-rules [--deploy-functions]` |
| Web env snippet | `node scripts/env-for-solution.js <id> [--support-url <url>]` |
| Register web URL | `node scripts/update-solution.js <id> --web-url <url>` |
| Link billing | `node scripts/link-billing.js <id>` |
| Deploy to all instances | `./scripts/deploy-all-instances.sh` or `.\scripts\deploy-all-instances.ps1` |

---

## See also

- [INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md) §3.1–3.2 – checklist and automation notes
- [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) – scripts and multi-instance ops
- [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) – multi-instance release steps
