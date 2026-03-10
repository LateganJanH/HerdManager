# Versioned API (v1)

The HerdManager web dashboard exposes a versioned REST API under **`/api/v1`** for third-party or mobile-backend use. The base path is fixed so you can add new endpoints without breaking existing clients.

## Discovery

- **GET /api/v1** – Returns the API version and links to endpoints. Use this to discover the available paths.
  - Response: `{ version: "v1", description: "...", endpoints: { animals: "/api/v1/animals", ... }, openapi: "/api/spec" }`
  - Cache: `public, max-age=300`

## Endpoints

| Path | Method | Status | Description |
|------|--------|--------|--------------|
| `/api/v1` | GET | 200 | Discovery: version and endpoint links |
| `/api/v1/billing-status` | GET | 200 or 501 | Billing/plan status for the current instance (from server env). Returns 501 when billing is not configured. |
| `/api/v1/animals` | GET | 200, 401, 503 | **Read-only list.** Requires `Authorization: Bearer <Firebase ID token>`. Returns `{ animals: [...] }` for the authenticated user. 401 when missing/invalid token; 503 when Firebase Admin is not configured. |
| `/api/v1/breeding-events` | GET | 200, 401, 503 | **Read-only list.** Requires `Authorization: Bearer <Firebase ID token>`. Returns `{ breedingEvents: [...] }` for the authenticated user. 401 when missing/invalid token; 503 when Firebase Admin is not configured. |

## Billing status (implemented)

When the instance has billing env set (`BILLING_PLAN`, `BILLING_STATUS`, `BILLING_CUSTOMER_ID`), **GET /api/v1/billing-status** returns:

```json
{
  "solutionId": "<instance-id>",
  "plan": "basic",
  "billingStatus": "active",
  "billingCustomerId": "cus_xxx"
}
```

When billing is not configured, the response is **501 Not Implemented** with an error body. See [BILLING-IMPLEMENTATION.md](BILLING-IMPLEMENTATION.md) and `web/.env.example`.

## OpenAPI

The full API spec (including unversioned dashboard routes and v1 paths) is at **GET /api/spec** (OpenAPI 3 YAML). The spec is also in `shared/api/openapi.yaml` and copied to `web/public/openapi.yaml` at build.

## Animals (read-only)

**GET /api/v1/animals** returns the authenticated user’s animals from Firestore. Set `Authorization: Bearer <Firebase ID token>` (obtain from Firebase Auth on the client). Response shape: `{ animals: Array<{ id, earTagNumber, sex, breed, dateOfBirth, farmId, status, name?, rfid?, coatColor?, hornStatus?, sireId?, damId?, isCastrated }> }`. Requires `FIREBASE_SERVICE_ACCOUNT_JSON` to be set on the server; otherwise the endpoint returns 503.

## Breeding events (read-only)

**GET /api/v1/breeding-events** returns the authenticated user’s breeding events from Firestore. Set `Authorization: Bearer <Firebase ID token>`. Response: `{ breedingEvents: Array<{ id, animalId, sireIds, eventType, serviceDate, notes, pregnancyCheckDateEpochDay?, pregnancyCheckDate?, pregnancyCheckResult?, createdAt?, updatedAt? }> }`. Requires `FIREBASE_SERVICE_ACCOUNT_JSON` on the server; otherwise returns 503.

## Phase Later

- **CRUD:** Optional create/update/delete for animals or breeding-events when needed for integrations.
- **Auth:** Other auth methods (e.g. API keys) may be added in a future iteration.

See [PHASE-LATER-ROADMAP.md](PHASE-LATER-ROADMAP.md) §5.1.

---

## GraphQL (optional)

**POST /api/graphql** – GraphQL Yoga endpoint with schema:

- **Query.animals** – Returns the authenticated user’s animals (same data as GET /api/v1/animals).
- **Query.breedingEvents** – Returns the authenticated user’s breeding events (same data as GET /api/v1/breeding-events).

**Auth:** Send `Authorization: Bearer <Firebase ID token>` in the request header. Unauthenticated requests receive a GraphQL error: `Unauthorized: provide Authorization Bearer <Firebase ID token>`.

**Example query:**

```graphql
query {
  animals { id earTagNumber sex breed status dateOfBirth }
  breedingEvents { id animalId eventType serviceDate pregnancyCheckResult }
}
```

**Server:** Requires `FIREBASE_SERVICE_ACCOUNT_JSON`; otherwise resolvers throw a service-unavailable error.
