import { build } from "esbuild";
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { loadRuleSets } from "../../core/load";
import { buildHeader } from "./header";

export interface BuildOptions {
  rulesDir: string;
  outDir: string;
  repoUrl: string;
  version: string;
}

const repoRoot = fileURLToPath(new URL("../..", import.meta.url));

/** Bundles the runtime with each rule set that has a `web` target. Returns the written paths, sorted. */
export async function buildUserscripts(options: BuildOptions): Promise<string[]> {
  await mkdir(options.outDir, { recursive: true });
  const written: string[] = [];

  for (const ruleSet of await loadRuleSets(options.rulesDir)) {
    if (!ruleSet.web) continue;
    const header = buildHeader({
      id: ruleSet.id,
      name: ruleSet.name,
      description: ruleSet.description,
      matches: ruleSet.web.matches,
      version: options.version,
      repoUrl: options.repoUrl,
    });
    const result = await build({
      stdin: {
        contents: `import { run } from "./web/runtime";
run(${JSON.stringify(ruleSet)}, { win: window, navigate: (href) => window.location.replace(href) });`,
        resolveDir: repoRoot,
        loader: "ts",
      },
      bundle: true,
      format: "iife",
      target: "es2020",
      write: false,
      banner: { js: header },
    });
    const file = path.join(options.outDir, `unscroll-${ruleSet.id}.user.js`);
    await writeFile(file, result.outputFiles[0].text);
    written.push(file);
  }

  return written.sort();
}
