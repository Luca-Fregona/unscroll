import { it, expect } from "vitest";
import { buildHideCss } from "./hide";

it("returns an empty string for no rules", () => expect(buildHideCss([])).toBe(""));

it("emits one block per rule so a bad selector only drops itself", () =>
  expect(
    buildHideCss([
      { id: "a", description: "a", selector: "ytd-reel-shelf-renderer" },
      { id: "b", description: "b", selector: 'a[href="/reels/"]' },
    ]),
  ).toBe(
    'ytd-reel-shelf-renderer { display: none !important; }\na[href="/reels/"] { display: none !important; }',
  ));
