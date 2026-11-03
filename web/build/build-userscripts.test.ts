import { it, expect, beforeAll } from "vitest";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { buildUserscripts } from "./build-userscripts";
import validFull from "../../core/fixtures/valid-full.json";

let rulesDir: string;
let outDir: string;

beforeAll(async () => {
  rulesDir = await fs.mkdtemp(path.join(os.tmpdir(), "unscroll-rules-"));
  outDir = await fs.mkdtemp(path.join(os.tmpdir(), "unscroll-out-"));
  await fs.writeFile(path.join(rulesDir, "example.json"), JSON.stringify(validFull));
  const { web: _web, ...androidOnly } = structuredClone(validFull);
  await fs.writeFile(
    path.join(rulesDir, "android-only.json"),
    JSON.stringify({ ...androidOnly, id: "android-only" }),
  );
});

it("writes one parseable, offline userscript per web rule set", async () => {
  const out = await buildUserscripts({
    rulesDir,
    outDir,
    repoUrl: "https://github.com/Luca-Fregona/unscroll",
    version: "0.1.0",
  });
  expect(out).toEqual([path.join(outDir, "unscroll-example.user.js")]);
  const code = await fs.readFile(out[0], "utf8");
  expect(code.startsWith("// ==UserScript==")).toBe(true);
  expect(code).toContain("feed-to-home");
  expect(() => new Function(code)).not.toThrow();
  expect(code).not.toMatch(/fetch\(|XMLHttpRequest|sendBeacon|localStorage|indexedDB/);
});
