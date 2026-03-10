import { describe, it, expect, vi, beforeEach } from "vitest";
import { GET } from "./route";
import * as firebaseAdmin from "../../../lib/firebaseAdmin";

vi.mock("../../../lib/firebaseAdmin", () => ({
  verifyIdToken: vi.fn(),
  getAdminFirestore: vi.fn(),
}));

describe("GET /api/v1/breeding-events", () => {
  beforeEach(() => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue(null);
    vi.mocked(firebaseAdmin.getAdminFirestore).mockReset();
  });

  it("returns 401 when Authorization header is missing", async () => {
    const req = new Request("http://localhost/api/v1/breeding-events");
    const res = await GET(req);
    expect(res.status).toBe(401);
    const data = (await res.json()) as { error: string };
    expect(data.error).toBe("Unauthorized");
  });

  it("returns 401 when token is invalid", async () => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue(null);
    const req = new Request("http://localhost/api/v1/breeding-events", {
      headers: { Authorization: "Bearer invalid" },
    });
    const res = await GET(req);
    expect(res.status).toBe(401);
  });

  it("returns 200 with breedingEvents array when token valid and Firestore returns docs", async () => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue({ uid: "user1" });
    const mockGet = vi.fn().mockResolvedValue({
      docs: [
        {
          id: "be1",
          data: () => ({
            animalId: "a1",
            sireIds: ["sire1"],
            eventType: "AI",
            serviceDate: 19300,
            notes: "First service",
            pregnancyCheckResult: "PREGNANT",
          }),
        },
      ],
    });
    vi.mocked(firebaseAdmin.getAdminFirestore).mockReturnValue({
      collection: () =>
        ({
          doc: () =>
            ({
              collection: () => ({ get: mockGet }),
            }) as unknown,
        }) as unknown,
    });

    const req = new Request("http://localhost/api/v1/breeding-events", {
      headers: { Authorization: "Bearer valid-token" },
    });
    const res = await GET(req);
    expect(res.status).toBe(200);
    const data = (await res.json()) as { breedingEvents: unknown[] };
    expect(Array.isArray(data.breedingEvents)).toBe(true);
    expect(data.breedingEvents).toHaveLength(1);
    const event = data.breedingEvents[0] as Record<string, unknown>;
    expect(event.id).toBe("be1");
    expect(event.animalId).toBe("a1");
    expect(event.sireIds).toEqual(["sire1"]);
    expect(event.eventType).toBe("AI");
    expect(event.serviceDate).toBe("2022-11-04");
    expect(event.notes).toBe("First service");
    expect(event.pregnancyCheckResult).toBe("PREGNANT");
  });

  it("returns 503 when getAdminFirestore throws", async () => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue({ uid: "user1" });
    vi.mocked(firebaseAdmin.getAdminFirestore).mockImplementation(() => {
      throw new Error("Firebase not configured");
    });

    const req = new Request("http://localhost/api/v1/breeding-events", {
      headers: { Authorization: "Bearer valid-token" },
    });
    const res = await GET(req);
    expect(res.status).toBe(503);
    const data = (await res.json()) as { error: string };
    expect(data.error).toBe("Service unavailable");
  });
});
