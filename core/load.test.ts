import { it, expect, beforeEach } from "vitest";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { loadRuleSets } from "./load";
import validFull from "./fixtures/valid-full.json";

let dir: string;
const rs = (id: string): any => ({ ...structuredClone(validFull), id });
const put = (file: string, data: unknown) =>
  fs.writeFile(path.join(dir, file), typeof data === "string" ? data : JSON.stringify(data));

beforeEach(async () => {
  dir = await fs.mkdtemp(path.join(os.tmpdir(), "unscroll-"));
});

it("loads valid files sorted by id and ignores subfolders", async () => {
  await put("b.json", rs("b"));
  await put("a.json", rs("a"));
  await fs.mkdir(path.join(dir, "schema"));
  await put("schema/junk.json", "not json");
  expect((await loadRuleSets(dir)).map((r) => r.id)).toEqual(["a", "b"]);
});

it("reports an id that does not match the file name", async () => {
  await put("b.json", rs("a"));
  await expect(loadRuleSets(dir)).rejects.toMatchObject({
    errors: ['b.json: id "a" does not match file name'],
  });
});

it("reports invalid JSON", async () => {
  await put("bad.json", "{");
  await expect(loadRuleSets(dir)).rejects.toMatchObject({ errors: ["bad.json: invalid JSON"] });
});

it("prefixes validation errors with the file name", async () => {
  await put("x.json", { ...rs("x"), version: "nope" });
  await expect(loadRuleSets(dir)).rejects.toMatchObject({
    errors: [expect.stringMatching(/^x\.json: \/version /)],
  });
});
