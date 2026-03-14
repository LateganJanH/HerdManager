# HerdManager: Modern UI Trends & Functionality Gaps

## 1. Modern UI / Layout Trends (2024–2025)

### Current alignment
- **Material 3** – Using Material 3 theming, color roles, typography, shapes.
- **Rounded corners** – 12dp cards and inputs; consistent shape scale (`UiDefaults.CardShape`, `CardInnerPadding` used on Herd list, Alerts, Home).
- **Branding** – Ferdinand-inspired bull logo used on Android (HerdManager branding tile above hero, launcher icon) and web (header, favicon); single asset style for recognition across platforms. Android Home: branding tile shows logo + "HerdManager"; "Your herd at a glance" card shows farm name (from Farm settings) then title and subtitle.
- **Clear hierarchy** – Title/subtitle in app bar, summary strip, section headings.
- **Offline-first** – Data in Room; no network dependency for core flows.
- **Dashboard-style metrics** – Herd count and “due soon” in app bar and summary strip. **Home “due soon” card** shows the first few due items (ear tag + calving, pregnancy check, withdrawal end, or weaning weight due) so the user sees who is due at a glance (Android and web). **Home “Attention needed” card** shows at-risk items: open cows/heifers with no breeding in 60 days, calves with weaning weight overdue; tap opens Alerts or animal detail. **Growth & weaning KPIs:** Android animal detail shows recent growth summary (latest weight and average daily gain), and herd analytics (Android + web) include average daily gain and average weaning weight; headline “head” counts deliberately exclude animals with status SOLD, DECEASED, or CULLED. **Body condition (BCS):** Android animal detail has a Condition section (score history, add/edit/delete); Analytics shows average BCS for the current herd and an optional BCS distribution (count per score 1–9).

### Gaps vs current trends

| Trend | Status | Recommendation |
|-------|--------|----------------|
| **Pull-to-refresh** | Done | Home, Analytics, Breeding, and Herd list: pull triggers sync or refresh. |
| **Loading / skeleton states** | Done | Android: animal detail and herd list show skeletons. Web: Profiles list, Alerts list, Home status cards, and Analytics show loading skeletons. |
| **Micro-interactions / feedback** | Done | Snackbars after save/delete (add/edit/delete animal, settings, herds, restore). |
| **Empty states** | Done | Herd list: icon + "Register animal" or "Clear search" CTA; Alerts (Breeding): icon + "View herd (Profiles)" CTA. |
| **Section containers** | Done | Detail screen: Animal details, Reproduction, Health, Condition (BCS), Weight each in same Surface/card style (12dp, surfaceVariant). |
| **Bottom sheets** | **Done** | Register animal as bottom sheet on large screens (≥600dp) done; phones keep full-screen. |
| **Dynamic / expressive color** | Done | Android 12+: Material You dynamic color; primary stays HerdManager green; `HerdManagerTheme(dynamicColor = true)` uses system accent for surfaces/secondary when available. |
| **Data viz** | Done | Status/sex breakdown bars; Phase 2: “This year by month” bar charts for calvings and breeding events on web Analytics. |
| **Gesture navigation** | Done | Swipe-back is system; swipe-to-delete on herd list (swipe left to remove animal). |
| **Accessibility** | Improved | Content descriptions on herd, detail, settings, summary, photos; min touch targets via Material. Web: skip link (“Skip to main content”) moves focus to main; keyboard tab 1–5 switch tabs; focus visible styles. |

---

## 2. Layout Improvements (Applied or Recommended)

- **Herd screen:** Summary strip (“X head”, “Y due soon”), search, filters, then list; FAB “Register animal”.
- **Animal detail:** Clear section order – Photos → Animal details (card) → Reproduction (card) → Health & treatments (card) → **Condition (BCS)** (card) → Weight (card). Condition section lists body condition score history; “Record score” opens a dialog (date picker, score 1–9, optional notes); edit/delete per record. **Web:** Profiles animal detail includes Weight & weaning (list, Log weight, Edit, Delete) with Firestore CRUD.
- **Reproduction screen:** List of gestation cards; empty state with one primary CTA line.
- **Transactions & expenses:** Top app bar title, sync status strip, **Sales / Purchases / Expenses** tabs, totals card, and list/empty-state content. Expenses tab shows horizontal category filter chips and a **Manage categories** action; delete uses a confirmation dialog.
- **Tasks & reminders:** Tasks screen (from Home) has top app bar “Tasks & reminders”, back navigation; summary row (Open / Due today / Overdue); horizontal status filter chips (All, Pending, In progress, Done); FAB to add task; list of task cards (tap to edit, long-press to delete with confirmation); add/edit dialog (title, notes, due date, optional animal link, status when editing); empty state “No tasks yet” with CTA to tap + to add. **Web:** Tasks tab (nav + shortcut 6): full CRUD, status filters, summary row, Add task, Edit/Delete per task; Home card links to Tasks tab.
- **Farm profile:** Grouped fields (Farm info, Contacts, Calving & reminders) with section titles. Multiple contacts (name, phone, email) supported on web and Android. On Android, the Settings (Farm profile) screen uses a horizontal tab row (Farm, Operations, Herds, Sync, System, Data, About) via `HorizontalFilterChips`; scrolling is handled only inside the chip row to avoid nested horizontal-scroll crashes.
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
| **Reports** | In-app: herd summary + breeding/calving metrics; CSV export; PDF herd export (Android); PDF and **Excel** report (web Analytics). | Medium |
| **Multi-device sync** | Firestore sync; multiple devices per account | **Done** |
| **Auth** | Email/password (Firebase); single user per device | **Done** |
| **Pedigree** | ~~No sire/dam or family tree~~ **Phase 2 done** | Sire/dam on animal (Android: Animal detail shows Sire/Dam; Edit animal has Pedigree section to set sire/dam; new calves get damId). Web: Profiles animal detail shows Sire/Dam when synced. Firestore and API schema include sireId/damId. |
| **Tasks / reminders** | **Android:** Farm-wide task board: Tasks screen from Home; summary (open / due today / overdue); status filter chips (All, Pending, In progress, Done); add/edit tasks (title, notes, due date, optional animal link, status in edit); status values PENDING, IN_PROGRESS, DONE, CANCELLED; long-press delete with confirmation; backup/restore and sync include tasks. **Web:** Tasks tab (nav + shortcut 6): full CRUD (add, edit, delete), status filters, summary row, optional due date and link to animal; Home card links to Tasks tab. | Done (Android); Done (web) |

