import { describe, it, expect, vi, beforeEach } from "vitest";
import { GET } from "./route";
import * as firebaseAdmin from "../../../lib/firebaseAdmin";

vi.mock("../../../lib/firebaseAdmin", () => ({
  verifyIdToken: vi.fn(),
  getAdminFirestore: vi.fn(),
}));

describe("GET /api/v1/animals", () => {
  beforeEach(() => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue(null);
    vi.mocked(firebaseAdmin.getAdminFirestore).mockReset();
  });

  it("returns 401 when Authorization header is missing", async () => {
    const req = new Request("http://localhost/api/v1/animals");
    const res = await GET(req);
    expect(res.status).toBe(401);
    const data = (await res.json()) as { error: string; message?: string };
    expect(data.error).toBe("Unauthorized");
  });

  it("returns 401 when token is invalid", async () => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue(null);
    const req = new Request("http://localhost/api/v1/animals", {
      headers: { Authorization: "Bearer invalid" },
    });
    const res = await GET(req);
    expect(res.status).toBe(401);
  });

  it("returns 200 with animals array when token valid and Firestore returns docs", async () => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue({ uid: "user1" });
    const mockGet = vi.fn().mockResolvedValue({
      docs: [
        {
          id: "a1",
          data: () => ({
            earTagNumber: "001",
            sex: "FEMALE",
            breed: "Angus",
            dateOfBirth: 19300, // epoch day
            farmId: "farm1",
            status: "ACTIVE",
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

    const req = new Request("http://localhost/api/v1/animals", {
      headers: { Authorization: "Bearer valid-token" },
    });
    const res = await GET(req);
    expect(res.status).toBe(200);
    const data = (await res.json()) as { animals: unknown[] };
    expect(Array.isArray(data.animals)).toBe(true);
    expect(data.animals).toHaveLength(1);
    const animal = data.animals[0] as Record<string, unknown>;
    expect(animal.id).toBe("a1");
    expect(animal.earTagNumber).toBe("001");
    expect(animal.sex).toBe("FEMALE");
    expect(animal.breed).toBe("Angus");
    expect(animal.dateOfBirth).toBe("2022-11-04"); // 19300 epoch day
    expect(animal.status).toBe("ACTIVE");
  });

  it("returns 503 when getAdminFirestore throws", async () => {
    vi.mocked(firebaseAdmin.verifyIdToken).mockResolvedValue({ uid: "user1" });
    vi.mocked(firebaseAdmin.getAdminFirestore).mockImplementation(() => {
      throw new Error("Firebase not configured");
    });

    const req = new Request("http://localhost/api/v1/animals", {
      headers: { Authorization: "Bearer valid-token" },
    });
    const res = await GET(req);
    expect(res.status).toBe(503);
    const data = (await res.json()) as { error: string };
    expect(data.error).toBe("Service unavailable");
  });
});
