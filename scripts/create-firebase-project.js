#!/usr/bin/env node
/**
 * Add Firebase to an existing Google Cloud project and register it for a solution (Phase Later 3.2).
 * Usage: node scripts/create-firebase-project.js <solutionId> --project-id <gcp-project-id> [--deploy]
 *
 * Prerequisites:
 * - Solution already exists in solution-registry.json (run create-solution.js first).
 * - The GCP project must already exist (create it in Google Cloud Console if needed).
 * - Set GOOGLE_APPLICATION_CREDENTIALS to a service account key file that has:
 *   - Firebase Admin SDK Administrator role (or firebase.projects.update, resourcemanager.projects.get,
 *     serviceusage.services.enable, serviceusage.services.get) on the target project.
 *   - Enable Firebase Management API in the project that owns the service account.
 *
 * Steps:
 * 1. Call Firebase Management API projects.addFirebase for the given GCP project.
 * 2. Poll the operation until done.
 * 3. Update solution-registry.json with firebaseProjectId.
 * 4. If --deploy: run provision-instance.js with --deploy-rules and --deploy-functions.
 *
 * See docs/PROVISIONING-RUNBOOK.md and docs/architecture/INSTANCE-PER-FARM-STRATEGY.md.
 */

const fs = require("fs");
const path = require("path");
const https = require("https");
const { execSync } = require("child_process");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");
const ROOT_DIR = path.join(__dirname, "..");
const FIREBASE_API_BASE = "firebase.googleapis.com";
const TOKEN_URL = "oauth2.googleapis.com";

function getCredentials() {
  const keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!keyPath || !fs.existsSync(keyPath)) {
    console.error("Set GOOGLE_APPLICATION_CREDENTIALS to a service account key JSON path.");
    process.exit(1);
  }
  const key = JSON.parse(fs.readFileSync(keyPath, "utf-8"));
  if (!key.client_email || !key.private_key) {
    console.error("Invalid service account key: need client_email and private_key.");
    process.exit(1);
  }
  return key;
}

function base64UrlEncode(buf) {
  return Buffer.from(buf)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function createJwt(credentials, scope) {
  const header = { alg: "RS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    iss: credentials.client_email,
    sub: credentials.client_email,
    aud: `https://${TOKEN_URL}/token`,
    iat: now,
    exp: now + 3600,
    scope: scope || "https://www.googleapis.com/auth/cloud-platform",
  };
  const headerB64 = base64UrlEncode(JSON.stringify(header));
  const payloadB64 = base64UrlEncode(JSON.stringify(payload));
  const signatureInput = `${headerB64}.${payloadB64}`;
  const sign = require("crypto").createSign("RSA-SHA256");
  sign.update(signatureInput);
  const signature = sign.sign(credentials.private_key, "base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
  return `${signatureInput}.${signature}`;
}

function postHttps(host, path, body) {
  return new Promise((resolve, reject) => {
    const data = typeof body === "string" ? body : JSON.stringify(body);
    const req = https.request(
      {
        host,
        path,
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Content-Length": Buffer.byteLength(data),
        },
      },
      (res) => {
        let buf = "";
        res.on("data", (chunk) => { buf += chunk; });
        res.on("end", () => {
          try {
            resolve(JSON.parse(buf));
          } catch {
            resolve(buf);
          }
        });
      }
    );
    req.on("error", reject);
    req.write(data);
    req.end();
  });
}

function getAccessToken(credentials) {
  const jwt = createJwt(credentials);
  const body = `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${encodeURIComponent(jwt)}`;
  return postHttps(TOKEN_URL, "/token", body).then((res) => {
    if (res.access_token) return res.access_token;
    throw new Error(res.error_description || JSON.stringify(res));
  });
}

function request(host, path, method, token, body) {
  return new Promise((resolve, reject) => {
    const opts = {
      host,
      path,
      method: method || "GET",
      headers: { Authorization: `Bearer ${token}` },
    };
    if (body != null && body !== "") {
      opts.headers["Content-Type"] = "application/json";
      opts.headers["Content-Length"] = Buffer.byteLength(body);
    } else if (method === "POST") {
      opts.headers["Content-Length"] = 0;
    }
    const req = https.request(opts, (res) => {
      let buf = "";
      res.on("data", (chunk) => { buf += chunk; });
      res.on("end", () => {
        try {
          resolve({ status: res.statusCode, data: buf ? JSON.parse(buf) : null });
        } catch {
          resolve({ status: res.statusCode, data: buf });
        }
      });
    });
    req.on("error", reject);
    if (body) req.write(body);
    req.end();
  });
}

async function addFirebase(projectId, token) {
  const path = `/v1beta1/projects/${projectId}:addFirebase`;
  const { status, data } = await request(FIREBASE_API_BASE, path, "POST", token, "");
  if (status !== 200 || !data.name) {
    throw new Error(data?.error?.message || data?.error || `HTTP ${status}: ${JSON.stringify(data)}`);
  }
  return data; // Operation
}

async function getOperation(operationName, token) {
  const path = `/v1beta1/${operationName}`;
  const { status, data } = await request(FIREBASE_API_BASE, path, "GET", token);
  if (status !== 200) throw new Error(data?.error?.message || `HTTP ${status}`);
  return data;
}

async function pollUntilDone(operation, token) {
  const name = operation.name;
  for (let i = 0; i < 60; i++) {
    const op = await getOperation(name, token);
    if (op.done) {
      if (op.error) throw new Error(op.error.message || JSON.stringify(op.error));
      return op.response;
    }
    await new Promise((r) => setTimeout(r, 2000));
  }
  throw new Error("Operation timed out");
}

function main() {
  const args = process.argv.slice(2);
  const solutionId = args.find((a) => !a.startsWith("--"));
  const projectIdIdx = args.indexOf("--project-id");
  const projectId = projectIdIdx !== -1 && args[projectIdIdx + 1] ? args[projectIdIdx + 1].trim() : null;
  const deploy = args.includes("--deploy");

  if (!solutionId || !projectId) {
    console.error("Usage: node scripts/create-firebase-project.js <solutionId> --project-id <gcp-project-id> [--deploy]");
    console.error("  --project-id  Existing Google Cloud project ID to add Firebase to.");
    console.error("  --deploy      After adding Firebase, run provision-instance.js --deploy-rules --deploy-functions.");
    process.exit(1);
  }

  if (!fs.existsSync(REGISTRY_PATH)) {
    console.error("Registry not found. Run: node scripts/create-solution.js [--name \"Farm Name\"]");
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

  (async () => {
    console.log("Adding Firebase to GCP project:", projectId);
    const credentials = getCredentials();
    const token = await getAccessToken(credentials);
    const operation = await addFirebase(projectId, token);
    console.log("Waiting for Firebase provisioning...");
    await pollUntilDone(operation, token);
    console.log("Firebase added successfully.");

    execSync(
      `node scripts/update-solution.js ${solutionId} --firebase-project-id ${projectId}`,
      { stdio: "inherit", cwd: ROOT_DIR }
    );

    if (deploy) {
      console.log("Running provision-instance.js --deploy-rules --deploy-functions...");
      execSync(
        `node scripts/provision-instance.js ${solutionId} --deploy-rules --deploy-functions`,
        { stdio: "inherit", cwd: ROOT_DIR }
      );
    } else {
      console.log("");
      console.log("Next: deploy rules and functions with:");
      console.log("  node scripts/provision-instance.js", solutionId, "--deploy-rules --deploy-functions");
    }
  })().catch((e) => {
    console.error(e.message || e);
    process.exit(1);
  });
}

main();
