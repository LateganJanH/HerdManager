#!/usr/bin/env node
/**
 * Post-create provisioning helper for a HerdManager solution (instance).
 * Run after create-solution.js and after you have created the Firebase project.
 * Usage: node scripts/provision-instance.js <solutionId> [--deploy-rules] [--deploy-functions]
 *
 * - Validates the solution exists in the registry.
 * - Prints a checklist and commands for: Firebase rules, functions, web env, web deploy, optional billing.
 * - With --deploy-rules: runs firebase use <projectId> and firebase deploy --only firestore:rules.
 * - With --deploy-functions: runs firebase use <projectId> and firebase deploy --only functions.
 *   (Requires Firebase CLI and firebaseProjectId set in the registry.)
 *
 * See docs/architecture/INSTANCE-PER-FARM-STRATEGY.md §3.1–3.2.
 */

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");
const ROOT_DIR = path.join(__dirname, "..");

function main() {
  const args = process.argv.slice(2);
  const solutionId = args.find((a) => !a.startsWith("--"));
  const deployRules = args.includes("--deploy-rules");
  const deployFunctions = args.includes("--deploy-functions");

  if (!solutionId) {
    console.error("Usage: node scripts/provision-instance.js <solutionId> [--deploy-rules] [--deploy-functions]");
    process.exit(1);
  }

  if (!fs.existsSync(REGISTRY_PATH)) {
    console.error("Registry not found:", REGISTRY_PATH);
    console.error("Create a solution first: node scripts/create-solution.js [--name \"Farm Name\"]");
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
    console.error("Solution not found in registry:", solutionId);
    process.exit(1);
  }

  const projectId = (entry.firebaseProjectId || "").trim();
  const webUrl = (entry.webUrl || "").trim();

  if (!projectId) {
    console.log("Solution:", solutionId, "(" + (entry.farmName || "—") + ")");
    console.log("");
    console.log("Next: add the Firebase project ID to the registry (after creating the project in Firebase Console):");
    console.log("  node scripts/update-solution.js", solutionId, "--firebase-project-id <your-project-id>");
    console.log("");
    console.log("Then run this script again to see the full provisioning checklist.");
    return;
  }

  console.log("Provisioning checklist for:", solutionId, "(" + (entry.farmName || "—") + ")");
  console.log("Firebase project:", projectId);
  console.log("");

  const steps = [];

  steps.push({
    done: !!projectId,
    title: "Firebase project created and registered",
    cmd: null,
  });

  steps.push({
    done: false,
    title: "Deploy Firestore rules",
    cmd: `firebase use ${projectId} && firebase deploy --only firestore:rules`,
  });

  steps.push({
    done: false,
    title: "Deploy Cloud Functions",
    cmd: `firebase use ${projectId} && firebase deploy --only functions`,
  });

  steps.push({
    done: false,
    title: "Set web env and deploy dashboard",
    cmd: `node scripts/env-for-solution.js ${solutionId} [--support-url <url>]`,
  });

  if (!webUrl) {
    steps.push({
      done: false,
      title: "Update registry with web URL after deploy",
      cmd: `node scripts/update-solution.js ${solutionId} --web-url <dashboard-url>`,
    });
  } else {
    steps.push({
      done: true,
      title: "Web URL registered: " + webUrl,
      cmd: null,
    });
  }

  steps.push({
    done: !!(entry.billingCustomerId || "").trim(),
    title: "(Optional) Link billing",
    cmd: `node scripts/link-billing.js ${solutionId}`,
  });

  steps.forEach((s, i) => {
    const mark = s.done ? "[x]" : "[ ]";
    console.log(`  ${mark} ${i + 1}. ${s.title}`);
    if (s.cmd) console.log("       " + s.cmd);
  });

  console.log("");
  console.log("To deploy from repo root:");
  console.log("  firebase use", projectId);
  console.log("  firebase deploy --only firestore:rules");
  console.log("  firebase deploy --only functions");
  console.log("");

  function runFirebaseUse() {
    execSync(`firebase use ${projectId}`, { stdio: "inherit", cwd: ROOT_DIR });
  }

  if (deployRules || deployFunctions) {
    try {
      runFirebaseUse();
      if (deployRules) {
        console.log("Deploying Firestore rules...");
        execSync("firebase deploy --only firestore:rules", { stdio: "inherit", cwd: ROOT_DIR });
      }
      if (deployFunctions) {
        console.log("Deploying Cloud Functions...");
        execSync("firebase deploy --only functions", { stdio: "inherit", cwd: ROOT_DIR });
      }
      console.log("Done.");
    } catch (e) {
      console.error("Deploy failed. Run the commands above manually.");
      process.exit(1);
    }
  }
}

main();
