# HerdManager: Feature Objectives Review (Master Architect)

**Purpose:** Map stated product objectives to current implementation and identify missing or partial functionality for enhancement. Covers: (1) industry best practices (Cattlytics, My Cattle Manager), Cargill/Penn State AI, PLF, native capabilities; (2) **extended objectives**: multi-lingual speech/text and default language in settings; accurate identification (ID tags, branding/marking, photos, face/nose print AI); comprehensive datasheets; condition scoring; custom immunization and feed schedules; inventory and sustainability; Android 10+, offline, multi-platform; (3) **meat price & sales**: best-practice meat price tracking, market price indicators, and extending animals sold with sold weight (avg) and per-kilogram meat price; (4) **core design principles**: Individual-Animal First, Photo-Centric Identification, Offline-First, Scientific Accuracy, Low Friction Data Capture, Explainable AI.

---

## 1. Objective vs Current State

### 1.1 Industry Best Practices (Cattlytics, My Cattle Manager)

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Individual animal profiles** | ✅ Strong. CRUD, ear tag, breed, DOB, sex, status, photos, pedigree (sire/dam), weight, health events. | — |
| **Biological events** | ✅ Breeding, gestation, calving, pregnancy check, health (vaccination, treatment, disease, withdrawal, castration). | Optional: breeding method (natural vs AI) more prominent; embryo transfer as first-class type. |
| **Operational events** | ✅ Transactions (sales, purchases, expenses), expense categories, herd assignments. | Optional: tasks/work orders; inventory (meds, feed) if targeting mixed operations. |
| **Reproductive tracking** | ✅ Service date, due date (configurable gestation), record calving, link calf, sire selection. | — |
| **Alerts & reminders** | ✅ Calving window, pregnancy check due, withdrawal end, weaning weight due (Alerts tab + Home “due soon”). | **Gap:** No task list or persistent reminders; no SMS/email/push. In-app only. |
| **Reports & export** | ✅ CSV herd export; PDF herd (Android); web Analytics (CSV, Excel, PDF); API. | Optional: scheduled reports; more pre-built report templates (e.g. Cattlytics-style summaries). |
| **Multi-device sync** | ✅ Firestore sync; offline-first Android; web dashboard with real data. | — |

**Summary:** Core “best practices” from Cattlytics/My Cattle Manager are largely met. Main gaps: **no push/SMS/email alerts**, **no formal task/reminder list**, and **no milk/dairy** (if targeting dairy).

---

### 1.2 Android Native Capabilities

| Capability | Objective | Current State | Gap / Enhancement |
|------------|-----------|---------------|-------------------|
| **Camera** | Photo recognition, real-time data entry | ✅ Camera + gallery for animal photos; angles (left/right/face/rear); geo-tag (lat/long) on capture. | **Partial:** ML Kit **text** recognition only (ear tag photo → copy text). **Gap:** No **animal/visual recognition** (e.g. identify animal from photo, body condition score from image). |
| **GPS / location** | Location tracking | ✅ Geo-tag on **photos** (last known location at capture). | **Gap:** No **animal-level** or **event-level** location (e.g. pasture/paddock, grazing location over time). No map view of animals or resources. |
| **Microphone** | Voice input for real-time data entry | ✅ **Done.** System speech recognizer; mic on ear tag & breed (Register/Edit animal) and herd list search. | — |
| **Real-time monitoring & alerts** | Real-time data entry, monitoring, alerts | ✅ Real-time in-app: due-soon list, Alerts tab, sync status. | **Gap:** No **push notifications** or **background alerts**; no “real-time” external (SMS/email) or wearable. |

**Summary:** Camera and voice are well used; GPS is used only for photo geo-tag. Missing: **photo-based animal/BCS recognition**, **location tracking per animal/event**, **push/background alerts**.

---

