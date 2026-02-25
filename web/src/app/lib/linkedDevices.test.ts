import { describe, it, expect } from "vitest";
import {
  filterValidDevices,
  getMockLinkedDevices,
  isValidLinkedDevice,
} from "./linkedDevices";

describe("linkedDevices", () => {
  describe("isValidLinkedDevice", () => {
    it("returns true for valid device objects", () => {
      expect(isValidLinkedDevice({ id: "a", name: "Device", lastSyncAt: 123 })).toBe(true);
      expect(isValidLinkedDevice({ id: "b", name: "X", lastSyncAt: 0, platform: "Android" })).toBe(
        true
      );
    });

    it("returns false for non-objects or missing fields", () => {
      expect(isValidLinkedDevice(null)).toBe(false);
      expect(isValidLinkedDevice(undefined)).toBe(false);
      expect(isValidLinkedDevice({})).toBe(false);
      expect(isValidLinkedDevice({ id: "a", name: "X" })).toBe(false);
      expect(isValidLinkedDevice({ id: "a", lastSyncAt: 1 })).toBe(false);
      expect(isValidLinkedDevice({ name: "X", lastSyncAt: 1 })).toBe(false);
      expect(isValidLinkedDevice({ id: 1, name: "X", lastSyncAt: 1 })).toBe(false);
      expect(isValidLinkedDevice({ id: "a", name: 2, lastSyncAt: 1 })).toBe(false);
      expect(isValidLinkedDevice({ id: "a", name: "X", lastSyncAt: "1" })).toBe(false);
    });
  });

  describe("filterValidDevices", () => {
    it("returns only valid devices", () => {
      const input = [
        { id: "1", name: "A", lastSyncAt: 1 },
        null,
        { id: "2", name: "B" },
        { id: "3", name: "C", lastSyncAt: 3 },
      ];
      expect(filterValidDevices(input)).toEqual([
        { id: "1", name: "A", lastSyncAt: 1 },
        { id: "3", name: "C", lastSyncAt: 3 },
      ]);
    });
  });

  describe("getMockLinkedDevices", () => {
    it("returns two devices with required fields", () => {
      const devices = getMockLinkedDevices();
      expect(devices).toHaveLength(2);
      for (const d of devices) {
        expect(d).toHaveProperty("id");
        expect(d).toHaveProperty("name");
        expect(d).toHaveProperty("lastSyncAt");
        expect(typeof d.id).toBe("string");
        expect(typeof d.name).toBe("string");
        expect(typeof d.lastSyncAt).toBe("number");
      }
    });

    it("returns devices with distinct ids", () => {
      const devices = getMockLinkedDevices();
      expect(devices[0].id).not.toBe(devices[1].id);
    });

    it("uses recent lastSyncAt timestamps (within last hour)", () => {
      const now = Date.now();
      const devices = getMockLinkedDevices();
      for (const d of devices) {
        expect(d.lastSyncAt).toBeLessThanOrEqual(now);
        expect(d.lastSyncAt).toBeGreaterThan(now - 3600_000);
      }
    });

    it("includes platform Android for display", () => {
      const devices = getMockLinkedDevices();
      expect(devices.every((d) => d.platform === "Android")).toBe(true);
    });
  });
});
