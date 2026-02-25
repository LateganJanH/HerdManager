# Sync design: current limitation and target

## Current behaviour (MVP)

- Each device **uploads** its full local state to Firestore, then **downloads** and **replaces** local with whatever is in Firestore.
- **Last writer wins:** the device that syncs last overwrites the cloud; no merge of changes from multiple devices.
- **Consequence:** Multiple people on multiple devices cannot safely work in parallel. Data from one device can overwrite another’s.

See [FIREBASE-SETUP.md](../../android/FIREBASE-SETUP.md) § Firestore sync.

---

## Target: multi-device, same info for everyone

Farming operations typically have:

- **Multiple people** executing and monitoring (field staff, vet, manager).
- **Multiple devices** (phones, tablets) that all need to see the **same** herd and event data.
- **Some data that is more static** and better managed in one place (e.g. farm settings, herd definitions, reference data).

So the target design is:

1. **Shared view of operational data** – Animals, breeding/calving/health events, photos: all devices see the same combined dataset; changes from any device are merged, not overwritten.
2. **Central management of static / config data** – Farm data (name, contact, alert settings), herd list, etc. can be edited in one place (e.g. web or “primary” device) and flow out to devices, or be merged with a clear “source of truth” (e.g. cloud-first for farm settings).

---

## Possible directions (for a future phase)

- **Merge by entity, not full replace**  
  Sync per collection (e.g. animals, breeding_events): upload only changed records (e.g. by `updatedAt` or a sync flag), and merge remote changes into local by document ID. Use **last-updated wins per document** (or explicit conflict resolution) so that different people editing different animals don’t overwrite each other.

- **Central vs field data**  
  - **Central (farm, herds, reference):** Treat Firestore (or web) as source of truth; devices pull and apply. Edits to farm settings / herds could be allowed only on web or one “admin” device.  
  - **Field (animals, events, photos):** Multi-directional merge – each device pushes its new/updated records and pulls others’ changes; merge by ID and timestamp so everyone ends up with the same set.

- **Real-time listeners**  
  Web and (where useful) Android subscribe to Firestore listeners so that as soon one device writes, others see updates without a manual “Sync now”.

- **Conflict handling**  
  When the same record is edited on two devices before sync, define a rule (e.g. newest `updatedAt` wins, or “central wins for farm, newest wins for events”) and optionally surface rare conflicts to the user.

---

## Phase 2a: Timestamps on Firestore (schema readiness for merge)

To support future per-document merge (e.g. last-updated-wins), Firestore documents include `updatedAt` (and `createdAt` where applicable). All uploads now set these fields:

| Collection         | createdAt | updatedAt | Notes |
|--------------------|-----------|-----------|--------|
| animals            | ✓ uploaded | ✓ uploaded | From AnimalEntity. |
| settings/farm      | —         | ✓ uploaded | Set on each upload. |
| herds              | ✓ uploaded | ✓ uploaded | updatedAt = createdAt until HerdEntity has updatedAt. |
| breeding_events    | ✓ uploaded | ✓ uploaded | updatedAt = createdAt until entity has updatedAt. |
| calving_events     | ✓ uploaded | ✓ uploaded | Derived from actualDate (epoch day → ms). |
| health_events      | ✓ uploaded | ✓ uploaded | Derived from date (epoch day → ms). |
| weight_records     | ✓ uploaded | ✓ uploaded | Derived from date (epoch day → ms). |
| herd_assignments   | ✓ uploaded | ✓ uploaded | createdAt = assignedAt, updatedAt = removedAt ?: assignedAt. |
| photos             | ✓ uploaded | ✓ uploaded | createdAt = updatedAt = capturedAt. |

**Phase 2b:** ~~Change download from full replace to merge-by-document-ID using `updatedAt`~~ **Done.** Download merges into local: per document, apply remote if missing locally or if remote is newer (animals: `updatedAt`; herds/breeding: `createdAt`); keep local when local is newer; include local-only docs. Calving, health, weight, herd_assignments, photos: apply all remote (last-writer-wins per doc). Optional later: add `updatedAt` to more Room entities for full timestamp-based merge on all collections.

---

## Summary

| Aspect | Current (MVP) | Target |
|--------|----------------|--------|
| Multiple devices | Last sync overwrites; risk of data loss | Merge so everyone sees same operational data |
| Farm / static data | Same as above | Central source of truth; managed in one place |
| Field data (animals, events) | Same as above | Multi-directional merge; all devices contribute and see combined view |

This is recorded as a **design goal** for a post-MVP phase; implementation would touch Android sync, Firestore schema (e.g. `updatedAt` on documents), and optionally web as the “central” editor for farm data.