### 1.3 AI: Predictive Insights, Analytics, Recommendations

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Predictive insights** | None. No forecasting (e.g. calving risk, next heat, growth). | **Gap:** Add predictive layer: e.g. calving date distribution, “at-risk” animals (no pregnancy check, overdue weaning), simple growth/ADG from weight. |
| **Analytics** | ✅ Herd stats (counts, status, sex, category); breeding/calving by month; charts; CSV/Excel/PDF export. | **Gap:** No **growth rate**, **weaning weight indexes**, **reproductive KPIs** (e.g. conception rate, calving interval). Optional: benchmarks vs herd or industry. |
| **Automated recommendations** | None. Alerts are rule-based (due dates, withdrawal, weaning). | **Gap:** No “recommendations” engine: e.g. “Schedule pregnancy check for X”, “Consider culling Y (age/performance)”, “Replenish vaccine Z”. |
| **On-device AI** | ✅ ML Kit text recognition (photo → text for ear tag). | **Gap:** No TensorFlow Lite / custom model for animal ID, BCS, or condition scoring from photos. |

**Summary:** Analytics are descriptive only. Missing: **predictive models**, **performance indexes**, **recommendation engine**, and **advanced on-device vision** (beyond text).

---

### 1.4 Precision Livestock Farming (PLF)

| PLF Element | Objective | Current State | Gap / Enhancement |
|-------------|-----------|---------------|-------------------|
| **Health monitoring** | AI analyzes data for health monitoring | ✅ Health events (vaccination, treatment, disease, withdrawal); alerts for withdrawal end. | **Gap:** No **automated** health analysis (e.g. illness risk from patterns); no integration with **sensors/vitals** (temperature, activity). |
| **Disease prevention** | Disease prevention | ✅ Record disease events and treatments; withdrawal tracking. | **Gap:** No **outbreak/contact** logic; no **biosecurity** workflows (quarantine, movement history); no **predictive** disease risk. |
| **Resource optimization** | Resource optimization | ✅ Transactions (sales/purchases/expenses); herd and event data. | **Gap:** No **feed/forage** tracking; no **pasture/paddock** or **stocking rate**; no **labor** or **equipment** utilization. |
| **Individual records** | Per-animal data for PLF | ✅ Strong per-animal profile and event history. | — |
| **Sensors / EID** | IoT, RFID/EID | Not implemented. | **Gap:** No RFID/EID reader; no sensor ingestion (e.g. scales, activity collars). Architecture doc mentions future IoT/Cloud Functions. |

**Summary:** PLF foundations (individual records, health events, alerts) exist. Missing: **automated health/disease analytics**, **sensor/EID integration**, **resource optimization** (feed, pasture, labor).

---

### 1.5 Offline & Cross-Platform

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Offline functionality** | ✅ Room DB; full CRUD offline; sync when online; merge-by-document. | — |
| **Cloud sync** | ✅ Firestore; animals, events, photos, herds, settings, transactions, expense categories. | — |
| **Data accessibility across devices and platforms** | ✅ Android app + web dashboard; same Firebase project; real-time listeners on web. | Optional: iOS app (same backend); optional API for third-party tools. |

**Summary:** Offline-first and cross-device sync objectives are **met**.

---

## 2. Prioritized Gaps and Recommended Enhancements

### Tier 1 — High impact, align with stated objectives

1. **Push / background alerts**  
   - **Gap:** Alerts only in-app; no push notifications or scheduled reminders.  
   - **Enhancement:** FCM (Firebase Cloud Messaging) for “due soon” (calving, pregnancy check, weaning, withdrawal); optional daily digest.  
   - **Ref:** MVP explicitly had “in-app only”; objective says “real-time … monitoring and alerts” — push is the next step.

2. **Photo-based animal recognition (beyond text)**  
   - **Gap:** ML Kit used for text only; no “recognize this animal” or BCS from image.  
   - **Enhancement:** Optional TensorFlow Lite (or cloud) model: match photo to existing animal profile, or suggest BCS/condition from image (Cargill/Penn State–style). Start with “suggest animal from photo” using existing photos as reference.

3. **Location beyond photo geo-tag**  
   - **Gap:** No animal or event-level location; no pasture/paddock.  
   - **Enhancement:** Optional “location” or “pasture/paddock” on animal or event; record at event time (e.g. breeding, treatment). Later: map view, movement history.

