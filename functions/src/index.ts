/**
 * HerdManager Cloud Functions (Option B).
 * Callable functions for herd stats and devices; web can call with auth (no Next.js server needed).
 * Optional: stripeWebhook for billing (Phase Later) – set STRIPE_WEBHOOK_SECRET and STRIPE_SECRET_KEY.
 */

import * as admin from "firebase-admin";
import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import { createStripeWebhookApp } from "./stripeWebhook";
import {
  computeHerdStatsFromDocs,
  toEpochDay,
  type AnimalDoc,
  type BreedingDoc,
  type CalvingDoc,
} from "./aggregateHerdStats";

admin.initializeApp();

const db = admin.firestore();

/** Get herd stats for the authenticated user. Requires Firebase Auth. */
export const getHerdStats = onCall(
  { enforceAppCheck: false },
  async (request): Promise<unknown> => {
    if (!request.auth?.uid) {
      throw new HttpsError("unauthenticated", "Must be signed in to get herd stats.");
    }
    const uid = request.auth.uid;

    const [animalsSnap, breedingSnap, calvingSnap] = await Promise.all([
      db.collection("users").doc(uid).collection("animals").get(),
      db.collection("users").doc(uid).collection("breeding_events").get(),
      db.collection("users").doc(uid).collection("calving_events").get(),
    ]);

    const animals: AnimalDoc[] = animalsSnap.docs.map((doc) => {
      const data = doc.data();
      const dob = toEpochDay(data.dateOfBirth);
      return {
        id: doc.id,
        status: (data.status as string) || "ACTIVE",
        sex: (data.sex as string) || "FEMALE",
        dateOfBirth: dob != null ? dob : null,
        isCastrated: data.isCastrated as boolean | undefined,
      };
    });

    const breeding: BreedingDoc[] = breedingSnap.docs.map((doc) => {
      const data = doc.data();
      const serviceDate = toEpochDay(data.serviceDate);
      return {
        id: doc.id,
        pregnancyCheckResult: data.pregnancyCheckResult as string | undefined,
        serviceDate: serviceDate ?? undefined,
      };
    });

    const calving: CalvingDoc[] = calvingSnap.docs.map((doc) => {
      const data = doc.data();
      const actualDate = toEpochDay(data.actualDate);
      return {
        id: doc.id,
        breedingEventId: data.breedingEventId as string | undefined,
        actualDate: actualDate ?? undefined,
      };
    });

    return computeHerdStatsFromDocs(animals, breeding, calving);
  }
);

/** Get linked devices for the authenticated user. Requires Firebase Auth. */
export const getDevices = onCall(
  { enforceAppCheck: false },
  async (request): Promise<{ devices: Array<{ id: string; name: string; lastSyncAt: number }> }> => {
    if (!request.auth?.uid) {
      throw new HttpsError("unauthenticated", "Must be signed in to get devices.");
    }
    const uid = request.auth.uid;

    const snap = await db
      .collection("users")
      .doc(uid)
      .collection("devices")
      .get();

    const devices = snap.docs.map((doc) => {
      const data = doc.data();
      const lastSyncAt = typeof data.lastSyncAt === "number" ? data.lastSyncAt : 0;
      return {
        id: doc.id,
        name: (data.name as string) ?? doc.id,
        lastSyncAt,
      };
    });

    return { devices };
  }
);

/** Stripe webhook (Phase Later). Set STRIPE_WEBHOOK_SECRET and STRIPE_SECRET_KEY. Uses Express for raw body. */
export const stripeWebhook = onRequest(createStripeWebhookApp());
