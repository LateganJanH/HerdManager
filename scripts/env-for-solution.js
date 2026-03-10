#!/usr/bin/env node
/**
 * Print env snippet and Android build command for a given solution (multi-instance provisioning).
 * Usage: node scripts/env-for-solution.js <solutionId> [--support-url https://support.example.com]
 * Reads scripts/solution-registry.json to validate solutionId exists (if file present).
 */

const fs = require("fs");
const path = require("path");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");

function main() {
  const args = process.argv.slice(2);
  const solutionId = args.find((a) => !a.startsWith("--"));
  const supportIdx = args.indexOf("--support-url");
  const supportUrl = supportIdx !== -1 && args[supportIdx + 1] ? args[supportIdx + 1] : "";

  if (!solutionId) {
    console.error("Usage: node scripts/env-for-solution.js <solutionId> [--support-url <url>]");
    process.exit(1);
  }

  if (fs.existsSync(REGISTRY_PATH)) {
    try {
      const registry = JSON.parse(fs.readFileSync(REGISTRY_PATH, "utf-8"));
      const solutions = Array.isArray(registry.solutions) ? registry.solutions : [];
      const found = solutions.some((s) => s.solutionId === solutionId);
      if (!found) {
        console.warn("Warning: solutionId '" + solutionId + "' not found in solution-registry.json");
      }
    } catch (e) {
      console.warn("Warning: could not read registry:", e.message);
    }
  }

  console.log("# Web (.env.production or build env)");
  console.log("NEXT_PUBLIC_SOLUTION_ID=" + solutionId);
  if (supportUrl) {
    console.log("NEXT_PUBLIC_SUPPORT_URL=" + supportUrl);
  } else {
    console.log("# NEXT_PUBLIC_SUPPORT_URL=<your-support-portal-base-url>");
  }
  console.log("");
  console.log("# Android (from repo root)");
  const gradlePart = supportUrl
    ? `-PsolutionId=${solutionId} -PsupportBaseUrl=${supportUrl}`
    : `-PsolutionId=${solutionId}`;
  console.log("cd android && ./gradlew " + gradlePart + " :app:assembleDebug");
  console.log("# Windows: cd android && .\\gradlew.bat " + gradlePart + " :app:assembleDebug");
}

main();