4. **Predictive insights (first steps)**  
   - **Gap:** No forecasting or risk flags.  
   - **Enhancement:** Derived “at-risk” or “attention” list: e.g. open cows with no breeding in N days; animals with no pregnancy check in window; overdue weaning weight. Surface on Home or Alerts as “Recommended actions”.

### Tier 2 — Differentiators (PLF / AI)

5. **Growth and performance metrics**  
   - **Gap:** No ADG, weaning weight indexes, or reproductive KPIs.  
   - **Enhancement:** ADG from weight over time; weaning weight vs farm average or target; simple KPIs (e.g. conception rate, calving interval) on Analytics.

6. **Automated recommendations**  
   - **Gap:** No proactive “recommendations” beyond due-date alerts.  
   - **Enhancement:** Rule-based “recommendations” (e.g. “Schedule pregnancy check”, “Record weaning weight for calf X”) in a dedicated list or section; later ML-based.

7. **Health/disease analytics**  
   - **Gap:** Health data is recorded but not analyzed for patterns or risk.  
   - **Enhancement:** Simple analytics: treatments by product/period; withdrawal compliance; optional “sick pen” or risk list from event patterns.

8. **EID / RFID (future)**  
   - **Gap:** No reader integration.  
   - **Enhancement:** Document reader API/UX; optional NFC or Bluetooth reader for ear tag → auto-fill animal in forms.

### Tier 3 — Optional / later

9. **SMS/email alerts**  
   - Per objective “real-time … alerts”; currently out of scope. Add when push is stable.

10. **Milk production (dairy)**  
    - Only if targeting dairy segment; not in current data model.

11. **Feed / pasture / resource optimization**  
    - PLF “resource optimization”: feed usage, pasture rotation, stocking rate. New data models and UI.

12. **Sensors / IoT ingestion**  
    - Architecture allows Cloud Functions for external data; add when hardware partners exist.

---

## 3. Summary Table: Objective → Status → Next Step

| Stated objective | Status | Suggested next step |
|------------------|--------|---------------------|
| Best practices (Cattlytics, MCM) | ✅ Largely met | Task/reminder list; optional push/SMS |
| Camera for photo recognition | ⚠️ Text only | Animal match or BCS from photo (TFLite or cloud) |
| GPS for location tracking | ⚠️ Photo geo-tag only | Animal/event location; pasture; optional map |
| Microphone for voice input | ✅ Done | — |
| Real-time monitoring & alerts | ⚠️ In-app only | Push notifications (FCM) |
| Detailed animal profiles | ✅ Done | — |
| Biological & operational events | ✅ Done | Optional tasks/work orders |
| AI predictive insights | ❌ Missing | “At-risk” lists; simple predictions |
| AI analytics | ⚠️ Descriptive only | ADG, indexes, reproductive KPIs |
| AI automated recommendations | ❌ Missing | Rule-based recommendation list |
| PLF health monitoring | ⚠️ Data only | Health analytics; optional sensors |
| PLF disease prevention | ⚠️ Records only | Biosecurity/contact; risk flags |
| PLF resource optimization | ❌ Missing | Feed/pasture later; transactions done |
| Offline + cloud sync | ✅ Done | — |
| Cross-device/platform | ✅ Done | Optional iOS; API for partners |
| **Multi-lingual (speech/text, default language in settings)** | ❌ Missing | Add languageCode to settings; drive voice + future i18n |
| **ID tags, branding, photos, face/nose print AI** | ⚠️ Tags + photos only | Add branding field; add face/nose print recognition |
| **Comprehensive datasheets (heritage, health, breeding, performance)** | ⚠️ Data present, no single view | Datasheet view; add performance metrics (ADG, indexes) |
| **Condition scoring and notes (best practice)** | ❌ Missing | BCS/condition record; optional AI-assisted BCS from photo |
| **Custom immunization & feed schedules (create, schedule, track, alert)** | ❌ Missing | Plan + ScheduleEntry model; Alerts from plan due dates |
| **Inventory management** | ❌ Missing | Inventory entities; stock levels; optional reorder alerts |
| **Sustainability** | ❌ Missing | Optional metrics/reporting later |
| **Android 10+, offline field, multi-platform install/upgrade** | ✅ Done | minSdk 29; offline-first; min-version + in-app update |
| **Meat price tracking; market price indicators; sold weight & R/kg on sales** | ❌ Missing | Extend Transaction with weightKg, pricePerKgCents; add market price reference; analytics R/kg vs market |
| **Core design: Individual-Animal First, Offline-First** | ✅ Met | — |
| **Core design: Photo-Centric, Scientific Accuracy, Low Friction** | ⚠️ Partial | Branding, BCS, face/nose AI; ADG/indexes; AI-assisted BCS (see §4.10) |
| **Explainable AI** | Design guardrail | When adding AI insights, require transparent reasoning/sources |

