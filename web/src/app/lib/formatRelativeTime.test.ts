import { describe, it, expect } from "vitest";
import { formatRelativeTime } from "./formatRelativeTime";

describe("formatRelativeTime", () => {
  const base = 1_000_000_000_000; // fixed timestamp for tests

  it('returns "just now" when within 60 seconds', () => {
    expect(formatRelativeTime(base, base)).toBe("just now");
    expect(formatRelativeTime(base, base + 30_000)).toBe("just now");
    expect(formatRelativeTime(base, base + 59_000)).toBe("just now");
  });

  it('returns "X min ago" when within an hour', () => {
    expect(formatRelativeTime(base, base + 60_000)).toBe("1 min ago");
    expect(formatRelativeTime(base, base + 5 * 60_000)).toBe("5 min ago");
    expect(formatRelativeTime(base, base + 59 * 60_000)).toBe("59 min ago");
  });

  it('returns "X hour(s) ago" when within 24 hours', () => {
    const oneHour = base + 3600_000;
    expect(formatRelativeTime(base, oneHour)).toBe("1 hour ago");
    expect(formatRelativeTime(base, base + 2 * 3600_000)).toBe("2 hours ago");
    expect(formatRelativeTime(base, base + 23 * 3600_000)).toBe("23 hours ago");
  });

  it('returns "yesterday" when between 24 and 48 hours', () => {
    const past25h = base + 25 * 3600_000;
    expect(formatRelativeTime(base, past25h)).toBe("yesterday");
  });

  it('returns "X days ago" when between 2 and 7 days', () => {
    expect(formatRelativeTime(base, base + 2 * 86400_000)).toBe("2 days ago");
    expect(formatRelativeTime(base, base + 6 * 86400_000)).toBe("6 days ago");
  });

  it("returns short date when 7+ days ago", () => {
    const result = formatRelativeTime(base, base + 8 * 86400_000);
    expect(result).toMatch(/[A-Za-z]{3}\s+\d{1,2}/);
  });
});
