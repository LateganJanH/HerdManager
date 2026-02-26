# HerdManager: Modern UI Trends & Functionality Gaps

## 1. Modern UI / Layout Trends (2024–2025)

### Current alignment
- **Material 3** – Using Material 3 theming, color roles, typography, shapes.
- **Rounded corners** – 12dp cards and inputs; consistent shape scale.
- **Clear hierarchy** – Title/subtitle in app bar, summary strip, section headings.
- **Offline-first** – Data in Room; no network dependency for core flows.
- **Dashboard-style metrics** – Herd count and “due soon” in app bar and summary strip. **Home “due soon” card** shows the first few due items (ear tag + “Calving in X days” / “Pregnancy check due today”) so the user sees who is due at a glance.

### Gaps vs current trends

| Trend | Status | Recommendation |
|-------|--------|----------------|
| **Pull-to-refresh** | Done | Home, Analytics, Breeding, and Herd list: pull triggers sync or refresh. |
| **Loading / skeleton states** | Done | Animal detail shows “Loading...” only; Animal detail and herd list show skeletons while loading. |
| **Micro-interactions / feedback** | Done | Snackbars after save/delete (add/edit/delete animal, settings, herds, restore). |
| **Empty states** | Done | Herd list: icon + "Register animal" or "Clear search" CTA; Alerts (Breeding): icon + "View herd (Profiles)" CTA. |
| **Section containers** | Done | Detail screen: Animal details, Reproduction, Health, Weight each in same Surface/card style (12dp, surfaceVariant). |
| **Bottom sheets** | **Done** | Register animal as bottom sheet on large screens (≥600dp) done; phones keep full-screen. |
| **Dynamic / expressive color** | Fixed palette | Optional: follow system accent (Material You) for secondary actions. |
| **Data viz** | Done | Status/sex breakdown bars; Phase 2: “This year by month” bar charts for calvings and breeding events on web Analytics. |
| **Gesture navigation** | Done | Swipe-back is system; swipe-to-delete on herd list (swipe left to remove animal). |
| **Accessibility** | Improved | Content descriptions on herd, detail, settings, summary, photos; min touch targets via Material. Web: skip link (“Skip to main content”) moves focus to main; keyboard tab 1–5 switch tabs; focus visible styles. |

---

## 2. Layout Improvements (Applied or Recommended)

- **Herd screen:** Summary strip (“X head”, “Y due soon”), search, filters, then list; FAB “Register animal”.
- **Animal detail:** Clear section order – Photos → Animal details (card) → Reproduction (card) → Health & treatments (card).
- **Reproduction screen:** List of gestation cards; empty state with one primary CTA line.
- **Farm profile:** Grouped fields (Farm info, Contacts, Calving & reminders) with section titles. Multiple contacts (name, phone, email) supported on web and Android.
- **Consistent spacing:** 16dp screen padding, 12dp between sections, 8dp between list items.

---

## 3. Functionality Gaps

### vs MVP (P0 / P1)

| Feature | MVP | Status | Notes |
|---------|-----|--------|-------|
| User auth | P0 | **Done** | Email/password sign-in (Firebase Auth); app gated on auth. Single user per device. |
| Cloud sync | P0 | **Done** | Firestore sync when online; sync status strip (Synced / Syncing / Saved offline / Sync failed) at top of Home, Herd list, Analytics, Settings; "Sync now" in Settings. |
| Farm profile | P0 | Done | Name, address, multiple contacts (name, phone, email), calving/pregnancy reminders. Editable on web (Settings → Farm & sync → Edit farm) and in app; syncs to all devices. |
| Animal profiles + photos | P0 | Done | CRUD, photos with angle. |
| Breeding + gestation + calving | P0 | Done | Service date, due date, record calving, link calf. |
| Basic alerts | P0 | Done | Calving window, pregnancy check due (Alerts screen with filters). |
| Herd list & search | P0 | Done | Search, status filter. |
| CSV export | P1 | Done | Export herd to CSV. |
| Geo-tag on photos | P0 | **Done** | Capture flow stores latitude/longitude when location available. |

