# Billing implementation guide (multi-instance)

This guide translates the **billing and procurement strategy** in [INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md) §7 into concrete implementation steps. Billing is **Phase Later**: the HerdManager app does not yet integrate with a payment provider; this doc is for when you add it.

**Principle:** One billing customer per **solution ID**. The solution registry and your billing system both key by `solutionId` so you have a single place to see “this instance is on Plan X, paid until Y.”

---

## 1. Strategy summary

| Decision | Choice |
|----------|--------|
| **Unit of billing** | One subscription per instance (per `solutionId`). |
| **Payment provider** | Use a subscription-aware provider (e.g. **Stripe**, Paddle) for cards, direct debit, or invoices. |
| **Customer identity** | One billing customer per `solutionId` (or per legal entity if one customer has multiple farms). Store the provider’s customer ID in the solution registry or your own DB. |
| **Flow** | Sign-up → Provision (create solution, assign solutionId) → Link to billing customer → Go-live → First invoice → Automated renewals and dunning. |

---

## 2. Solution registry and billing

The solution registry (`scripts/solution-registry.json`) can hold optional billing-related fields so ops and automation stay in sync. Example fields (all optional):

| Field | Description |
|-------|-------------|
| `billingCustomerId` | Payment provider’s customer ID (e.g. Stripe `cus_xxx`). Links this solution to the billing entity. |
| `plan` | Plan or product name (e.g. `basic`, `team`, `enterprise`). Use your provider’s product/price IDs in the billing system; this is for display or scripting. |
| `billingStatus` | Status for ops: e.g. `active`, `trial`, `past_due`, `suspended`, `cancelled`. Can be synced from webhooks. |

See `scripts/solution-registry.example.json` for the optional schema. Scripts (`create-solution.js`, `update-solution.js`) do not require these fields; add them when you integrate billing. The script `update-solution.js` supports optional `--billing-customer-id`, `--plan`, and `--billing-status` to store billing data in the registry.

---

## 3. Implementation checklist

### 3.1 Payment provider setup

1. **Create an account** with your chosen provider (e.g. Stripe).
2. **Define products and prices** (e.g. “HerdManager Basic” monthly, “HerdManager Team” annual). Use the provider’s dashboard or API.
3. **Configure webhooks** to receive subscription lifecycle events (e.g. `customer.subscription.created`, `customer.subscription.updated`, `invoice.paid`, `invoice.payment_failed`). Webhook endpoint can live in a small backend or Cloud Function that:
   - Identifies the subscription/customer (linked to `solutionId` via metadata).
   - Updates your solution registry or DB (e.g. set `billingStatus`, `plan`, or next billing date).
   - Optionally triggers “suspend access” (see §3.4).

   **Implemented:** A Cloud Function **stripeWebhook** in `functions/` verifies the Stripe signature and updates Firestore **`config/app`** with `accessSuspended: true` when `customer.subscription.updated` / `customer.subscription.deleted` has status cancelled/unpaid/past_due, or when `invoice.payment_failed` fires. The Android app reads `config/app` (global) and `users/{uid}/config/app` and shows “Subscription lapsed” when either has `accessSuspended: true`. Deploy with `firebase deploy --only functions`, then set `STRIPE_WEBHOOK_SECRET` and `STRIPE_SECRET_KEY` (Firebase config), and in Stripe add the webhook URL (e.g. `https://<region>-<project>.cloudfunctions.net/stripeWebhook`) and select the events above. See `functions/README.md` and `firestore.rules.example` for the `config/app` read rule.

### 3.2 Link solution to billing customer

When you **provision a new solution** (after `create-solution.js` and creating the Firebase project):

1. Create a **billing customer** in the provider (e.g. Stripe Customer) with metadata `solutionId: <id>`.
2. Store the provider’s **customer ID** (e.g. `cus_xxx`) in the solution registry (`billingCustomerId`) or in your own DB keyed by `solutionId`.
3. When the customer goes live, create a **Subscription** (or one-off invoice for setup fee) and attach it to that customer. Use the provider’s Checkout, Customer Portal, or API.

**Script:** From the repo root, run `node scripts/link-billing.js <solutionId>` to print step-by-step instructions (create customer with metadata, then update registry). After creating the customer in Stripe (or your provider), run `node scripts/link-billing.js <solutionId> --customer-id cus_xxx` to write the customer ID into the registry. Optionally set plan and status with `update-solution.js` (e.g. `--plan basic --billing-status active`).

**Automation:** A script or backend can: read `solution-registry.json` (or your DB), create a Stripe Customer per new solution with `metadata: { solutionId }`, then update the registry with the returned customer ID.

### 3.3 Renewals and dunning

- **Renewals:** The provider handles recurring billing. No custom code required if you use subscription products.
- **Dunning:** Configure in the provider: retry failed payments, send email reminders, and (after a grace period) cancel the subscription or mark it `past_due` / `suspended`.
- **Webhook:** On `invoice.payment_failed` or `customer.subscription.deleted`, update your registry/DB so ops (or an automated step) can suspend access or notify the customer.

### 3.4 Optional: suspend access when unpaid

To enforce “no pay, no access”:

- **Option A – accessSuspended (implemented):** When billing status is `suspended` or `cancelled`, set Firestore `users/{uid}/config/app` with **`accessSuspended: true`**. The Android app reads this field and shows **“Subscription lapsed. Contact support.”** with a “Contact support” button (opens `SUPPORT_BASE_URL` when set). See `firestore.rules.example` and Android `AppConfigRepository.getAccessSuspended`, `SubscriptionLapsedScreen`.
- **Option B – Min-version:** Alternatively set `minVersionCode` to a value higher than the current app version; the app shows “Update required” and “Open Play Store.” Use when you want to force an app update rather than a subscription message.
- **Option C – Auth:** If you move to a solution-level auth model, disable sign-in or sync for that instance when the subscription is inactive (e.g. check a “subscription active” flag in your backend before issuing tokens).
- **Option D – Registry only:** Keep `billingStatus` in the registry for ops visibility; handle suspension manually (e.g. turn off instance or contact customer) until you automate.

---

## 4. Single source of truth

- **Solution registry** holds: `solutionId`, `firebaseProjectId`, `webUrl`, `farmName`, and optionally `billingCustomerId`, `plan`, `billingStatus`.
- **Billing system** (e.g. Stripe) holds: customers, subscriptions, invoices, payment methods. Always store `solutionId` in customer metadata so you can join data (e.g. “list all solutions with past_due” by querying Stripe and matching metadata).
- **Reporting:** Revenue per instance = list subscriptions by customer, group by `solutionId` (from metadata). MRR/ARR and churn follow from the provider’s reporting or your own aggregation.

---

## 5. References

- **Strategy:** [INSTANCE-PER-FARM-STRATEGY.md](architecture/INSTANCE-PER-FARM-STRATEGY.md) §7 (Billing and procurement).
- **Registry:** [MULTI-INSTANCE-STRATEGY.md](MULTI-INSTANCE-STRATEGY.md) §5.1 (solution registry and scripts).
- **Provisioning:** Same strategy doc §3.1 (checklist); link solutionId to billing in step “Go-live and first invoice.”
