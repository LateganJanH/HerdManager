/**
 * Stripe webhook handler (Phase Later – billing).
 * Receives subscription/invoice events, verifies signature, and updates Firestore config/app
 * (e.g. accessSuspended) so the Android app can show "Subscription lapsed" when unpaid.
 *
 * Deploy and set STRIPE_WEBHOOK_SECRET in Firebase config. Configure the webhook URL in Stripe
 * (e.g. https://<region>-<project>.cloudfunctions.net/stripeWebhook) and select events:
 * customer.subscription.updated, customer.subscription.deleted, invoice.payment_failed.
 *
 * Uses Express with raw body middleware so Stripe signature verification works.
 */

import * as admin from "firebase-admin";
import express, { Request, Response } from "express";
import Stripe from "stripe";

const db = admin.firestore();
const CONFIG_APP = "config/app";
const FIELD_ACCESS_SUSPENDED = "accessSuspended";

function getAccessSuspendedForSubscription(sub: Stripe.Subscription): boolean {
  const status = sub.status;
  return status === "canceled" || status === "unpaid" || status === "past_due";
}

export function createStripeWebhookApp(): express.Express {
  const app = express();

  // Stripe requires the raw body for signature verification; do not use express.json() for this route
  app.post(
    "/",
    express.raw({ type: "application/json" }),
    async (req: Request, res: Response): Promise<void> => {
      const secret = process.env.STRIPE_WEBHOOK_SECRET;
      if (!secret) {
        console.warn("STRIPE_WEBHOOK_SECRET not set; skipping webhook");
        res.status(500).send("Webhook not configured");
        return;
      }

      const sig = req.headers["stripe-signature"];
      if (!sig || typeof sig !== "string") {
        res.status(400).send("Missing stripe-signature");
        return;
      }

      let event: Stripe.Event;
      const body = req.body as Buffer;
      try {
        const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || "");
        event = stripe.webhooks.constructEvent(body, sig, secret);
      } catch (err) {
        const message = err instanceof Error ? err.message : "Unknown error";
        console.warn("Stripe webhook signature verification failed:", message);
        res.status(400).send(`Webhook Error: ${message}`);
        return;
      }

      try {
        if (event.type === "customer.subscription.updated" || event.type === "customer.subscription.deleted") {
          const sub = event.data.object as Stripe.Subscription;
          const accessSuspended = getAccessSuspendedForSubscription(sub);
          await db.doc(CONFIG_APP).set({ [FIELD_ACCESS_SUSPENDED]: accessSuspended }, { merge: true });
          console.log("Updated config/app accessSuspended:", accessSuspended, "subscription:", sub.id);
        } else if (event.type === "invoice.payment_failed") {
          // Conservative: treat payment failure as access suspended until subscription is updated
          await db.doc(CONFIG_APP).set({ [FIELD_ACCESS_SUSPENDED]: true }, { merge: true });
          console.log("Updated config/app accessSuspended=true after invoice.payment_failed");
        }
        res.json({ received: true });
      } catch (err) {
        console.error("Webhook handler error:", err);
        res.status(500).send("Webhook handler error");
      }
    }
  );

  return app;
}
