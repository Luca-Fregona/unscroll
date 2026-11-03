// @vitest-environment jsdom
import { it, expect, vi, beforeEach, afterEach } from "vitest";
import { watchUrl } from "./watch";

beforeEach(() => history.replaceState({}, "", "/"));
afterEach(() => vi.useRealTimers());

it("reports a pushState navigation once, and nothing after stop", () => {
  vi.useFakeTimers();
  const seen: string[] = [];
  const stop = watchUrl(window, (href) => seen.push(href));
  history.pushState({}, "", "/shorts/dQw4w9WgXcQ");
  vi.advanceTimersByTime(600);
  expect(seen).toEqual([`${location.origin}/shorts/dQw4w9WgXcQ`]);
  stop();
  history.pushState({}, "", "/other");
  vi.advanceTimersByTime(600);
  expect(seen).toHaveLength(1);
});

it("reports popstate without waiting for the timer", () => {
  const seen: string[] = [];
  const stop = watchUrl(window, (href) => seen.push(href));
  history.pushState({}, "", "/a");
  window.dispatchEvent(new PopStateEvent("popstate"));
  expect(seen).toEqual([`${location.origin}/a`]);
  stop();
});
