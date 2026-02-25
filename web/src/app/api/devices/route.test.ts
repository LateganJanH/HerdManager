import { describe, it, expect } from "vitest";
import { filterValidDevices } from "../../lib/linkedDevices";
import { GET } from "./route";

describe("GET /api/devices", () => {
  it("returns 200 with devices array", async () => {
    const res = await GET();
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(Array.isArray(body.devices)).toBe(true);
  });

  it("returns empty devices list until backend is connected", async () => {
    const res = await GET();
    const body = await res.json();
    expect(body.devices).toHaveLength(0);
  });

  it("response devices have valid shape (id, name, lastSyncAt)", async () => {
    const res = await GET();
    const body = await res.json();
    const valid = filterValidDevices(body.devices ?? []);
    expect(valid.length).toBe(body.devices.length);
  });
});