### vs benchmarks (My Cattle Manager, Cattlytics)

| Area | Gap | Priority |
|------|-----|----------|
| **Weight** | Weight & weaning section on animal detail; log weight (date, kg, note); **edit/update** weight and health records (CRUD). | Done |
| **Milk** | No milk production (dairy) | Medium if targeting dairy |
| **Reports** | In-app: herd summary + breeding/calving metrics; CSV export; PDF herd export (Android); PDF report (web Analytics). No Excel yet | Medium |
| **Multi-device sync** | Firestore sync; multiple devices per account | **Done** |
| **Auth** | Email/password (Firebase); single user per device | **Done** |
| **Pedigree** | ~~No sire/dam or family tree~~ **Phase 2 done** | Sire/dam on animal (Android: Animal detail shows Sire/Dam; Edit animal has Pedigree section to set sire/dam; new calves get damId). Web: Profiles animal detail shows Sire/Dam when synced. Firestore and API schema include sireId/damId. |
| **Tasks / reminders** | Only “due soon” banners; no task list or reminders | Medium |

### Precision livestock farming (PLF)

- **Individual records:** Strong (animal-level profile, events).
- **Alerts:** Calving, pregnancy check, and withdrawal-period end on web and Android (from health_events with withdrawalPeriodEnd). **Weaning weight due** on Android: Alerts tab and Home “due soon” show calves whose weaning date (DOB + farm weaning age) is in the next 14 days or up to 30 days overdue, with no weight recorded in the window; farm setting “Weaning age (days)” 150–300 (default 200).
- **Performance metrics:** No growth rate, weaning weight, or indexes.
- **Sensors / EID:** No RFID/EID reader integration; ear tag is manual.
- **Data export:** CSV and PDF (herd list on Android; Analytics report on web). API spec (OpenAPI 3) served at `GET /api/spec` and linked in web Settings → About for third-party integration.

---

## 4. Suggested Roadmap (Concise)

1. ~~**MVP closure**~~ **Done.** Auth (email/password), cloud sync (Firestore), geo-tag on photos.

2. **UI polish (short term)**  
   Loading/skeleton states, section cards and dividers, empty-state CTA, optional toasts. Pull-to-refresh on Home, Analytics, Breeding, and Herd list done.

3. ~~**Competitive features (core)**~~  
   Weight/weaning, backup/restore done. Simple reports (e.g. breeding/calving summary) still optional.

4. ~~**Web dashboard – real data**~~ **Done.** Firebase Auth + Firestore; dashboard and Profiles show synced herd data. Clicking an animal on Profiles loads full detail (breed, DOB, herd, breeding/calving/health events, photo count). See [NEXT-STEPS.md](NEXT-STEPS.md).

5. **Phase 2**  
   ~~**Pedigree UI**~~ **Done:** Sire and dam on animal (optional). Android: Animal detail shows Sire/Dam; Edit animal has Pedigree (optional) to set sire/dam; new calves get damId. Web: Profiles animal detail shows Sire/Dam when from Firestore. Data model: sireId/damId on Animal; sync, backup, OpenAPI and shared schema updated.  
   **Weaning weight reminders:** Done on Android (Alerts + Home due soon; farm setting weaning age 150–300 days). Optional milk (dairy), more data viz later.

6. **Later**  
   Multi-user, finances, EID/sensors.

---

## 5. References

- [Material Design – Material 3](https://m3.material.io/)
- [My Cattle Manager – Bivatec](https://www.bivatec.com/apps/my-cattle-manager)
- [Cattlytics – Cattle Management Software](https://www.cattlytics.com/cattle-management-software/)
- MVP scope: `docs/architecture/MVP-DEFINITION.md`
