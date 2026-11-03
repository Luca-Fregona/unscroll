import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { validateRuleSet } from "./validate";
import type { RuleSet } from "./types";

export class RuleSetLoadError extends Error {
  constructor(public readonly errors: string[]) {
    super(errors.join("\n"));
    this.name = "RuleSetLoadError";
  }
}

/** Reads every `*.json` directly inside `dir`, validates it and checks that its id matches the file name. */
export async function loadRuleSets(dir: string): Promise<RuleSet[]> {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = entries
    .filter((entry) => entry.isFile() && entry.name.endsWith(".json"))
    .map((entry) => entry.name)
    .sort();

  const ruleSets: RuleSet[] = [];
  const errors: string[] = [];
  for (const file of files) {
    let data: unknown;
    try {
      data = JSON.parse(await readFile(path.join(dir, file), "utf8"));
    } catch {
      errors.push(`${file}: invalid JSON`);
      continue;
    }
    const result = validateRuleSet(data);
    if (!result.ok) {
      errors.push(...result.errors.map((error) => `${file}: ${error}`));
      continue;
    }
    if (`${result.ruleSet.id}.json` !== file) {
      errors.push(`${file}: id "${result.ruleSet.id}" does not match file name`);
      continue;
    }
    ruleSets.push(result.ruleSet);
  }

  if (errors.length > 0) throw new RuleSetLoadError(errors);
  return ruleSets.sort((a, b) => a.id.localeCompare(b.id));
}
