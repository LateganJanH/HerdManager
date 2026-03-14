/**
 * HerdManager Cloud Functions (Option B).
 * Callable functions for herd stats and devices; web can call with auth (no Next.js server needed).
 * Optional: stripeWebhook for billing (Phase Later) – set STRIPE_WEBHOOK_SECRET and STRIPE_SECRET_KEY.
 */

import * as admin from "firebase-admin";
import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
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

/**
 * Scheduled function: send a generic \"due soon\" notification to each user who
 * has herd alerts coming up and at least one registered device with an FCM token.
 * Runs daily; can be adjusted in Firebase console if needed.
 */
export const sendDueSoonNotifications = onSchedule("every 24 hours", async () => {
  const usersSnap = await db.collection("users").get();

  await Promise.all(
    usersSnap.docs.map(async (userDoc) => {
      const uid = userDoc.id;

      const userRef = db.collection("users").doc(uid);
      const [animalsSnap, breedingSnap, calvingSnap, devicesSnap, weightsSnap, tasksSnap, farmSettingsSnap] =
        await Promise.all([
          userRef.collection("animals").get(),
          userRef.collection("breeding_events").get(),
          userRef.collection("calving_events").get(),
          userRef.collection("devices").get(),
          userRef.collection("weight_records").get().catch(() => ({ docs: [] })),
          userRef.collection("farm_tasks").get().catch(() => ({ docs: [] })),
          userRef.collection("settings").doc("farm").get().catch(() => null as FirebaseFirestore.DocumentSnapshot | null),
        ]);

      if (devicesSnap.empty) {
        return;
      }

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

      const stats = computeHerdStatsFromDocs(animals, breeding, calving);

      // Derive additional alert counts for weaning-weight-due and overdue tasks.
      let weaningWeightDueCount = 0;
      let tasksOverdueCount = 0;

      try {
        const farmSettingsData = farmSettingsSnap && farmSettingsSnap.exists ? farmSettingsSnap.data() : null;
        const DEFAULT_WEANING_AGE_DAYS = 200;
        const WEANING_AGE_DAYS_MIN = 150;
        const WEANING_AGE_DAYS_MAX = 300;
        const WEANING_ALERT_WINDOW_DAYS = 14;
        const WEANING_OVERDUE_DAYS = 30;

        const weaningAgeDaysRaw =
          (farmSettingsData?.weaningAgeDays as number | undefined) ?? DEFAULT_WEANING_AGE_DAYS;
        const weaningAgeDays = Math.max(
          WEANING_AGE_DAYS_MIN,
          Math.min(WEANING_AGE_DAYS_MAX, weaningAgeDaysRaw)
        );

        const today = new Date();
        const todayEpoch = toEpochDay(today) ?? Math.floor(today.getTime() / 86400_000);

        const activeAnimals = animals.filter(
          (a) => a.status !== "SOLD" && a.status !== "DECEASED" && a.status !== "CULLED"
        );
        const weightDatesByAnimal = new Map<string, number[]>();
        weightsSnap.docs.forEach((doc) => {
          const data = doc.data();
          const animalId = data.animalId as string | undefined;
          const dateEpoch = toEpochDay(data.date);
          if (!animalId || dateEpoch == null) return;
          const list = weightDatesByAnimal.get(animalId) ?? [];
          list.push(dateEpoch);
          weightDatesByAnimal.set(animalId, list);
        });

        activeAnimals.forEach((a) => {
          if (a.dateOfBirth == null) return;
          const dobEpoch =
            a.dateOfBirth > 1e12 ? Math.floor(a.dateOfBirth / 86400_000) : Math.floor(a.dateOfBirth);
          const weaningDueEpoch = dobEpoch + weaningAgeDays;
          if (
            weaningDueEpoch < todayEpoch - WEANING_OVERDUE_DAYS ||
            weaningDueEpoch > todayEpoch + WEANING_ALERT_WINDOW_DAYS
          ) {
            return;
          }
          const weightDates = weightDatesByAnimal.get(a.id) ?? [];
          const hasWeightInWindow = weightDates.some(
            (epoch) => epoch >= weaningDueEpoch - WEANING_ALERT_WINDOW_DAYS
          );
          if (!hasWeightInWindow) {
            weaningWeightDueCount++;
          }
        });

        // Overdue tasks: farm_tasks where status is not DONE/CANCELLED and dueDateEpochDay < today.
        const OPEN_STATUSES = new Set(["PENDING", "IN_PROGRESS"]);
        tasksSnap.docs.forEach((doc) => {
          const data = doc.data();
          const status = (data.status as string | undefined) ?? "PENDING";
          const dueEpoch = data.dueDateEpochDay as number | undefined;
          if (!OPEN_STATUSES.has(status)) return;
          if (typeof dueEpoch === "number" && dueEpoch < todayEpoch) {
            tasksOverdueCount++;
          }
        });
      } catch (e) {
        logger.warn("Error computing weaning/tasks counts for notifications", {
          uid,
          error: (e as Error).message,
        });
      }

      if (!stats.dueSoon || stats.dueSoon <= 0) {
        // Even if there are weaning/tasks alerts, keep the main dueSoon notification as gate.
        if (weaningWeightDueCount <= 0 && tasksOverdueCount <= 0) {
          return;
        }
      }

      const tokens: string[] = [];
      devicesSnap.docs.forEach((doc) => {
        const data = doc.data();
        const token = data.fcmToken as string | undefined;
        if (token) tokens.push(token);
      });

      if (tokens.length === 0) {
        return;
      }

      const b = stats.dueSoonBreakdown;
      const parts: string[] = [];
      if (b.calvingDue > 0) parts.push(`${b.calvingDue} calving`);
      if ((b.pregnancyCheckDue ?? 0) > 0) parts.push(`${b.pregnancyCheckDue} pregnancy check`);
      if ((b.withdrawalDue ?? 0) > 0) parts.push(`${b.withdrawalDue} withdrawal`);
      if ((b.weaningDue ?? 0) > 0) parts.push(`${b.weaningDue} weaning`);
      const bodyDetail =
        parts.length > 0
          ? `${parts.join(", ")} due soon.`
          : `You have ${stats.dueSoon} herd alert${stats.dueSoon === 1 ? "" : "s"} due soon in HerdManager.`;

      const baseData: Record<string, string> = {
        type: "dueSoon",
        count: String(stats.dueSoon),
        calvingDue: String(b.calvingDue),
        pregnancyCheckDue: String(b.pregnancyCheckDue ?? 0),
        withdrawalDue: String(b.withdrawalDue ?? 0),
        weaningDue: String(b.weaningDue ?? 0),
      };

      try {
        const res = await admin.messaging().sendEachForMulticast({
          notification: {
            title: "Herd alerts due soon",
            body: bodyDetail,
          },
          data: baseData,
          tokens,
        });
        logger.info("Sent due-soon notifications", {
          uid,
          successCount: res.successCount,
          failureCount: res.failureCount,
        });
      } catch (err) {
        logger.error("Error sending due-soon notifications", { uid, error: (err as Error).message });
      }

      // Optional, more specific notifications for weaning weights and tasks.
      if (weaningWeightDueCount > 0) {
        try {
          const res = await admin.messaging().sendEachForMulticast({
            notification: {
              title: "Weaning weights due",
              body: `${weaningWeightDueCount} calf${weaningWeightDueCount === 1 ? "" : "s"} need weaning weights.`,
            },
            data: {
              type: "weaning_weight",
              count: String(weaningWeightDueCount),
            },
            tokens,
          });
          logger.info("Sent weaning-weight notifications", {
            uid,
            successCount: res.successCount,
            failureCount: res.failureCount,
          });
        } catch (err) {
          logger.error("Error sending weaning-weight notifications", { uid, error: (err as Error).message });
        }
      }

      if (tasksOverdueCount > 0) {
        try {
          const res = await admin.messaging().sendEachForMulticast({
            notification: {
              title: "Tasks overdue",
              body: `${tasksOverdueCount} farm task${tasksOverdueCount === 1 ? "" : "s"} are overdue.`,
            },
            data: {
              type: "tasks_overdue",
              count: String(tasksOverdueCount),
            },
            tokens,
          });
          logger.info("Sent tasks-overdue notifications", {
            uid,
            successCount: res.successCount,
            failureCount: res.failureCount,
          });
        } catch (err) {
          logger.error("Error sending tasks-overdue notifications", { uid, error: (err as Error).message });
        }
      }
    })
  );
});

/** Stripe webhook (Phase Later). Set STRIPE_WEBHOOK_SECRET and STRIPE_SECRET_KEY. Uses Express for raw body. */
export const stripeWebhook = onRequest(createStripeWebhookApp());
