import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { GET } from "./route";

describe("GET /api/v1/billing-status", () => {
  const origEnv = process.env;

  afterEach(() => {
    process.env = { ...origEnv };
  });

  it("returns 501 when no billing env is set", async () => {
    delete process.env.BILLING_PLAN;
    delete process.env.BILLING_STATUS;
    delete process.env.BILLING_CUSTOMER_ID;

    const res = await GET();
    expect(res.status).toBe(501);

    const data = (await res.json()) as { error: string; message?: string };
    expect(data.error).toBe("Billing not configured");
  });

  it("returns 200 with plan and billingStatus when env is set", async () => {
    process.env.BILLING_PLAN = "basic";
    process.env.BILLING_STATUS = "active";

    const res = await GET();
    expect(res.status).toBe(200);

    const data = (await res.json()) as {
      solutionId: string;
      plan: string | null;
      billingStatus: string | null;
      billingCustomerId: string | null;
    };
    expect(typeof data.solutionId).toBe("string");
    expect(data.plan).toBe("basic");
    expect(data.billingStatus).toBe("active");
    expect(data.billingCustomerId).toBeNull();
  });

  it("sets Cache-Control no-store", async () => {
    process.env.BILLING_PLAN = "team";

    const res = await GET();
    const cacheControl = res.headers.get("Cache-Control") ?? "";
    expect(cacheControl).toMatch(/no-store|max-age=0/);
  });
});
