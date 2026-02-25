import { describe, it, expect } from "vitest";
import { GET } from "./route";
import { isValidStats } from "@/app/lib/herdStatsValidation";
import { SAMPLE_STATS_DATA } from "@/app/lib/sampleStatsData";

describe("GET /api/stats", () => {
  it("returns 200 with valid herd stats shape", async () => {
    const res = await GET();
    expect(res.status).toBe(200);

    const data = await res.json();
    expect(isValidStats(data)).toBe(true);
  });

  it("returns shared sample stats (single source of truth)", async () => {
    const res = await GET();
    const data = await res.json();
    expect(data).toEqual(SAMPLE_STATS_DATA);
  });

  it("returns stats with required number fields", async () => {
    const res = await GET();
    const data = await res.json();

    expect(typeof data.totalAnimals).toBe("number");
    expect(typeof data.dueSoon).toBe("number");
    expect(typeof data.calvingsThisYear).toBe("number");
    expect(typeof data.breedingEventsThisYear).toBe("number");
    expect(typeof data.openPregnant).toBe("number");
    expect(data.byStatus).toBeDefined();
    expect(typeof data.byStatus).toBe("object");
    expect(data.bySex).toBeDefined();
    expect(typeof data.bySex).toBe("object");
  });
});
