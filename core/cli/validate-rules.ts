import { loadRuleSets, RuleSetLoadError } from "../load";

const dir = process.argv[2] ?? "rules";

try {
  const ruleSets = await loadRuleSets(dir);
  console.log(`✓ ${ruleSets.length} rule sets valid`);
} catch (error) {
  if (!(error instanceof RuleSetLoadError)) throw error;
  for (const message of error.errors) console.error(`✗ ${message}`);
  process.exit(1);
}
