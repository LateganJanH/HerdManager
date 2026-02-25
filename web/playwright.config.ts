import { defineConfig, devices } from "@playwright/test";

/** Port used by the E2E dev server to avoid conflicting with a dev server on 3000. */
const E2E_PORT = 3100;

/** Unset Firebase so the app shows the dashboard (no sign-in) when .env.local has Firebase keys. */
const E2E_ENV = {
  ...process.env,
  NEXT_PUBLIC_FIREBASE_API_KEY: "",
  NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN: "",
  NEXT_PUBLIC_FIREBASE_PROJECT_ID: "",
  NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET: "",
  NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID: "",
  NEXT_PUBLIC_FIREBASE_APP_ID: "",
};

/**
 * E2E tests for the web dashboard.
 * Run with: pnpm exec playwright test
 * The dev server is started with Firebase env vars cleared so the dashboard is shown without sign-in.
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: "e2e",
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  timeout: 60_000,
  reporter: "html",
  use: {
    baseURL: `http://localhost:${E2E_PORT}`,
    trace: "on-first-retry",
    actionTimeout: 15_000,
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "pnpm dev:e2e",
    url: `http://localhost:${E2E_PORT}/api/health`,
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
    stdout: "pipe",
    stderr: "pipe",
    env: E2E_ENV,
  },
});
