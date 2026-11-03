import Ajv2020 from "ajv/dist/2020.js";
import { parse } from "css-what";
import schema from "../rules/schema/ruleset.schema.json";
import type { RuleSet, ValidationResult } from "./types";

const checkSchema = new Ajv2020({ allErrors: true }).compile(schema);

export function validateRuleSet(data: unknown): ValidationResult {
  if (!checkSchema(data)) {
    const errors = (checkSchema.errors ?? []).map((e) => `${e.instancePath || "/"} ${e.message}`);
    return { ok: false, errors };
  }
  const ruleSet = data as unknown as RuleSet;
  const errors = [...duplicateIds(ruleSet), ...redirectErrors(ruleSet), ...selectorErrors(ruleSet)];
  return errors.length > 0 ? { ok: false, errors } : { ok: true, ruleSet };
}

function duplicateIds(ruleSet: RuleSet): string[] {
  const ids = [
    ...(ruleSet.web?.redirects ?? []),
    ...(ruleSet.web?.hide ?? []),
    ...(ruleSet.android?.rules ?? []),
  ].map((rule) => rule.id);
  const seen = new Set<string>();
  const repeated = new Set<string>();
  for (const id of ids) (seen.has(id) ? repeated : seen).add(id);
  return [...repeated].map((id) => `duplicate rule id "${id}"`);
}

/** Every redirect target must be final: no rule, including its own, may redirect it again. */
function redirectErrors(ruleSet: RuleSet): string[] {
  const errors: string[] = [];
  const compiled: Array<{ id: string; to: string; pattern: RegExp }> = [];
  for (const rule of ruleSet.web?.redirects ?? []) {
    try {
      compiled.push({ id: rule.id, to: rule.to, pattern: new RegExp(rule.from) });
    } catch {
      errors.push(`redirect "${rule.id}": invalid regex`);
    }
  }
  for (const rule of compiled) {
    const targetPath = new URL(rule.to, "https://unscroll.invalid").pathname;
    if (rule.pattern.test(targetPath)) {
      errors.push(`redirect "${rule.id}": target matches its own pattern (loop)`);
      continue;
    }
    const other = compiled.find((candidate) => candidate !== rule && candidate.pattern.test(targetPath));
    if (other) errors.push(`redirect "${rule.id}": target matches redirect "${other.id}" (chain)`);
  }
  return errors;
}

function selectorErrors(ruleSet: RuleSet): string[] {
  const errors: string[] = [];
  for (const rule of ruleSet.web?.hide ?? []) {
    let groups: number;
    try {
      groups = parse(rule.selector).length;
    } catch {
      errors.push(`hide "${rule.id}": invalid selector`);
      continue;
    }
    if (groups !== 1) errors.push(`hide "${rule.id}": use one selector per rule`);
  }
  return errors;
}
