/** App display name for titles, branding, and manifest. */
export const APP_NAME = "HerdManager";

/** App version – set NEXT_PUBLIC_APP_VERSION in build or default to package version. */
export const APP_VERSION =
  typeof process !== "undefined" && process.env?.NEXT_PUBLIC_APP_VERSION
    ? process.env.NEXT_PUBLIC_APP_VERSION
    : "0.1.0";
