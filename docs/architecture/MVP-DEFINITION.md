# HerdManager MVP Definition

## 1. MVP Scope (Phase 1)

MVP delivers core value: individual animal profiles, offline-first data capture, and basic reproductive tracking with alerts.

### In Scope

| Feature | Description | Priority |
|---------|-------------|----------|
| User auth | Email/Google login, single user per device | P0 |
| Farm profile | Create one farm, basic details | P0 |
| Animal profiles | Create/edit with mandatory fields + photos | P0 |
| Photo capture | Left/right side, face; geo-tagged | P0 |
| Offline storage | Room DB, full CRUD offline | P0 |
| Cloud sync | Firestore sync when online | P0 |
| Breeding events | Record service date, bull (optional) | P0 |
| Gestation tracking | Auto-calc due date (283 days default) | P0 |
| Calving events | Record calving, link calf | P0 |
| Basic alerts | Calving window (configurable), pregnancy check due | P0 |
| Herd list & search | List animals, filter by status, search by tag | P0 |
| Export | CSV export of animals | P1 |

### Out of Scope (MVP)

- AI photo recognition (use manual selection)
- Voice input
- AI chatbot
- Pedigree tree UI
- Health events (vaccinations, treatments)
- Analytics dashboards
- Multi-user roles
- Web dashboard
- SMS/email alerts (in-app only for MVP)

## 2. Phase 2 (Post-MVP)

- AI photo recognition (ML Kit)
- Voice capture (speech-to-text)
- Health management (vaccinations, treatments)
- Pedigree visualization
- Analytics & reporting
- Web dashboard

## 3. Phase 3 (Future)

- AI predictive analytics
- Multi-user with roles
- SMS/email notifications
- Inventory management
- IoT/sensor integration

## 4. Success Criteria (MVP)

1. User can register, create farm, add 50+ animals offline
2. User can capture 2+ photos per animal with geo-tag
3. User can record breeding, see gestation countdown, get calving alerts
4. Data syncs to cloud when online
5. CSV export works for animals
