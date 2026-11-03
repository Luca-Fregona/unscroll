import { it, expect } from "vitest";
import { resolveRedirect } from "./redirect";
import type { RedirectRule } from "../../core/types";

const rules: RedirectRule[] = [
  { id: "shorts", description: "s", from: "^/shorts/([A-Za-z0-9_-]{11})", to: "/watch?v=$1" },
  { id: "reels", description: "r", from: "^/reels(/|$)", to: "/" },
  { id: "self", description: "x", from: "^/$", to: "/" },
];
const go = (href: string) => resolveRedirect(rules, href);

it("sends a Short to the normal player", () =>
  expect(go("https://www.youtube.com/shorts/dQw4w9WgXcQ")).toBe(
    "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  ));

it("keeps the video id and drops share query strings", () =>
  expect(go("https://m.youtube.com/shorts/dQw4w9WgXcQ?feature=share&t=10")).toBe(
    "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
  ));

it("redirects the Reels feed but not a single reel", () => {
  expect(go("https://www.instagram.com/reels/C1a2B3c4D5e/")).toBe("https://www.instagram.com/");
  expect(go("https://www.instagram.com/reel/C1a2B3c4D5e/")).toBeNull();
});

it("returns null when nothing matches", () =>
  expect(go("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).toBeNull());

it("never redirects a page to itself", () => expect(go("https://www.instagram.com/")).toBeNull());
