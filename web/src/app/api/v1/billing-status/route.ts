import { NextResponse } from "next/server";
import { SOLUTION_ID } from "../../../lib/version";

/**
 * Returns billing/subscription status for the current instance (Phase Later).
 * When billing is integrated, set BILLING_PLAN, BILLING_STATUS, and optionally
 * BILLING_CUSTOMER_ID in server env (e.g. from registry or webhook-updated store).
 * If no billing env is set, returns 501 so clients know billing is not configured.
 * OpenAPI: GET /api/v1/billing-status → BillingStatusResponse.
 */
export async function GET() {
  const plan = process.env.BILLING_PLAN?.trim() || null;
  const billingStatus = process.env.BILLING_STATUS?.trim() || null;
  const billingCustomerId = process.env.BILLING_CUSTOMER_ID?.trim() || null;

  const solutionId = SOLUTION_ID || "";

  if (plan == null && billingStatus == null && billingCustomerId == null) {
    return NextResponse.json(
      { error: "Billing not configured", message: "Set BILLING_PLAN and/or BILLING_STATUS in server env when billing is integrated." },
      { status: 501, headers: { "Cache-Control": "no-store, max-age=0" } }
    );
  }

  return NextResponse.json(
    {
      solutionId,
      plan: plan ?? null,
      billingStatus: billingStatus ?? null,
      billingCustomerId: billingCustomerId ?? null,
    },
    { headers: { "Cache-Control": "no-store, max-age=0" } }
  );
}
