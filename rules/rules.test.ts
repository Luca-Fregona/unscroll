import { it, expect } from "vitest";
import { fileURLToPath } from "node:url";
import { loadRuleSets } from "../core/load";
import { resolveRedirect } from "../web/engine/redirect";

const sets = await loadRuleSets(fileURLToPath(new URL(".", import.meta.url)));
const byId = Object.fromEntries(sets.map((s) => [s.id, s]));
const go = (id: string, href: string) => resolveRedirect(byId[id].web!.redirects, href);

it("YouTube covers desktop and mobile hosts", () =>
  expect(byId.youtube.web!.matches).toEqual(
    expect.arrayContaining(["*://www.youtube.com/*", "*://m.youtube.com/*"]),
  ));

it("YouTube Shorts open in the player with the right id", () =>
  expect(go("youtube", "https://m.youtube.com/shorts/dQw4w9WgXcQ?feature=share")).toBe(
    "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
  ));

it("YouTube hides mobile Shorts surfaces", () =>
  expect(byId.youtube.web!.hide.some((r) => r.selector.startsWith("ytm-"))).toBe(true));

it("Instagram Reels feed goes home and the Explore grid goes to search", () => {
  expect(go("instagram", "https://www.instagram.com/reels/C1a2B3c4D5e/")).toBe(
    "https://www.instagram.com/",
  );
  expect(go("instagram", "https://www.instagram.com/explore/")).toBe(
    "https://www.instagram.com/explore/search/",
  );
});

it("Instagram keeps the Explore link, which is the search entry on mobile", () =>
  expect(byId.instagram.web!.hide.map((r) => r.selector)).not.toContain('a[href="/explore/"]'));

it("Instagram keeps single reels, Explore search and DMs reachable", () => {
  for (const p of ["/reel/C1a2B3c4D5e/", "/explore/search/", "/direct/inbox/"])
    expect(go("instagram", `https://www.instagram.com${p}`)).toBeNull();
});
