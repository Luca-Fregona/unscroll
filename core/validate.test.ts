import { describe, it, expect } from "vitest";
import { validateRuleSet } from "./validate";
import validFull from "./fixtures/valid-full.json";

const clone = (): any => structuredClone(validFull);

describe("validateRuleSet — schema", () => {
  it("accepts the full fixture", () =>
    expect(validateRuleSet(clone())).toEqual({ ok: true, ruleSet: validFull }));

  it("requires web or android", () => {
    const d = clone();
    delete d.web;
    delete d.android;
    expect(validateRuleSet(d).ok).toBe(false);
  });

  it("rejects ids that are not kebab-case", () => {
    const d = clone();
    d.id = "You Tube";
    expect(validateRuleSet(d).ok).toBe(false);
  });

  it("rejects cross-origin redirect targets", () => {
    const d = clone();
    d.web.redirects[0].to = "https://evil.example/";
    expect(validateRuleSet(d).ok).toBe(false);
  });

  it("rejects a malformed version", () => {
    const d = clone();
    d.version = "3 Nov 2026";
    expect(validateRuleSet(d).ok).toBe(false);
  });

  it("rejects android rules with an empty when", () => {
    const d = clone();
    d.android.rules[0].when = {};
    expect(validateRuleSet(d).ok).toBe(false);
  });
});
