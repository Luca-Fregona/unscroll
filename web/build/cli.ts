import { readFile } from "node:fs/promises";
import { buildUserscripts } from "./build-userscripts";

const pkg = JSON.parse(await readFile("package.json", "utf8")) as {
  version: string;
  repository: { url: string };
};

const written = await buildUserscripts({
  rulesDir: "rules",
  outDir: "dist",
  repoUrl: pkg.repository.url,
  version: pkg.version,
});

console.log(`built ${written.length} userscripts`);
for (const file of written) console.log(`  ${file}`);