---

## 4. Extended Objectives (Multi-Language, Identification, Datasheets, PLF Schedules)

*This section maps additional product objectives: multi-lingual operation, identification (ID tags, branding, face/nose print AI), comprehensive datasheets, condition scoring, custom immunization/feed schedules, and platform/UX requirements.*

### 4.1 Multi-Lingual: Speech and Text in Specific Language

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Interpret speech and text in specific language** | Voice input uses **device default locale** only (`Locale.getDefault()` in `VoiceInputButton`); no app-level language selection. | **Gap:** No configurable language for speech recognition; no UI translations. |
| **Default operational language settable in settings** | No language/locale setting in `FarmSettings` or Settings UI. | **Gap:** Add **operational language** (e.g. ISO 639-1) to Farm or app settings; drive speech recognizer locale and (when added) UI locale from it. |

**Enhancement:** (1) Add `languageCode: String` (e.g. `"en"`, `"af"`, `"zu"`) to FarmSettings or a dedicated app-settings store; persist and sync. (2) Use that locale for `RecognizerIntent.EXTRA_LANGUAGE` and for any future text-to-speech. (3) When introducing i18n, load string resources by that locale so labels and messages match the operational language.

---

### 4.2 Accurate, Unique Identification and Tracking

| Method | Objective | Current State | Gap / Enhancement |
|--------|-----------|---------------|-------------------|
| **ID tags** | Primary identification | ✅ **earTagNumber** (mandatory); **rfid** (optional, manual entry). | Optional: EID/RFID **reader** integration (NFC/Bluetooth) to auto-fill from tag. |
| **Branding / marking** | Secondary identification | Not in data model. | **Gap:** Add optional **branding/marking** (e.g. brand ID, fire brand, freeze brand, or free-text description) to Animal (or extended profile) for regions where branding is standard. |
| **Photos** | Visual identification | ✅ Multiple photos per animal; angles (left/right/face/rear); geo-tag; ML Kit **text** recognition on add. | **Gap:** No **face recognition** or **nose print** AI to match photo → animal. |
| **AI recognition** | Face recognition, nose print | Not implemented. | **Gap:** Add **face recognition** (e.g. bovine face matching from profile photos) and/or **nose print** (unique pattern) as optional identification methods: store templates/embeddings, match at capture or lookup. |

**Enhancement:** (1) Add `brandingOrMarking: String?` (or structured brand type + value) to Animal. (2) Design **photo-based identity**: either TFLite/cloud model for face match to existing animals, or nose-print capture + match; link result to animal ID and show confidence. (3) Document identification hierarchy (ear tag → RFID → brand → photo/AI) in data model and UI.

---

### 4.3 Comprehensive Datasheets (Heritage, Health, Breeding, Performance)

