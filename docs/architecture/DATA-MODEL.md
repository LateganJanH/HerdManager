# HerdManager Data Model

## 1. Core Entities

### Animal

Central entity. Every cow/bull is a first-class record.

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | System-generated, immutable |
| earTagNumber | String | ✓ | Primary human-readable ID |
| rfid | String? | | If NFC scanned |
| name | String? | | Optional |
| sex | Enum | ✓ | MALE, FEMALE |
| breed | String | ✓ | Breed or composite |
| dateOfBirth | LocalDate | ✓ | Exact or estimated |
| farmId | UUID | ✓ | Farm/location |
| coatColor | String? | | Coat color & markings |
| hornStatus | Enum? | POLLED, HORNED, SCURED |
| registrationNumbers | List\<String\>? | | Stud book refs |
| status | Enum | ✓ | ACTIVE, SOLD, DECEASED, CULLED, ACQUIRED_FROM |
| ownershipHistory | List\<OwnershipRecord\>? | | |
| createdAt | Instant | ✓ | |
| updatedAt | Instant | ✓ | |
| syncStatus | Enum | ✓ | SYNCED, PENDING, CONFLICT |

### Photo

Multiple photos per animal, for identification and AI.

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | |
| animalId | UUID | ✓ | FK |
| angle | Enum | ✓ | LEFT_SIDE, RIGHT_SIDE, FACE, REAR |
| uri | String | ✓ | Local or cloud path |
| capturedAt | Instant | ✓ | |
| latitude | Double? | | Geo-tag |
| longitude | Double? | | Geo-tag |
| confidenceScore | Double? | | AI match confidence |

### Pedigree

Sire, dam, ancestry.

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| animalId | UUID | ✓ | FK |
| sireId | UUID? | | Parent |
| damId | UUID? | | Parent |
| breedComposition | Map\<String, Double\>? | | % per breed |
| inbreedingCoefficient | Double? | | Calculated |
| ebvs | Map\<String, Double\>? | | Estimated Breeding Values |

### BreedingEvent

Mating, AI, embryo transfer.

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | |
| animalId | UUID | ✓ | Dam |
| sireId | UUID? | | Bull (if known) |
| eventType | Enum | ✓ | NATURAL, AI, EMBRYO_TRANSFER |
| serviceDate | LocalDate | ✓ | |
| bullAssignmentId | UUID? | | For natural mating |
| notes | String? | | |

### Gestation

Derived from breeding event; drives alerts.

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| breedingEventId | UUID | ✓ | FK |
| dueDate | LocalDate | ✓ | Breed-adjusted (e.g. 283 days) |
| gestationLengthDays | Int | ✓ | Configurable per breed |
| status | Enum | ✓ | CONFIRMED, UNCONFIRMED, LOST |
| pregnancyCheckDue | LocalDate? | | |

### CalvingEvent

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | |
| damId | UUID | ✓ | |
| calfId | UUID? | | Created at calving |
| breedingEventId | UUID | ✓ | FK |
| actualDate | LocalDate | ✓ | |
| actualTime | LocalTime? | | |
| easeOfCalving | Enum? | UNASSISTED, ASSISTED, DIFFICULT |
| assistanceRequired | Boolean | ✓ | |
| calfSex | Enum? | MALE, FEMALE |
| calfWeight | Double? | kg |
| vitality | Enum? | NORMAL, WEAK, STILLBORN |
| notes | String? | |

### HealthEvent

Vaccinations, treatments, diseases.

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | |
| animalId | UUID | ✓ | |
| eventType | Enum | ✓ | VACCINATION, TREATMENT, DISEASE, WITHDRAWAL |
| date | LocalDate | ✓ | |
| product | String? | | Vaccine/medication name |
| dosage | String? | | |
| withdrawalPeriodEnd | LocalDate? | | |
| notes | String? | | |
| vetId | UUID? | | |

### Alert

Configurable alerts for gestation, calving, health.

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | |
| userId | UUID | ✓ | |
| type | Enum | ✓ | GESTATION, CALVING, HEALTH, INVENTORY, CUSTOM |
| entityId | UUID | ✓ | Animal, event, etc. |
| dueDate | LocalDate? | | |
| status | Enum | ✓ | PENDING, SENT, ACKNOWLEDGED |
| deliveryChannels | List\<Enum\> | ✓ | IN_APP, SMS, EMAIL |
| createdAt | Instant | ✓ | |

### Farm

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | |
| name | String | ✓ | |
| location | GeoPoint? | | |
| herdSize | Int? | | |
| settings | FarmSettings | ✓ | Calving window, gestation defaults |

### User

| Field | Type | Mandatory | Notes |
|-------|------|-----------|-------|
| id | UUID | ✓ | Firebase UID |
| email | String? | | |
| displayName | String? | | |
| role | Enum | ✓ | ADMIN, MANAGER, WORKER, VET |
| farmIds | List\<UUID\> | ✓ | Farms user has access to |

## 2. Event-Based Records

Per PRD: "Event-based record system (breeding, health, calving)".

- **BreedingEvent**, **CalvingEvent**, **HealthEvent** are event records
- Queries aggregate events for an animal (e.g. full health history)
- Immutable append-only events support audit trail

## 3. Relationships (Conceptual ER)

```
Farm 1───* Animal
Animal 1───* Photo
Animal 1───1 Pedigree
Animal 1───* BreedingEvent
BreedingEvent 1───1 Gestation
BreedingEvent 1───? CalvingEvent
Animal 1───* CalvingEvent (as dam)
Animal 1───* HealthEvent
User *───* Farm (via FarmMembership with role)
```

## 4. Sync Model

- Each entity has `syncStatus`, `updatedAt`, `createdAt`
- Conflict resolution: last-write-wins with optional merge strategies for lists
- Sync queue: local changes pushed when online; pull on app foreground
