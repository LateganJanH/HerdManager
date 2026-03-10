#!/usr/bin/env node
/**
 * List solutions from the solution registry (for deploy loops and ops).
 * Usage:
 *   node scripts/list-solutions.js              # human-readable table
 *   node scripts/list-solutions.js --ids        # one solutionId per line
 *   node scripts/list-solutions.js --project-ids # one firebaseProjectId per line (non-empty only)
 *   node scripts/list-solutions.js --json        # JSON array of entries
 *   node scripts/list-solutions.js --deployable # only entries with non-empty firebaseProjectId
 * Table columns include plan, billingStatus, and billingCustomerId when present.
 */

const fs = require("fs");
const path = require("path");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");

function main() {
  const args = process.argv.slice(2);
  const idsOnly = args.includes("--ids");
  const projectIdsOnly = args.includes("--project-ids");
  const jsonOut = args.includes("--json");
  const deployableOnly = args.includes("--deployable");

  if (!fs.existsSync(REGISTRY_PATH)) {
    console.error("Registry not found:", REGISTRY_PATH);
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

  let solutions = registry.solutions;
  if (deployableOnly) {
    solutions = solutions.filter((s) => s.firebaseProjectId && String(s.firebaseProjectId).trim());
  }

  if (idsOnly) {
    solutions.forEach((s) => console.log(s.solutionId));
    return;
  }
  if (projectIdsOnly) {
    solutions
      .filter((s) => s.firebaseProjectId && String(s.firebaseProjectId).trim())
      .forEach((s) => console.log(s.firebaseProjectId));
    return;
  }
  if (jsonOut) {
    console.log(JSON.stringify(solutions, null, 2));
    return;
  }

  // Human-readable
  if (solutions.length === 0) {
    console.log("No solutions" + (deployableOnly ? " with firebaseProjectId set" : "") + ".");
    return;
  }
  const col = (s, w) => String(s).slice(0, w).padEnd(w);
  const wId = Math.max(12, ...solutions.map((s) => String(s.solutionId).length));
  const wProject = Math.max(10, ...solutions.map((s) => String(s.firebaseProjectId || "").length), 18);
  const wPlan = Math.max(8, ...solutions.map((s) => String(s.plan || "").length), 10);
  const wBilling = Math.max(13, ...solutions.map((s) => String(s.billingStatus || "").length), 14);
  const wCust = Math.max(14, ...solutions.map((s) => String(s.billingCustomerId || "").length), 20);
  const wUrl = Math.max(8, ...solutions.map((s) => String(s.webUrl || "").length), 24);
  console.log(col("solutionId", wId) + col("firebaseProjectId", wProject) + col("plan", wPlan) + col("billingStatus", wBilling) + col("billingCustomerId", wCust) + "webUrl");
  console.log("-".repeat(wId + wProject + wPlan + wBilling + wCust + wUrl));
  for (const s of solutions) {
    console.log(col(s.solutionId, wId) + col(s.firebaseProjectId || "", wProject) + col(s.plan || "", wPlan) + col(s.billingStatus || "", wBilling) + col(s.billingCustomerId || "", wCust) + (s.webUrl || ""));
  }
}

main();
