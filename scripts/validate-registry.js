#!/usr/bin/env node
/**
 * Validate solution-registry.json: no duplicate solutionIds, required fields present.
 * Usage: node scripts/validate-registry.js
 * Exit 0 if valid, 1 if invalid. Use in CI or before deploy.
 */

const fs = require("fs");
const path = require("path");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");

function main() {
  if (!fs.existsSync(REGISTRY_PATH)) {
    console.error("Registry not found:", REGISTRY_PATH);
    process.exit(1);
  }

  let registry;
  try {
    registry = JSON.parse(fs.readFileSync(REGISTRY_PATH, "utf-8"));
    if (!Array.isArray(registry.solutions)) {
      console.error("Registry must have a 'solutions' array");
      process.exit(1);
    }
  } catch (e) {
    console.error("Invalid JSON:", e.message);
    process.exit(1);
  }

  const solutions = registry.solutions;
  const ids = new Set();
  let failed = false;

  for (let i = 0; i < solutions.length; i++) {
    const s = solutions[i];
    if (!s.solutionId || typeof s.solutionId !== "string" || !s.solutionId.trim()) {
      console.error("Entry", i + 1, "missing or empty solutionId");
      failed = true;
    } else if (ids.has(s.solutionId)) {
      console.error("Duplicate solutionId:", s.solutionId);
      failed = true;
    } else {
      ids.add(s.solutionId);
    }
    if (!s.farmName || typeof s.farmName !== "string" || !String(s.farmName).trim()) {
      console.error("Entry", i + 1, "(solutionId:", s.solutionId || "?", ") missing or empty farmName");
      failed = true;
    }
  }

  if (failed) process.exit(1);
  process.exit(0);
}

main();
