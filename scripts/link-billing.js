#!/usr/bin/env node
/**
 * Link a HerdManager solution to a billing customer (Phase Later).
 * Prints steps to create a Stripe (or other) customer with solutionId metadata
 * and update the solution registry.
 *
 * Optional: If STRIPE_SECRET_KEY is set and the "stripe" package is installed
 * (npm install stripe), creates the Stripe customer and updates the registry in one step.
 *
 * Usage: node scripts/link-billing.js <solutionId>
 *        node scripts/link-billing.js <solutionId> --customer-id cus_xxx
 *
 * With --customer-id: only updates the registry (run update-solution.js).
 * Without: create via Stripe if configured, else print instructions.
 */

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");
const ROOT_DIR = path.join(__dirname, "..");

function printInstructions(solutionId, farmName) {
  console.log("Link billing for solution:", solutionId, "(" + (farmName || "") + ")");
  console.log("");
  console.log("1. Create a billing customer in your provider (e.g. Stripe) with metadata:");
  console.log("   solutionId:", solutionId);
  console.log("");
  console.log("   Stripe example:");
  console.log("   stripe customers create --metadata solutionId=" + solutionId);
  console.log("   (or via API: metadata: { solutionId: '" + solutionId + "' })");
  console.log("");
  console.log("2. After you have the customer ID (e.g. cus_xxx), update the registry:");
  console.log("   node scripts/link-billing.js " + solutionId + " --customer-id <cus_xxx>");
  console.log("");
  console.log("3. Optionally set plan and status:");
  console.log("   node scripts/update-solution.js " + solutionId + " --plan basic --billing-status active");
  console.log("");
  console.log("See docs/BILLING-IMPLEMENTATION.md for webhooks and full flow.");
}

async function createStripeCustomerAndUpdate(solutionId, entry) {
  const key = process.env.STRIPE_SECRET_KEY && process.env.STRIPE_SECRET_KEY.trim();
  if (!key) return false;
  let Stripe;
  try {
    Stripe = require("stripe");
  } catch (_) {
    return false;
  }
  const stripe = new Stripe(key);
  const customer = await stripe.customers.create({
    metadata: { solutionId },
    email: undefined,
    name: entry.farmName || solutionId,
  });
  execSync(
    `node scripts/update-solution.js ${solutionId} --billing-customer-id ${customer.id}`,
    { stdio: "inherit", cwd: ROOT_DIR }
  );
  console.log("Created Stripe customer:", customer.id);
  console.log("Optionally set plan/status: node scripts/update-solution.js", solutionId, "--plan <plan> --billing-status active");
  return true;
}

async function main() {
  const args = process.argv.slice(2);
  const solutionId = args.find((a) => !a.startsWith("--"));
  const customerIdIdx = args.indexOf("--customer-id");
  const customerId = customerIdIdx !== -1 && args[customerIdIdx + 1] ? args[customerIdIdx + 1].trim() : null;

  if (!solutionId) {
    console.error("Usage: node scripts/link-billing.js <solutionId> [--customer-id cus_xxx]");
    console.error("  Without --customer-id: create via Stripe if STRIPE_SECRET_KEY set, else print steps.");
    console.error("  With --customer-id: run update-solution.js to set billingCustomerId in registry.");
    process.exit(1);
  }

  if (!fs.existsSync(REGISTRY_PATH)) {
    console.error("Registry not found:", REGISTRY_PATH);
    console.error("Create it first: node scripts/create-solution.js [--name \"Farm Name\"]");
    process.exit(1);
  }

  let registry;
  try {
    registry = JSON.parse(fs.readFileSync(REGISTRY_PATH, "utf-8"));
    if (!Array.isArray(registry.solutions)) registry.solutions = [];
  } catch (e) {
    console.error("Error reading registry:", e.message);
    process.exit(1);
  }

  const entry = registry.solutions.find((s) => s.solutionId === solutionId);
  if (!entry) {
    console.error("Solution not found:", solutionId);
    process.exit(1);
  }

  if (customerId) {
    try {
      execSync(
        `node scripts/update-solution.js ${solutionId} --billing-customer-id ${customerId}`,
        { stdio: "inherit", cwd: ROOT_DIR }
      );
      console.log("Registry updated. Optionally set plan/status: node scripts/update-solution.js", solutionId, "--plan <plan> --billing-status active");
    } catch (e) {
      process.exit(e.status || 1);
    }
    return;
  }

  try {
    const done = await createStripeCustomerAndUpdate(solutionId, entry);
    if (done) return;
  } catch (e) {
    console.warn("Stripe create failed:", e.message || e);
  }
  printInstructions(solutionId, entry.farmName);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
