# HerdManager System Architecture

## 1. Overview

HerdManager is a **multi-platform** cattle management system with an offline-first mobile app and a web dashboard for analytics and reporting. The architecture follows clean architecture principles, domain-driven design, and PRD best practices.

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
├──────────────────────────────┬──────────────────────────────────────────────┤
│      Android App             │           Web Dashboard                       │
│  (Kotlin, Jetpack Compose)   │     (Next.js, TypeScript, React)              │
│  - Offline-first             │     - Analytics & reporting                   │
│  - Camera, GPS, Voice        │     - Herd overview                           │
│  - Photo recognition (ML)    │     - Export/import                           │
└──────────────┬───────────────┴──────────────────┬───────────────────────────┘
               │                                  │
               │  REST / Firestore SDK             │  REST / Firestore SDK
               │  (with offline persistence)       │
               ▼                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BACKEND / CLOUD                                      │
│  Firebase (Auth, Firestore, Storage, Cloud Functions)                        │
│  - User auth (email, Google, phone)                                          │
│  - Firestore for animal/event data                                           │
│  - Cloud Storage for photos                                                  │
│  - Cloud Functions for AI, sync, notifications                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. Multi-Platform Strategy

### Shared Assets

| Asset | Location | Description |
|-------|----------|-------------|
| API contracts | `shared/api/` | OpenAPI spec, request/response schemas |
| Data models | `shared/schemas/` | JSON Schema for Animal, Event, etc. |
| Business rules | Documented in PRD | Gestation periods, alert rules, etc. |

### Platform-Specific

- **Android**: Native Kotlin, Jetpack Compose, Room (local DB), ML Kit, Android Speech APIs
- **Web**: Next.js, React, TanStack Query, Tailwind CSS

### Why Not Kotlin Multiplatform (KMP)?

- PRD specifies Android-first; web has different UX (dashboards vs field capture)
- Android requires device APIs (camera, GPS, mic) not applicable to web
- Separate codebases allow platform-optimized UX and easier certification

## 4. Data Flow

### Offline-First (Android)

```
User Action → ViewModel → UseCase → Repository
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              Local (Room)    Sync Queue      Remote (Firebase)
                    │               │               │
                    └───────► Conflict Resolution ◄─┘
```

### Web (Online-First)

```
User Action → React Query → API Client → Firebase / Backend
```

## 5. Clean Architecture Layers (Android)

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Compose, ViewModels)                         │
├─────────────────────────────────────────────────────────┤
│  Domain Layer (UseCases, Domain Models)                 │
├─────────────────────────────────────────────────────────┤
│  Data Layer (Repositories, Data Sources, DTOs)          │
└─────────────────────────────────────────────────────────┘
```

- **UI**: Compose screens, ViewModels, navigation
- **Domain**: Business logic, use cases, domain entities (no framework deps)
- **Data**: Room, Firebase, WorkManager, file storage

## 6. Key Technical Choices

| Concern | Choice | Rationale |
|---------|--------|-----------|
| Local DB (Android) | Room + SQLite | PRD, offline-first, encrypted |
| Cloud | Firebase | PRD, offline sync, auth, storage |
| UI (Android) | Jetpack Compose | Modern, declarative |
| UI (Web) | React + Next.js | SSR, SEO, dashboards |
| DI (Android) | Hilt | Official, scoping, testing |
| AI | ML Kit, TensorFlow Lite | PRD, on-device, offline |
| Sync | WorkManager + Firestore offline | Conflict resolution, background |

## 7. Security

- **Auth**: Firebase Auth (email, Google, phone)
- **Data**: Encrypted Room DB (EncryptedSharedPreferences for keys)
- **Transport**: HTTPS only
- **Access**: Role-based (admin, manager, worker, vet) per farm
- **Compliance**: POPIA/GDPR-ready (data minimisation, export, delete)

## 8. Non-Functional Targets (from PRD)

- App launch < 2 s
- Photo recognition < 5 s offline
- Support 1,000+ animals without lag
- 99% uptime for cloud features
- Battery-efficient camera & AI usage

## 9. Future Extensibility

- **HerdManager Solution (instance per farm):** One solution = Firebase + web + field app(s) + (future) IoT; provisioning, branding, updates. See [INSTANCE-PER-FARM-STRATEGY.md](INSTANCE-PER-FARM-STRATEGY.md).
- **Auth and identity (target):** Solution service account for Firebase/backend; separate user identity and RBAC. See [AUTH-AND-IDENTITY.md](AUTH-AND-IDENTITY.md).
- **Multi-farm / software updates:** Option A (multi-tenant) only when one customer wants multiple farms in one solution; update practices. See [MULTI-FARM-AND-UPDATES.md](MULTI-FARM-AND-UPDATES.md).
- **iOS**: Separate SwiftUI app, same Firebase backend
- **API Gateway**: Optional REST layer for enterprise integrations
- **IoT/Sensors**: Cloud Functions to ingest external data
- **Genomics API**: Integration point for EBVs, genetic markers
