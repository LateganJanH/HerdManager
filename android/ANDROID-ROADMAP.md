# HerdManager Android – Development roadmap

Focus: bring the Android app on par with the MVP and with the web dashboard where it makes sense.

## Current state (as of this doc)

### Implemented

| Area | Status |
|------|--------|
| **Navigation** | Home, Herd list (Profiles), Breeding (Alerts), Herd summary (Analytics), Add/Edit animal, Animal detail, Farm settings |
| **Room DB** | Animals, breeding events, calving events, photos, health events, weight records, herds, herd assignments |
| **Animals** | CRUD, ear tag, status, sex, DOB, breed; add/edit/delete |
| **Breeding & calving** | Record service, due date (283d default), record calving, link calf |
| **Alerts** | Calving due and pregnancy check due on Breeding (Alerts) screen; pull-to-refresh |
| **Photos** | Store per animal (angle, uri, capturedAt, **latitude**, **longitude**); display in PhotosSection. **Geo-tag:** last known location (GPS or network) attached when capturing from camera or gallery. |
| **Farm settings** | Farm name, address, **multiple contacts** (name, phone, email), calving/pregnancy reminder days, gestation length, herds; DataStore |
| **Backup / restore** | JSON export and import (Farm settings) |
| **Export** | CSV and PDF export of herd (Herd list → overflow → Export herd to CSV / Export herd to PDF). |
| **UI** | Jetpack Compose, Material 3, bottom nav (**5 tabs**: Home, Profiles, Alerts, Analytics, Settings), Coil for images |
| **DI** | Hilt; repositories, ViewModels, DB |

### Not yet implemented (MVP gaps)

| MVP item | Description |
|----------|-------------|
| ~~**User auth**~~ | ✅ **Done.** Email/password sign-in via Firebase Auth; app gated on auth (sign-in screen when not signed in). Single user per device. Add `google-services.json` and enable Email/Password in Firebase Console — see [FIREBASE-SETUP.md](FIREBASE-SETUP.md). |
| ~~**Cloud sync**~~ | ✅ **Done.** Firestore sync: upload local data to `users/{uid}/...`, download and replace local. Settings → Sync: "Last synced" and "Sync now". Requires auth. |
| ~~**Photo geo-tag**~~ | ✅ **Done.** `PhotoEntity` and `Photo` have `latitude`/`longitude` (nullable); capture flow gets last known location (GPS or network) and passes to `addPhoto`. **Runtime:** `ACCESS_FINE_LOCATION` is requested when adding a photo (camera or gallery) for better geo-tag. |
| ~~**Pregnancy check due**~~ | Basic alerts include “pregnancy check due” (P0). ✅ **Done.** Alerts screen shows Calving due and Pregnancy check due with filter All/Calving/Pregnancy check as a separate list or filter. |

### Parity with web dashboard

- **Tabs:** Web has 5 tabs (Home, Profiles, Alerts, Analytics, **Settings**). Android has **5 bottom tabs**; Settings is in the bottom nav (parity).
- **Naming:** Web uses “Profiles” and “Alerts”; Android uses “Profiles” and “Alerts” (Breeding screen title is “Reproduction & Calving”). Optional: align Breeding screen title with web (“Alerts”) or keep current.
- **Data source / Refresh:** Web shows “Data: From API / Sample data” and “Refresh” on each tab. Android: **Home** and **Analytics** show “Last synced” and one-tap “Sync”; pull-to-refresh on both triggers cloud sync. Settings has full “Sync now” and backup/restore.

---

## Suggested order of work

1. **Parity / polish**
   - ~~Add **Settings** to the bottom navigation (5 tabs like web).~~ Done.
   - ~~"Due soon" / Alerts summary on Home.~~ Done. Home shows "X alert(s) due soon – View Alerts" when count > 0; tap goes to Alerts tab.
   - ~~Sync on Home and Analytics.~~ Done. Home and Analytics: one-tap “Sync”, pull-to-refresh triggers sync; “Last synced” and sync error + Dismiss. Theme (Dark/Light/System) in Settings → Appearance. Sign-out confirmation; Add/Edit animal Save disabled until required fields valid.

2. ~~**Photo geo-tag (MVP)**~~ ✅ Implemented.

3. ~~**Pregnancy check alerts (MVP)**~~ ✅
   - Use farm setting “pregnancy check due” days; derive “pregnancy check due date” from breeding/events where applicable.
   - Show pregnancy-check-due items on Breeding (Alerts) screen: e.g. filter/tabs “Calving” vs “Pregnancy check”, or a single list with type label.

4. ~~**Auth (MVP)**~~ ✅
   - Firebase and Firebase Auth added; AuthRepository, AuthViewModel, SignInScreen; app shows sign-in when not authenticated, main app when signed in. Email/password only; single user per device. See FIREBASE-SETUP.md.

5. ~~**Cloud sync (MVP)**~~ Done.
   - Design sync model (Firestore collections for animals, events, etc.; last-modified, conflict handling).
   - Implement upload of local changes and download of remote changes; optional “Sync now” and “Last synced” in Settings/Home.

6. **Later (post-MVP)**
   - AI photo recognition (ML Kit), voice input, health events, pedigree, etc., per main product roadmap.

---

## References

- [MVP Definition](../docs/architecture/MVP-DEFINITION.md)
- [Architecture](../docs/architecture/ARCHITECTURE.md)
- [Data model](../docs/architecture/DATA-MODEL.md)
- [API spec](../shared/api/openapi.yaml)
- [Web dashboard](../web) – behaviour and tabs to align with where useful
