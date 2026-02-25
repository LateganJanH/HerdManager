/**
 * Linked mobile field devices (phones/tablets running the Android app)
 * that sync herd data to this web dashboard.
 */

export interface LinkedDevice {
  id: string;
  name: string;
  lastSyncAt: number;
  /** Optional: device model or "Android" for display */
  platform?: string;
}

export interface LinkedDevicesResponse {
  devices: LinkedDevice[];
}

/** Returns true if the value has required LinkedDevice fields (id, name, lastSyncAt). */
export function isValidLinkedDevice(v: unknown): v is LinkedDevice {
  if (!v || typeof v !== "object") return false;
  const o = v as Record<string, unknown>;
  return (
    typeof o.id === "string" &&
    typeof o.name === "string" &&
    typeof o.lastSyncAt === "number"
  );
}

/** Filters an array to only valid LinkedDevice items. */
export function filterValidDevices(items: unknown[]): LinkedDevice[] {
  return items.filter(isValidLinkedDevice);
}

/** Mock devices for demo when sample data is enabled and API returns no devices. */
export function getMockLinkedDevices(): LinkedDevice[] {
  const now = Date.now();
  return [
    { id: "mock-1", name: "Field phone", lastSyncAt: now - 2 * 60_000, platform: "Android" },
    { id: "mock-2", name: "Barn tablet", lastSyncAt: now - 45 * 60_000, platform: "Android" },
  ];
}
