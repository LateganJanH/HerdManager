/** App display name for titles, branding, and manifest. */
export const APP_NAME = "HerdManager";

/** App version – set NEXT_PUBLIC_APP_VERSION in build or default to package version. */
export const APP_VERSION =
  typeof process !== "undefined" && process.env?.NEXT_PUBLIC_APP_VERSION
    ? process.env.NEXT_PUBLIC_APP_VERSION
    : "0.5.0";

/** Solution/instance ID for this deployment (multi-instance). Set NEXT_PUBLIC_SOLUTION_ID when provisioning. */
export const SOLUTION_ID =
  typeof process !== "undefined" ? process.env?.NEXT_PUBLIC_SOLUTION_ID ?? "" : "";

/** Base URL for support portal (e.g. https://support.yourproduct.com). Links in Settings use this + solutionId. */
export const SUPPORT_BASE_URL =
  typeof process !== "undefined" ? process.env?.NEXT_PUBLIC_SUPPORT_URL ?? "" : "";