| Element | Objective | Current State | Gap / Enhancement |
|---------|-----------|---------------|-------------------|
| **Heritage** | Per-animal lineage | ✅ **sireId**, **damId** (pedigree); animal detail shows Sire/Dam; Edit has Pedigree section. | Optional: **breedComposition**, **inbreedingCoefficient**, **EBVs** (data model doc; not yet in app). |
| **Health** | Health history on datasheet | ✅ Health events (vaccination, treatment, disease, withdrawal, castration) on animal detail; list + add/edit/delete. | Optional: summary block on “datasheet” view (last vaccine, next due if from schedule). |
| **Breeding** | Breeding history on datasheet | ✅ Breeding events, gestation, calving on animal detail; service date, due date, sire, record calving. | — |
| **Performance metrics** | On datasheet | ✅ Weight records; weaning weight due logic. | **Gap:** No **ADG**, **weaning weight index**, or **reproductive KPIs** (calving interval, conception rate) on animal or summary view. |

**Enhancement:** (1) Add a **single “datasheet” view** (print/export or full-screen) that aggregates: identity (ear tag, RFID, brand), pedigree, key events (last breeding, last calving, last health), weight trend, and (when added) performance metrics. (2) Add computed **performance metrics** (ADG, weaning index, calving interval) to domain/API and surface on animal detail and Analytics.

---

### 4.4 Condition Scoring and Notes (Best Practice)

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Best-practice condition scoring** | No BCS or condition score in model. | **Gap:** Add **body condition score** (BCS) or equivalent (e.g. 1–9 scale, or thin/moderate/fat) as a record: either per **HealthEvent** (or new **ConditionRecord**) or as a standalone observation with date. |
| **Notes** | Free-text **notes** on breeding, calving, health, and animal-level in some flows. | Optional: structured **condition notes** (e.g. “ribs visible”, “good cover”) that map to best-practice guides; link to BCS. |

**Enhancement:** (1) Introduce **ConditionRecord** (or extend HealthEvent) with: `animalId`, `date`, `score` (numeric or enum), optional `notes`. (2) Optionally add **AI-assisted BCS** from photo (TFLite/cloud) as suggestion. (3) Expose BCS history on animal datasheet and in analytics (e.g. average BCS by group/time).

---

### 4.5 Automated Tracking and Alerts (Key Events)

| Event | Objective | Current State | Gap / Enhancement |
|-------|-----------|---------------|-------------------|
| **Gestation** | Track and alert | ✅ Breeding → due date; pregnancy check due; Alerts tab + Home “due soon”. | — |
| **Calving** | Track and alert | ✅ Calving window (configurable days); Alerts + Home. | — |
| **Vaccinations** | Track and alert | ✅ Health events (vaccination type); withdrawal end alert. | **Gap:** No **schedule-based** “next vaccination due” from a **plan** (see §4.8). |
| **Health issues** | Track and alert | ✅ Health events (disease, treatment); withdrawal end. | Optional: “sick list” or risk flags from patterns. |

**Summary:** Event-driven alerts (gestation, calving, withdrawal, weaning) are in place. Missing: **schedule-driven** vaccination and treatment reminders from **custom plans**.

---

### 4.6 AI for Decision-Making and Voice-Assisted Interactions

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Predictive analytics** | None. | **Gap:** Add “at-risk” / attention lists; simple forecasts (e.g. calving distribution); performance predictions (see §2 Tier 1–2). |
| **Voice-assisted interactions** | ✅ **Done.** Voice input (mic) on ear tag, breed, herd search; system speech recognizer. | Optional: voice **commands** (“add calf”, “record breeding”) and TTS feedback for hands-free field use. |

---

### 4.7 Real-Time Monitoring, Inventory, Sustainability, UX and Platform

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Real-time monitoring** | ✅ In-app: due-soon, Alerts, sync status. | **Gap:** Push notifications (FCM) for off-app alerts. |
| **Inventory management** | Not implemented. | **Gap:** No **inventory** (vaccines, meds, feed, supplies); no stock levels or “reorder” alerts. Optional for PLF/profitability. |
| **Sustainability** | Not in data model or UI. | **Gap:** No **sustainability** metrics (e.g. carbon, water, land use) or reporting. Optional: tie to herd/feed/land for future reporting. |
| **User-friendly, Android 10+** | ✅ **minSdk 29** = Android 10; Material 3, Compose, clear navigation. | — |
| **Offline field conditions** | ✅ Offline-first; Room; sync when online. | — |
| **Multi-platform install, maintenance, easy upgrade** | ✅ Android + web; min-version gate; in-app update (Play); provisioning scripts; instance-per-farm. | Optional: iOS; clearer staged rollout docs. |

