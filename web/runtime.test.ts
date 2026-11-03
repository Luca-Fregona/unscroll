// @vitest-environment jsdom
import { it, expect, vi, beforeEach, afterEach } from "vitest";
import { run } from "./runtime";
import type { RuleSet } from "../core/types";

const ruleSet: RuleSet = {
  schemaVersion: 1,
  id: "t",
  name: "T",
  description: "d",
  version: "2026.11.03",
  web: {
    matches: ["*://localhost/*"],
    redirects: [
      { id: "shorts", description: "s", from: "^/shorts/([A-Za-z0-9_-]{11})", to: "/watch?v=$1" },
    ],
    hide: [{ id: "shelf", description: "h", selector: ".shorts-shelf" }],
  },
};
const target = () => `${location.origin}/watch?v=dQw4w9WgXcQ`;

beforeEach(() => {
  history.replaceState({}, "", "/");
  document.getElementById("unscroll-t")?.remove();
  if (!document.head) document.documentElement.prepend(document.createElement("head"));
});
afterEach(() => vi.useRealTimers());

it("injects one style element with the hide rules", () => {
  const stop = run(ruleSet, { win: window, navigate: vi.fn() });
  expect(document.getElementById("unscroll-t")?.textContent).toBe(
    ".shorts-shelf { display: none !important; }",
  );
  stop();
});

it("redirects on load without injecting styles", () => {
  history.replaceState({}, "", "/shorts/dQw4w9WgXcQ");
  const navigate = vi.fn();
  run(ruleSet, { win: window, navigate });
  expect(navigate).toHaveBeenCalledExactlyOnceWith(target());
  expect(document.getElementById("unscroll-t")).toBeNull();
});

it("redirects after an in-page navigation", () => {
  vi.useFakeTimers();
  const navigate = vi.fn();
  const stop = run(ruleSet, { win: window, navigate });
  history.pushState({}, "", "/shorts/dQw4w9WgXcQ");
  vi.advanceTimersByTime(300);
  expect(navigate).toHaveBeenCalledExactlyOnceWith(target());
  stop();
});

it("falls back to <html> when <head> does not exist yet", () => {
  document.head.remove();
  const stop = run(ruleSet, { win: window, navigate: vi.fn() });
  expect(document.getElementById("unscroll-t")?.parentElement).toBe(document.documentElement);
  stop();
});