### Precision livestock farming (PLF)

- **Individual records:** Strong (animal-level profile, events).
- **Alerts:** Calving, pregnancy check, withdrawal-period end, and **weaning weight due** on web and Android. Weaning weight due: Alerts tab and Home “due soon” show calves whose weaning date (DOB + farm weaning age) is in the next 14 days or up to 30 days overdue, with no weight recorded in the window; farm setting “Weaning age (days)” 150–300 (default 200). Web: Settings → Edit farm includes weaning age; Alerts and Home use it for weaning-weight-due items from Firestore.
- **Performance metrics:** Per-animal growth summary (latest weight, gain over last interval, average daily gain) on Android animal detail; herd-level average daily gain and average weaning weight on Android and web Analytics. **Body condition scoring (BCS):** Android animal detail has a Condition section (list of scores, add/edit/delete with date, score 1–9, notes); herd Analytics shows average BCS for the current herd and an optional BCS distribution (count per score 1–9); condition records sync and are included in backup/restore. Headline herd-size metrics (`X head`) and due-soon/analytics use “current herd” (exclude SOLD, DECEASED, CULLED) so counts and totals add up.
- **Sensors / EID:** No RFID/EID reader integration; ear tag is manual. **Voice input** and **ML Kit text recognition** are implemented on Android: mic button on ear tag/breed (Register & Edit animal); text in added photos is detected and shown in a snackbar on animal detail.
- **Data export:** CSV and PDF (herd list on Android; Analytics report on web). API spec (OpenAPI 3) at `GET /api/spec`; web Settings → About opens it in an in-app modal (Close returns to dashboard) with "Open in new tab" for raw YAML. **Multi-instance:** When `NEXT_PUBLIC_SOLUTION_ID` and `NEXT_PUBLIC_SUPPORT_URL` are set, About shows instance ID and Help & support / Suggest a feature / Report a problem links (Android: same when BuildConfig is set). See [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) §5.

---

## 4. Suggested Roadmap (Concise)

1. ~~**MVP closure**~~ **Done.** Auth (email/password), cloud sync (Firestore), geo-tag on photos.

2. **UI polish (short term)**  
   Loading/skeleton states, section cards and dividers, empty-state CTA, optional toasts. Pull-to-refresh on Home, Analytics, Breeding, and Herd list done.

3. ~~**Competitive features (core)**~~  
   Weight/weaning, backup/restore done. Simple reports (e.g. breeding/calving summary) still optional.

4. ~~**Web dashboard – real data**~~ **Done.** Firebase Auth + Firestore; dashboard and Profiles show synced herd data. Clicking an animal on Profiles loads full detail (breed, DOB, herd, breeding/calving/health events, photo count). **API:** `GET /api/stats` and `GET /api/devices` return real data when the client sends `Authorization: Bearer <Firebase ID token>` and Firebase Admin is configured (see [NEXT-STEPS.md](NEXT-STEPS.md) §1.1 Option A).

5. **Phase 2**  
   ~~**Pedigree UI**~~ **Done:** Sire and dam on animal (optional). Android: Animal detail shows Sire/Dam; Edit animal has Pedigree (optional) to set sire/dam; new calves get damId. Web: Profiles animal detail shows Sire/Dam when from Firestore. Data model: sireId/damId on Animal; sync, backup, OpenAPI and shared schema updated.  
   **Weaning weight reminders:** Done on Android and web (Alerts + Home due soon; farm setting weaning age 150–300 days). Optional milk (dairy), more data viz later.

6. **Later**  
   Multi-user, finances, EID/sensors.

---

## 5. References

- [Material Design – Material 3](https://m3.material.io/)
- [My Cattle Manager – Bivatec](https://www.bivatec.com/apps/my-cattle-manager)
- [Cattlytics – Cattle Management Software](https://www.cattlytics.com/cattle-management-software/)
- MVP scope: `docs/architecture/MVP-DEFINITION.md`


