#!/usr/bin/env node
/**
 * Create a new Google Cloud project for a HerdManager solution (optional full automation).
 *
 * This is the missing "Phase Later" step before create-firebase-project.js:
 * - Creates a GCP project via Cloud Resource Manager API.
 * - Optionally links a billing account.
 * - Prints follow-up commands to add Firebase and provision the instance.
 *
 * Usage:
 *   node scripts/create-gcp-project.js <solutionId> \\
 *     --project-id <gcp-project-id> \\
 *     --name "Farm Name" \\
 *     --org-id <orgNumericId> \\
 *     --billing-account <billingAccounts/XXXX-XXXXXX-XXXXXX>
 *
 * Requirements:
 * - solution-registry.json exists and contains the solution (run create-solution.js first).
 * - GOOGLE_APPLICATION_CREDENTIALS points to a service account JSON with:
 *     - resourcemanager.projects.create
 *     - resourcemanager.projects.get
 *     - serviceusage.services.enable (for later Firebase steps)
 *     - billing.resourceAssociations.create on the billing account
 * - The Cloud Resource Manager and Cloud Billing APIs are enabled for the service account's project.
 *
 * With --deploy, runs create-firebase-project.js and provision-instance after project creation.
 * See docs/PROVISIONING-RUNBOOK.md (Option C) and docs/architecture/INSTANCE-PER-FARM-STRATEGY.md §3.2.
 */

const fs = require("fs");
const path = require("path");
const https = require("https");
const { execSync } = require("child_process");

const REGISTRY_PATH = path.join(__dirname, "solution-registry.json");
const ROOT_DIR = path.join(__dirname, "..");
const CLOUDRESOURCEMANAGER_HOST = "cloudresourcemanager.googleapis.com";
const CLOUDBILLING_HOST = "cloudbilling.googleapis.com";
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

function postForm(host, path, body) {
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
  return postForm(TOKEN_URL, "/token", body).then((res) => {
    if (res.access_token) return res.access_token;
    throw new Error(res.error_description || JSON.stringify(res));
  });
}

