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

describe("validateRuleSet — semantics", () => {
  const fail = (errors: string[]) => ({ ok: false, errors });

  it("rejects duplicate rule ids across sections", () => {
    const d = clone();
    d.web.hide[0].id = "feed-to-home";
    expect(validateRuleSet(d)).toEqual(fail(['duplicate rule id "feed-to-home"']));
  });

  it("rejects an invalid redirect regex", () => {
    const d = clone();
    d.web.redirects[0].from = "^/feed(";
    expect(validateRuleSet(d)).toEqual(fail(['redirect "feed-to-home": invalid regex']));
  });

  it("rejects a redirect whose target matches its own pattern", () => {
    const d = clone();
    d.web.redirects[0].from = "^/";
    expect(validateRuleSet(d)).toEqual(
      fail(['redirect "feed-to-home": target matches its own pattern (loop)']),
    );
  });

  it("rejects an invalid CSS selector", () => {
    const d = clone();
    d.web.hide[0].selector = "a[href=";
    expect(validateRuleSet(d)).toEqual(fail(['hide "feed-link": invalid selector']));
  });

  it("rejects a comma-separated selector list", () => {
    const d = clone();
    d.web.hide[0].selector = "a, b";
    expect(validateRuleSet(d)).toEqual(fail(['hide "feed-link": use one selector per rule']));
  });
});

describe("validateRuleSet — redirect chains", () => {
  it("rejects redirects whose targets another redirect would catch", () => {
    const d = clone();
    d.web.redirects.push({ id: "home-to-feed", description: "Loops back", from: "^/$", to: "/feed/" });
    expect(validateRuleSet(d)).toEqual({
      ok: false,
      errors: [
        'redirect "feed-to-home": target matches redirect "home-to-feed" (chain)',
        'redirect "home-to-feed": target matches redirect "feed-to-home" (chain)',
      ],
    });
  });
});