---

### 4.8 Custom Immunization and Feed Schedules/Plans (PLF)

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Create custom immunization schedule/plan** | No schedule or plan entities. Alerts are derived from **recorded** events (e.g. withdrawal end) or fixed rules (calving, pregnancy check, weaning). | **Gap:** No **vaccination/immunization plan** (e.g. “Clostridial at 0, 30, 60 days”; “BVD annually”). Cannot **create, schedule, track, or alert** on custom vaccination plans per animal or group. |
| **Create custom feed schedule/plan** | No feed plans or feed events. | **Gap:** No **feed schedule** or **feeding plan** (e.g. by herd, by stage); no tracking or alerts for “next feed change” or supplement schedule. |
| **Track and alert on plans** | N/A. | **Gap:** Need **schedule** and **plan** entities (e.g. Plan: name, type [vaccination, feed], rules; Schedule: planId, animalId or herdId, start date, next due, recurrence). Alerts generated from “next due” from schedules. |

**Enhancement:** (1) **Data model:** Add **Plan** (or **Protocol**): type (VACCINATION, FEED, CUSTOM), name, description, optional recurrence/interval rules. Add **ScheduleEntry** (or **PlanAssignment**): planId, animalId or herdId, startDate, nextDueDate, optional completion history. (2) **Alerts:** Compute “due” from ScheduleEntry.nextDueDate; show in Alerts tab and Home; optional push. (3) **UI:** Settings or dedicated “Plans” to create/edit plans; assign to animal/herd; “Record completed” to advance next due. (4) Align with **PLF best practices** (e.g. standard vaccination windows, weaning feed protocols) in default templates or docs.

---

### 4.9 Meat Price Tracking and Sales Extension (Best Practice)

| Objective | Current State | Gap / Enhancement |
|-----------|---------------|-------------------|
| **Best-practice meat price tracking** | No dedicated meat-price or live-weight price tracking. Transactions store **amountCents** only (total sale amount). | **Gap:** No **meat price** or **live-weight price** history; no tracking of R/kg (or local currency per kg) over time for comparison or benchmarking. |
| **Price indicators for market meat pricing** | None. | **Gap:** No **market price indicators** (e.g. regional or national avg price per kg, price trend, or reference rate). No UI or data source for “market price” to compare against own sales. |
| **Animals sold: sold weight (avg) and per kg meat price** | Sales have **animalId**, **amountCents**, buyer contact. No weight or per-kg price. | **Gap:** Sale transaction does not store **sold weight (kg)** or **price per kilogram** (cents/kg or display price/kg). Cannot compute or display “avg sold weight” or “R/kg” per sale or across sales. |

**Enhancement:** (1) **Extend Transaction (Sale/Purchase):** Add optional **weightKg: Double?** (sold/purchased live or carcass weight) and **pricePerKgCents: Long?** (price per kg in cents). Derive one from the other when possible (e.g. pricePerKgCents = amountCents / (weightKg * 100) when weight known). **Implementation:** Add columns to `TransactionEntity` and Room migration; add fields to Firestore `transactions` docs and sync/backup; extend domain `Transaction` and Transaction UI (Add/Edit, list, totals). (2) **UI:** Add weight and price/kg to Add/Edit transaction for Sale (and Purchase); show on list and detail; optionally show “avg sold weight” and “avg R/kg” in Transactions analytics or per-animal sale summary. (3) **Meat price tracking (best practice):** Add **MarketPriceIndicator** or **MeatPriceRecord** (date, pricePerKgCents, optional region/category, source) for reference data; allow manual entry or future API feed. (4) **Analytics:** Compare own sale R/kg to farm average or to market indicator when available; show trend or benchmark in Transactions or Analytics tab.

---

