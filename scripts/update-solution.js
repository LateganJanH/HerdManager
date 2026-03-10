#!/usr/bin/env node
/**
 * Update an existing solution entry in the solution registry (e.g. after creating
 * the Firebase project and deploying the web dashboard).
 * Usage: node scripts/update-solution.js <solutionId> [--firebase-project-id <id>] [--web-url <url>]
 *        [--billing-customer-id <id>] [--plan <plan>] [--billing-status <status>]
 * At least one option must be provided.
 */

const fs = require("fs");
const path = require("path");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");

function main() {
  const args = process.argv.slice(2);
  const solutionId = args.find((a) => !a.startsWith("--"));
  const projectIdx = args.indexOf("--firebase-project-id");
  const webUrlIdx = args.indexOf("--web-url");
  const billingCustomerIdx = args.indexOf("--billing-customer-id");
  const planIdx = args.indexOf("--plan");
  const billingStatusIdx = args.indexOf("--billing-status");
  const firebaseProjectId =
    projectIdx !== -1 && args[projectIdx + 1] ? args[projectIdx + 1].trim() : undefined;
  const webUrl = webUrlIdx !== -1 && args[webUrlIdx + 1] ? args[webUrlIdx + 1].trim() : undefined;
  const billingCustomerId =
    billingCustomerIdx !== -1 && args[billingCustomerIdx + 1] ? args[billingCustomerIdx + 1].trim() : undefined;
  const plan = planIdx !== -1 && args[planIdx + 1] ? args[planIdx + 1].trim() : undefined;
  const billingStatus =
    billingStatusIdx !== -1 && args[billingStatusIdx + 1] ? args[billingStatusIdx + 1].trim() : undefined;

  if (!solutionId) {
    console.error("Usage: node scripts/update-solution.js <solutionId> [--firebase-project-id <id>] [--web-url <url>] [--billing-customer-id <id>] [--plan <plan>] [--billing-status <status>]");
    process.exit(1);
  }
  const hasAny = firebaseProjectId !== undefined || webUrl !== undefined || billingCustomerId !== undefined || plan !== undefined || billingStatus !== undefined;
  if (!hasAny) {
    console.error("Provide at least one of: --firebase-project-id, --web-url, --billing-customer-id, --plan, --billing-status");
    process.exit(1);
  }

  if (!fs.existsSync(REGISTRY_PATH)) {
    console.error("Registry not found:", REGISTRY_PATH);
    console.error("Create it first with: node scripts/create-solution.js [--name \"Farm Name\"]");
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

  const idx = registry.solutions.findIndex((s) => s.solutionId === solutionId);
  if (idx === -1) {
    console.error("Solution not found:", solutionId);
    process.exit(1);
  }

  const entry = registry.solutions[idx];
  if (firebaseProjectId !== undefined) entry.firebaseProjectId = firebaseProjectId;
  if (webUrl !== undefined) entry.webUrl = webUrl;
  if (billingCustomerId !== undefined) entry.billingCustomerId = billingCustomerId;
  if (plan !== undefined) entry.plan = plan;
  if (billingStatus !== undefined) entry.billingStatus = billingStatus;

  fs.writeFileSync(REGISTRY_PATH, JSON.stringify(registry, null, 2) + "\n", "utf-8");
  console.log("Updated", solutionId, "in solution-registry.json");
  if (firebaseProjectId !== undefined) console.log("  firebaseProjectId:", firebaseProjectId);
  if (webUrl !== undefined) console.log("  webUrl:", webUrl);
  if (billingCustomerId !== undefined) console.log("  billingCustomerId:", billingCustomerId);
  if (plan !== undefined) console.log("  plan:", plan);
  if (billingStatus !== undefined) console.log("  billingStatus:", billingStatus);
}

main();
