import { test, expect } from "@playwright/test";

/**
 * E2E tests for the dashboard.
 * Assumes the app is running without Firebase (or signed in) so the dashboard is visible.
 * For CI, do not set NEXT_PUBLIC_FIREBASE_* so the app shows the dashboard with sample data.
 */
test.describe("Dashboard", () => {
  test("opens Home and shows main content or sign-in when Firebase configured", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 1 })).toContainText("HerdManager", { timeout: 30_000 });
    const main = page.getByRole("main");
    const signInEmail = page.getByLabel("Email");
    await Promise.race([
      main.waitFor({ state: "visible", timeout: 30_000 }),
      signInEmail.waitFor({ state: "visible", timeout: 30_000 }),
    ]);
    if (await main.isVisible()) {
      await expect(page.getByRole("navigation", { name: /main navigation/i })).toBeVisible();
    } else {
      await expect(page.getByText(/Sign in to view your herd dashboard/)).toBeVisible();
    }
  });

  test("skip link moves focus to main content", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("main")).toBeVisible({ timeout: 15_000 });
    const skipLink = page.getByRole("link", { name: "Skip to main content" });
    await expect(skipLink).toBeAttached();
    await skipLink.evaluate((el: HTMLAnchorElement) => el.click());
    await expect(page).toHaveURL(/#main-content/);
    await expect(page.getByRole("main")).toBeFocused();
  });

  test("Home shows Refresh button and overview", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("main")).toBeVisible({ timeout: 15_000 });
    await expect(page.getByRole("heading", { name: "Your herd at a glance" })).toBeVisible({ timeout: 15_000 });
    await expect(page.getByRole("button", { name: "Refresh" })).toBeVisible();
    await expect(page.getByRole("region", { name: "Overview", exact: true })).toBeVisible();
  });

  test("switches tabs via bottom nav", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("button", { name: "Home", exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Profiles", exact: true }).click();
    await expect(page).toHaveURL(/\?tab=profiles/);
    await expect(page.getByRole("button", { name: "Profiles" })).toHaveAttribute("aria-current", "page");
    await page.getByRole("button", { name: "Alerts", exact: true }).click();
    await expect(page).toHaveURL(/\?tab=alerts/);
    await page.getByRole("button", { name: "Analytics", exact: true }).click();
    await expect(page).toHaveURL(/\?tab=analytics/);
  });

  test("keyboard shortcut 2 switches to Profiles tab", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("main")).toBeVisible({ timeout: 15_000 });
    await page.getByRole("main").click();
    await page.keyboard.press("2");
    await expect(page).toHaveURL(/\?tab=profiles/, { timeout: 5_000 });
    await expect(page.getByRole("button", { name: "Profiles" })).toHaveAttribute("aria-current", "page");
  });

  test("opens Settings from menu and shows Settings content", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await page.getByRole("button", { name: "Open menu" }).click();
    await page.getByRole("button", { name: "Settings", exact: true }).click();
    await expect(page).toHaveURL(/\?tab=settings/);
    await expect(page.getByRole("heading", { level: 2, name: "Settings" })).toBeVisible();
    await expect(page.getByRole("heading", { level: 3, name: "Appearance" })).toBeVisible();
  });

  test("Settings About section has API spec link and spec is served", async ({ page, request }) => {
    await page.goto("/?tab=settings", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 2, name: "Settings" })).toBeVisible({ timeout: 15_000 });
    await expect(page.getByRole("heading", { level: 3, name: "About" })).toBeVisible();
    const apiSpecLink = page.getByRole("link", { name: "API spec (OpenAPI 3)" });
    await expect(apiSpecLink).toBeVisible();
    await expect(apiSpecLink).toHaveAttribute("href", "/api/spec");
    const specRes = await request.get(new URL("/api/spec", page.url()).toString());
    expect(specRes.ok()).toBe(true);
    const body = await specRes.text();
    expect(body).toContain("openapi:");
    expect(body).toContain("HerdManager API");
  });

  test("toggles theme in Settings", async ({ page }) => {
    await page.goto("/?tab=settings", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 2, name: "Settings" })).toBeVisible({ timeout: 15_000 });
    const lightRadio = page.getByRole("radio", { name: "Light" });
    const darkRadio = page.getByRole("radio", { name: "Dark" });
    await expect(lightRadio).toBeVisible();
    await darkRadio.click();
    await expect(darkRadio).toBeChecked();
    await lightRadio.click();
    await expect(lightRadio).toBeChecked();
  });

  test("toggles sample data in Settings", async ({ page }) => {
    await page.goto("/?tab=settings", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 2, name: "Settings" })).toBeVisible({ timeout: 15_000 });
    const sampleCheckbox = page.getByRole("checkbox", { name: "Use sample data" });
    await expect(sampleCheckbox).toBeVisible({ timeout: 15_000 });
    const initialState = await sampleCheckbox.isChecked();
    await sampleCheckbox.click();
    await expect(sampleCheckbox).toBeChecked({ checked: !initialState });
    await sampleCheckbox.click();
    await expect(sampleCheckbox).toBeChecked({ checked: initialState });
  });

  test("Profiles: open animal detail modal and close", async ({ page }) => {
    await page.goto("/?tab=profiles", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 2, name: "Profiles" })).toBeVisible({ timeout: 15_000 });
    const firstProfile = page.getByRole("button", { name: /View details for/i }).first();
    await expect(firstProfile).toBeVisible({ timeout: 10_000 });
    await firstProfile.click();
    const dialog = page.getByRole("dialog", { name: "Animal details" });
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole("heading", { name: "Animal details" })).toBeVisible();
    await expect(dialog.getByText("Ear tag", { exact: true })).toBeVisible();
    await dialog.getByRole("button", { name: "Close" }).click();
    await expect(dialog).not.toBeVisible();
  });

  test("Profiles tab shows herd list or empty state", async ({ page }) => {
    await page.goto("/?tab=profiles", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 2, name: "Profiles" })).toBeVisible({ timeout: 15_000 });
    await Promise.race([
      page.getByRole("button", { name: /View details for/i }).first().waitFor({ state: "visible", timeout: 10_000 }),
      page.getByText(/No herd data yet|Enable sample data in Settings/).waitFor({ state: "visible", timeout: 10_000 }),
    ]);
    const hasList = await page.getByRole("button", { name: /View details for/i }).first().isVisible().catch(() => false);
    const hasEmpty = await page.getByText(/No herd data yet|Enable sample data in Settings/).isVisible().catch(() => false);
    expect(hasList || hasEmpty).toBe(true);
  });

  test("Analytics tab shows Summary and Export", async ({ page }) => {
    await page.goto("/?tab=analytics", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 2, name: "Analytics" })).toBeVisible({ timeout: 15_000 });
    await expect(page.getByRole("heading", { level: 3, name: "Summary" })).toBeVisible();
    await expect(page.getByRole("heading", { level: 3, name: "Export" })).toBeVisible();
    await expect(page.getByRole("button", { name: /Export report \(CSV\)/ })).toBeVisible();
    await expect(page.getByRole("button", { name: /Export report \(PDF\)/ })).toBeVisible();
  });

  test("Alerts tab shows filters including Withdrawal", async ({ page }) => {
    await page.goto("/?tab=alerts", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { level: 2, name: "Alerts" })).toBeVisible({ timeout: 15_000 });
    await expect(page.getByRole("button", { name: "Withdrawal", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Calving", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Pregnancy check", exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Withdrawal", exact: true }).click();
    await expect(page.getByRole("button", { name: "Withdrawal" })).toBeVisible();
  });

  test("Settings: when Edit farm is visible, open form and Cancel closes it", async ({ page }) => {
    await page.goto("/?tab=settings", { waitUntil: "domcontentloaded" });
    await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible({ timeout: 25_000 });
    await expect(page.getByRole("heading", { name: "Farm & sync" })).toBeVisible();
    const editFarmButton = page.getByRole("button", { name: "Edit farm", exact: true });
    const isEditVisible = await editFarmButton.isVisible();
    if (isEditVisible) {
      await editFarmButton.click();
      await expect(page.getByRole("heading", { name: "Edit farm" })).toBeVisible();
      await expect(page.getByLabel("Name")).toBeVisible();
      await expect(page.getByRole("heading", { name: "Contacts" })).toBeVisible();
      await expect(page.getByRole("button", { name: "Add contact", exact: true })).toBeVisible();
      await page.getByRole("button", { name: "Cancel", exact: true }).click();
      await expect(page.getByRole("heading", { name: "Edit farm" })).not.toBeVisible();
      await expect(editFarmButton).toBeVisible();
    }
  });
});
