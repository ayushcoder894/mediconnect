import { describe, expect, it } from "vitest";
import { initials, isoDate, money, percent, titleCase } from "../lib/format";

describe("format helpers", () => {
  it("drops the Dr. prefix when building initials", () => {
    expect(initials("Dr. Ananya Sharma")).toBe("AS");
    expect(initials("Rahul Kumar Verma")).toBe("RK");
  });

  it("formats rupees without decimals", () => {
    expect(money(1200)).toBe("₹1,200");
    expect(money(null)).toBe("-");
  });

  it("caps extreme probabilities instead of showing 100% or 0%", () => {
    expect(percent(0.999)).toBe(">99%");
    expect(percent(0.001)).toBe("<1%");
    expect(percent(0.634)).toBe("63%");
  });

  it("uses the local date, not UTC", () => {
    expect(isoDate(new Date(2026, 0, 5, 23, 30))).toBe("2026-01-05");
  });

  it("title-cases enum values", () => {
    expect(titleCase("CHECKED_IN")).toBe("Checked In");
  });
});
