#!/usr/bin/env node
/**
 * Create a new HerdManager solution entry and add it to the solution registry.
 * Usage: node scripts/create-solution.js [--name "Farm Name"]
 * Outputs the new solutionId and next steps for provisioning.
 *
 * Registry file: scripts/solution-registry.json (create from solution-registry.example.json)
 */

const fs = require("fs");
const path = require("path");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");
const EXAMPLE_PATH = path.join(__dirname, "solution-registry.example.json");

function generateSlug(name) {
  const base = name
    ? name
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-|-$/g, "") || "farm"
    : "farm";
  const unique = Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
  return `${base}-${unique}`;
}

function main() {
  let farmName = "New Farm";
  const nameIdx = process.argv.indexOf("--name");
  if (nameIdx !== -1 && process.argv[nameIdx + 1]) {
    farmName = process.argv[nameIdx + 1];
  }

  let registry = { solutions: [] };
  if (fs.existsSync(REGISTRY_PATH)) {
    try {
      registry = JSON.parse(fs.readFileSync(REGISTRY_PATH, "utf-8"));
      if (!Array.isArray(registry.solutions)) registry.solutions = [];
    } catch (e) {
      console.error("Error reading registry:", e.message);
      process.exit(1);
    }
  } else {
    if (fs.existsSync(EXAMPLE_PATH)) {
      try {
        const example = JSON.parse(fs.readFileSync(EXAMPLE_PATH, "utf-8"));
        registry = { ...example, solutions: example.solutions ? [...example.solutions] : [] };
      } catch (_) {}
    }
    console.warn("No solution-registry.json found; creating new registry.");
  }

  const solutionId = generateSlug(farmName);
  const entry = {
    solutionId,
    farmName,
    firebaseProjectId: "",
    webUrl: "",
    bundleId: "com.herdmanager.app",
    createdAt: new Date().toISOString(),
    billingCustomerId: "",
    plan: "",
    billingStatus: "",
  };

  registry.solutions.push(entry);
  fs.writeFileSync(REGISTRY_PATH, JSON.stringify(registry, null, 2) + "\n", "utf-8");

  console.log("Created solution:", solutionId);
  console.log("");
  console.log("Next steps (see docs/architecture/INSTANCE-PER-FARM-STRATEGY.md §3.1):");
  console.log("  1. Create Firebase project; enable Auth, Firestore, Storage.");
  console.log("  2. Register Android and Web apps; get google-services.json and NEXT_PUBLIC_FIREBASE_*.");
  console.log("  3. Set in web .env.production: NEXT_PUBLIC_SOLUTION_ID=" + solutionId);
  console.log("  4. Deploy web dashboard to this instance's URL.");
  console.log("  5. Update this registry entry with firebaseProjectId and webUrl:");
  console.log("     node scripts/update-solution.js " + solutionId + " --firebase-project-id <project-id> --web-url <dashboard-url>");
  console.log("  6. (Optional) Build Android with solutionId: ./gradlew -PsolutionId=" + solutionId + " :app:assembleDebug");
  console.log("");
  console.log("After creating the Firebase project and updating the registry with firebaseProjectId, run:");
  console.log("  node scripts/provision-instance.js " + solutionId + " [--deploy-rules]");
  console.log("");
  console.log("For web env snippet and Android gradle command run:");
  console.log("  node scripts/env-for-solution.js " + solutionId + " [--support-url <url>]");
}

main();
