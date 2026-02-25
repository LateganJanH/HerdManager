# Authentication and identity: current state and target

**Audience:** Solution architect. This doc defines the **HerdManager Solution** boundary, describes why the current Firebase end-user login is a poor fit for security and RBAC, and outlines a **target** where the solution authenticates as a service and **people** (users) are a separate identity layer with roles.

---

## 1. HerdManager Solution (one instance)

**HerdManager Solution** is the term for **one sold instance** — the full stack that serves a single farm (customer):

| Component | Description |
|-----------|-------------|
| **Firebase project** | Auth, Firestore, Storage, Cloud Functions for that farm. |
| **Web dashboard** | Deployed web app (today: Next.js); may become more feature-rich (reporting, config, central management). |
| **Field client(s)** | Today: Android field app; future: other platform field apps (e.g. iOS), possibly dedicated devices. |
| **IoT / precision farming** | Future: sensors, EID readers, scales, or other devices that integrate with the solution. |

So: **one HerdManager Solution** = one Firebase project + one web dashboard + field app(s) + (future) IoT. One solution = one farm instance. See [INSTANCE-PER-FARM-STRATEGY.md](INSTANCE-PER-FARM-STRATEGY.md).

---

## 2. Current approach: Firebase Auth as end-user login

Today, **people** (farm staff) sign in to the field app and web with **Firebase Authentication** (e.g. email/password). The same Firebase UID is used across devices: “John” logs in on phone and tablet with the same account; sync and data are keyed by that UID (`users/{uid}/...`).

**Issues with this for a sold solution and future RBAC:**

1. **Conflates “who is the person” with “which solution/device.”** Firebase Auth is being used both as “this is the HerdManager instance” and “this is John.” For a **solution** sold per farm, it would be cleaner if the **solution (or device)** authenticates to the cloud as a **machine/service identity**, and **people** (John, Jane, the vet) are a separate notion with roles.
2. **Weak basis for RBAC.** Role-based access control (e.g. Admin, Manager, Worker, Vet) implies multiple **users** (people) with different permissions. If every device logs in as “the same” Firebase user (or one user per person but with no role model), you don’t have a clear place to attach roles and enforce “only Admins can delete animals” or “Workers can only add events.” Firebase Auth gives identity; it does not, by itself, give you a farm-level user store with roles.
3. **Security and audit.** You want “this action was done by John (Worker) on device X” — i.e. **solution identity** (device/app) + **user identity** (person + role). Tying everything to a single Firebase UID per person spreads “person” and “device” across the same channel and makes it harder to introduce proper user management and RBAC later.

So: **using the same Firebase end-user login across the app instance defies the login security purpose** when the solution is sold per farm and RBAC is in scope. It would be better to treat **Firebase (or backend) access as a HerdManager Solution service account** and keep **user** login and RBAC as a separate layer.

---

## 3. Target: solution service account + separate user identity and RBAC

**Direction:** The **HerdManager Solution** (field app, web, future IoT) authenticates to the Firebase project (or to a backend that fronts Firebase) as a **solution/service account** — a **machine identity** for that instance, not a human. Then **people** (farm staff) are modeled and authenticated **inside** the solution, with roles and permissions (RBAC).

### 3.1 Two possible shapes

**Option A – Firebase with solution credential + app-level user store**

- **Solution → Firebase:** Field app and web use a **single instance credential** (e.g. Firebase service account used from a backend, or a long-lived token / API key scoped to that project) so that all access to Firestore/Auth/Storage is “on behalf of the solution,” not on behalf of a human Firebase user.
- **Users (people):** Stored in the solution (e.g. Firestore `solution_users` or a small backend). Each person has a **user identity** (e.g. email + password, or link to an IdP) and a **role** (Admin, Manager, Worker, Vet). The field app and web implement **login** against this user store (or a thin auth service) and then pass the **current user id + role** with requests or store it in session. Firestore rules (or a backend) enforce RBAC based on that user and role.
- **Audit:** Logs and writes can record “solution X, user Y (role), action Z.”

**Option B – Backend-for-Frontend (BFF) / API layer**

- **Solution → Backend:** Field app and web call **your backend** (or Cloud Functions) using a **solution credential** (e.g. instance API key or service account). No direct client-side Firebase Auth for “users”; Firebase is accessed only from the backend (or from the client with a solution-level token).
- **Users (people):** Backend (or a dedicated auth service) holds user accounts and roles. Users log in (e.g. email/password, OAuth) **to the backend**; backend issues a session or JWT that carries `userId` and `role`. All access to data goes through the backend, which enforces RBAC and writes to Firestore (e.g. with Admin SDK / service account).
- **Audit:** Backend logs “solution X, user Y (role), action Z.”

In both cases, **Firebase project login is effectively a HerdManager Solution service account** (or equivalent machine identity), and **human user login and RBAC** are a separate, explicit layer.

### 3.2 What changes from today

- **Today:** Person signs in with Firebase Auth (email/password); `users/{uid}/...` is that person’s data; no notion of “solution” vs “user” or roles.
- **Target:** Solution (app/device) authenticates as service/instance; **users** are a separate store with roles; data access and audit are “solution + user (role).” Migration would introduce the user store and RBAC, then switch clients to solution credential + user login; existing `users/{uid}` data could be mapped to the new user model (e.g. one “primary” user per migrated account) or restructured.

---

## 4. Summary

| Concept | Current | Target |
|--------|---------|--------|
| **HerdManager Solution** | (Not named.) | One instance = Firebase project + web dashboard + field app(s) + (future) IoT. |
| **Who authenticates to Firebase?** | End-user (person) via Firebase Auth. | **Solution** (service account / instance credential); optionally via BFF. |
| **Who is the “user”?** | Same as Firebase identity (one uid per person). | **Separate** user store (people with roles); login and RBAC in app or backend. |
| **RBAC** | Not modeled. | Roles (e.g. Admin, Manager, Worker, Vet) on users; enforced in rules or backend. |
| **Audit** | “uid did X.” | “Solution X, user Y (role), action Z.” |

This document is the **target design** for authentication and identity once RBAC and clearer separation of solution vs. user identity are adopted. Current implementation remains Firebase end-user login until that migration.
