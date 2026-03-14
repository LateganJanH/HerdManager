# HerdManager – Changelog

## Unreleased

## 0.5.0 - 2026-03-14

### Web
- **Weight & weaning CRUD on Profiles:** Animal detail modal shows a Weight & weaning section: list of weight records, “Log weight” (add), Edit and Delete per record; add/update/delete via Firestore; real-time subscription so list updates when weights change.
- **Tasks & reminders full CRUD:** New **Tasks** tab (nav + keyboard shortcut 6): list tasks, status filters (All, Pending, In progress, Done, Cancelled), summary (open / due today / overdue), Add task, Edit and Delete per task; optional due date and link to animal; Firestore add/update/delete; Home “Tasks & reminders” card now links to the Tasks tab (“Manage tasks →”).
- **ChunkLoadError / loading stability:** Layout loads Providers via `next/dynamic` to keep the layout chunk smaller; `experimental.optimizePackageImports` for React Query; **dev:clean** script clears `.next` and runs `next dev`; global-error shows “Refresh the page” for chunk/timeout errors.

### Provisioning & instance-per-farm
- **GCP project creation:** `create-gcp-project.js` creates a new GCP project via Cloud Resource Manager API (requires `--org-id` or `--folder-id`); optional `--billing-account` and `--deploy` to chain Firebase provisioning and rules/functions deploy. Parent format fixed for API (`organizations/ID` or `folders/ID`).
- **Full onboarding scripts:** `onboard-farm.sh` (Unix) and `onboard-farm.ps1` (Windows) chain create-solution → create-gcp-project [--deploy]. **Runbook:** Option C (automated GCP project creation) and “Full automated onboarding” section; create-solution.js supports `--print-solution-id` for scripting.

### Android
- **Weight section:** Edit/Delete buttons on weight records use `Modifier.weight(1f)` so the action row stays visible; content descriptions “Edit weight” / “Delete weight”. Success handler clears weight edit state when operation completes.

---

## 0.4.0 - 2026-03-11

### Android
- **Tasks & reminders:** Farm-wide task board (Tasks screen from Home), summary (open / due today / overdue), status filters, optional link to animal; backup/restore includes tasks.
- **Body condition scoring (BCS):** Condition records (animal, date, score 1–9, notes); Condition section on animal detail (history list, “Record score” with date picker and score selector); Analytics shows average BCS for current herd and optional BCS distribution (count per score 1–9); sync and backup include condition records.
- **FCM due-soon notifications:** Push notifications for due-soon alerts; device FCM token stored in Firestore; Cloud Function sends scheduled due-soon notifications with body breakdown (e.g. "2 calving due soon") and data payload (count, calvingDue, pregnancyCheckDue, withdrawalDue, weaningDue); **tapping the notification deep-links to the Alerts tab**; notification permission and messaging service registered.
- **At-risk / predictive insights:** Home "Attention needed" card: open cows/heifers with no breeding in 60 days, calves with weaning weight overdue; tap card to open Alerts or tap a line to open animal detail.
- **Stability & sync:** Room migration fix for condition_records (schema aligned with entity: updatedAt default, dateEpochDay index); sync return type and error logging; Profiles herd filter dropdown and error messaging; “current herd” logic extended to exclude SOLD, DECEASED, CULLED from counts, due-soon, and analytics; By status and By sex in Analytics use active herd so totals add up; By category (Calves, Heifers, Cows, Bulls, Steers) added to Analytics.

---

This project has platform-specific changelogs:

- **Web dashboard:** [web/CHANGELOG.md](web/CHANGELOG.md) – HerdManager web app (Next.js).
- **Android:** Version is in [android/app/build.gradle.kts](android/app/build.gradle.kts) (`versionCode`, `versionName`). Release notes for Android are summarized in [docs/NEXT-STEPS.md](docs/NEXT-STEPS.md) and [docs/RELEASE-CHECKLIST.md](docs/RELEASE-CHECKLIST.md).

For release steps (version bump, tag, deploy), see [docs/RELEASE-CHECKLIST.md](docs/RELEASE-CHECKLIST.md).
