import { it, expect } from "vitest";
import { buildHeader } from "./header";

const h = buildHeader({
  id: "youtube",
  name: "YouTube",
  description: "Hides Shorts.",
  matches: ["*://www.youtube.com/*", "*://m.youtube.com/*"],
  version: "0.1.0",
  repoUrl: "https://github.com/Luca-Fregona/unscroll",
});
const lines = h.split("\n");

it("is a well-formed userscript block", () => {
  expect(lines[0]).toBe("// ==UserScript==");
  expect(lines.at(-1)).toBe("// ==/UserScript==");
  expect(h).toMatch(/^\/\/ @name\s+unscroll — YouTube$/m);
  expect(h).toMatch(/^\/\/ @version\s+0\.1\.0$/m);
  expect(h.match(/^\/\/ @match\s+/gm)).toHaveLength(2);
  expect(h).toMatch(/^\/\/ @run-at\s+document-start$/m);
  expect(h).toMatch(/^\/\/ @grant\s+none$/m);
  expect(h).toMatch(
    /^\/\/ @updateURL\s+https:\/\/github\.com\/Luca-Fregona\/unscroll\/releases\/latest\/download\/unscroll-youtube\.user\.js$/m,
  );
  expect(h).toMatch(/^\/\/ @license\s+GPL-3\.0-or-later$/m);
});