function requestJson(host, path, method, token, bodyObj) {
  return new Promise((resolve, reject) => {
    const body = bodyObj == null ? "" : JSON.stringify(bodyObj);
    const opts = {
      host,
      path,
      method: method || "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    };
    if (body) {
      opts.headers["Content-Length"] = Buffer.byteLength(body);
    }
    const req = https.request(opts, (res) => {
      let buf = "";
      res.on("data", (chunk) => { buf += chunk; });
      res.on("end", () => {
        let data = null;
        try {
          data = buf ? JSON.parse(buf) : null;
        } catch {
          data = buf;
        }
        resolve({ status: res.statusCode, data });
      });
    });
    req.on("error", reject);
    if (body) req.write(body);
    req.end();
  });
}

async function createProject(projectId, name, parent, token) {
  const body = {
    projectId,
    name,
  };
  if (parent) {
    body.parent = parent;
  }
  const { status, data } = await requestJson(
    CLOUDRESOURCEMANAGER_HOST,
    "/v1/projects",
    "POST",
    token,
      body
  );
  if (status !== 200 && status !== 201) {
    throw new Error(data?.error?.message || `HTTP ${status}: ${JSON.stringify(data)}`);
  }
  if (!data.name) {
    throw new Error("Unexpected response from projects.create: " + JSON.stringify(data));
  }
  return data; // long-running operation
}

async function getOperation(operationName, token) {
  const { status, data } = await requestJson(
    CLOUDRESOURCEMANAGER_HOST,
    `/v1/${operationName}`,
    "GET",
    token
  );
  if (status !== 200) {
    throw new Error(data?.error?.message || `HTTP ${status}: ${JSON.stringify(data)}`);
  }
  return data;
}

async function pollOperation(operation, token, maxSeconds) {
  const name = operation.name;
  const maxIters = Math.max(1, Math.floor((maxSeconds || 300) / 5));
  for (let i = 0; i < maxIters; i++) {
    const op = await getOperation(name, token);
    if (op.done) {
      if (op.error) {
        throw new Error(op.error.message || JSON.stringify(op.error));
      }
      return op.response;
    }
    await new Promise((r) => setTimeout(r, 5000));
  }
  throw new Error("Project creation operation timed out");
}

async function linkBilling(projectId, billingAccount, token) {
  const name = `projects/${projectId}`;
  const { status, data } = await requestJson(
    CLOUDBILLING_HOST,
    `/v1/${name}/billingInfo`,
    "PUT",
    token,
    {
      name: `projects/${projectId}/billingInfo`,
      projectId,
      billingAccountName: billingAccount,
      billingEnabled: true,
    }
  );
  if (status !== 200) {
    throw new Error(data?.error?.message || `HTTP ${status}: ${JSON.stringify(data)}`);
  }
  return data;
}

function parseArgs() {
  const args = process.argv.slice(2);
  const solutionId = args.find((a) => !a.startsWith("--"));
  function getFlag(name) {
    const idx = args.indexOf(name);
    if (idx === -1) return null;
    return args[idx + 1] || null;
  }
  const projectId = getFlag("--project-id");
  const name = getFlag("--name") || projectId;
  const orgId = getFlag("--org-id");
  const folderId = getFlag("--folder-id");
  const billingAccount = getFlag("--billing-account");
  const deploy = args.includes("--deploy");

  if (!solutionId || !projectId || (!orgId && !folderId)) {
    console.error("Usage: node scripts/create-gcp-project.js <solutionId> \\");
    console.error("  --project-id <gcp-project-id> --name \"Farm Name\" \\");
    console.error("  (--org-id <orgNumericId> | --folder-id <folderNumericId>) \\");
    console.error("  [--billing-account billingAccounts/XXXX-XXXXXX-XXXXXX] [--deploy]");
    console.error("");
    console.error("  --deploy   After creating the project, run create-firebase-project.js and provision rules/functions.");
    process.exit(1);
  }

  // Cloud Resource Manager API expects parent as string: "organizations/123" or "folders/456"
  let parent = null;
  if (orgId) {
    parent = `organizations/${orgId}`;
  } else if (folderId) {
    parent = `folders/${folderId}`;
  }

  return { solutionId, projectId, name, parent, billingAccount, deploy };
}

function ensureSolutionExists(solutionId) {
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
    console.error("Solution not found in registry:", solutionId);
    process.exit(1);
  }
  return entry;
}

async function main() {
  const { solutionId, projectId, name, parent, billingAccount, deploy } = parseArgs();
  ensureSolutionExists(solutionId);

  console.log("Creating GCP project for solution:", solutionId);
  console.log("  Project ID:", projectId);
  console.log("  Name     :", name);
  console.log("  Parent   :", parent || "(none)");
  if (billingAccount) {
    console.log("  Billing  :", billingAccount);
  } else {
    console.log("  Billing  : (not set; you can link later)");
  }
  console.log("");

  const credentials = getCredentials();
  const token = await getAccessToken(credentials);

  const op = await createProject(projectId, name, parent, token);
  console.log("Project creation started:", op.name || "(operation)");
  await pollOperation(op, token, 300);
  console.log("Project created:", projectId);

  if (billingAccount) {
    console.log("Linking billing account...");
    await linkBilling(projectId, billingAccount, token);
    console.log("Billing linked.");
  }

  if (deploy) {
    console.log("");
    console.log("Running create-firebase-project.js --deploy...");
    execSync(
      `node scripts/create-firebase-project.js ${solutionId} --project-id ${projectId} --deploy`,
      { stdio: "inherit", cwd: ROOT_DIR }
    );
  } else {
    console.log("");
    console.log("Next steps:");
    console.log("  1) Add Firebase to the project and update the solution registry:");
    console.log("       node scripts/create-firebase-project.js", solutionId, "--project-id", projectId, "[--deploy]");
    console.log("  2) (If you skipped billing here) link billing later with Cloud Console or gcloud,");
    console.log("     then optionally run: node scripts/link-billing.js", solutionId);
  }
}

main().catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});