### 4.10 Core Design Principles – Alignment Check

| Principle | Objective | Current State | Gap / Enhancement |
|-----------|-----------|---------------|-------------------|
| **Individual-Animal First** | Every cow and bull is a first-class entity | ✅ **Met.** Animal is central entity; all events (breeding, calving, health, weight, photos, transactions) link to animalId. Herd is grouping, not replacement for identity. | — |
| **Photo-Centric Identification** | Visual recognition complements ear tags and brands | ✅ Photos per animal (angles, geo-tag); ML Kit **text** recognition. ⚠️ No branding field; no **face/nose print** AI (see §4.2). | Add branding; add face/nose print recognition to complete “photo-centric” identification. |
| **Offline-First** | Fully functional without signal; auto-sync when online | ✅ **Met.** Room DB; full CRUD offline; sync when online; merge-by-document. | — |
| **Scientific Accuracy** | Dates, cycles, genetics, metrics aligned with veterinary standards | ✅ Dates (epoch day, LocalDate); gestation configurable (250–320 d); breeding/calving/health events. ⚠️ Genetics: sire/dam only (no EBVs/inbreeding in app). ⚠️ Metrics: no BCS, no ADG/weaning index (see §2, §4.3–4.4). | Add BCS; add performance metrics (ADG, indexes); optional EBVs when genetics data available. |
| **Low Friction Data Capture** | Camera, voice, and AI-assisted inputs | ✅ Camera (photos, text-from-photo); voice (ear tag, breed, search); AI-assisted: ML Kit text. ⚠️ No AI-assisted BCS or animal-from-photo (see §2). | Add AI-assisted condition score and/or animal match from photo. |
| **Explainable AI** | Insights must be transparent, not black-box guesses | No AI-derived “insights” yet (predictions, recommendations, or scoring). When added, design for **explainability**: show which data and rules drove the insight (e.g. “Due to no pregnancy check in 60 days”, “R/kg below 30-day farm average”). | **Design guardrail:** Document principle in architecture; require reasoning/sources for any future AI recommendations or scores. |

**Summary:** Individual-animal-first and offline-first are fully aligned. Photo-centric and scientific accuracy are partially met; low-friction capture is strong with voice and text AI. Explainable AI is a **forward guardrail** for when predictive or recommendation features are added.

---

## 5. Consolidated Gap List (Extended Objectives)

| # | Objective area | Gap | Priority |
|---|----------------|-----|----------|
| 1 | Multi-lingual | No app-level language; voice uses device locale only | High |
| 2 | Identification | No branding/marking field; no face/nose print AI | High (AI); Medium (branding) |
| 3 | Datasheets | No single datasheet view; no performance metrics on view | Medium |
| 4 | Condition scoring | No BCS or condition record; no best-practice scale | High |
| 5 | Custom schedules | No immunization or feed plans; no create/schedule/track/alert | High |
| 6 | Inventory | No inventory management | Medium |
| 7 | Sustainability | No sustainability metrics | Low / later |
| 8 | Push alerts | In-app only; no FCM | High (already in §2) |
| 9 | **Meat price & sales** | No sold weight or R/kg on sale; no meat price tracking; no market price indicators | High |
| 10 | **Explainable AI** | No AI insights yet; when added, require transparent reasoning (design guardrail) | Design / future |

---

## 6. Document References

- **MVP & phases:** [MVP-DEFINITION.md](MVP-DEFINITION.md)  
- **Current gaps & UI:** [DESIGN-UI-AND-GAPS.md](../../DESIGN-UI-AND-GAPS.md)  
- **Next steps & roadmap:** [NEXT-STEPS.md](../../NEXT-STEPS.md), [PHASE-LATER-ROADMAP.md](../../PHASE-LATER-ROADMAP.md)  
- **Architecture:** [ARCHITECTURE.md](ARCHITECTURE.md), [DATA-MODEL.md](DATA-MODEL.md)  
- **Android roadmap:** [android/ANDROID-ROADMAP.md](../../../android/ANDROID-ROADMAP.md)
