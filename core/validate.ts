import Ajv2020 from "ajv/dist/2020.js";
import schema from "../rules/schema/ruleset.schema.json";
import type { RuleSet, ValidationResult } from "./types";

const checkSchema = new Ajv2020({ allErrors: true }).compile(schema);

export function validateRuleSet(data: unknown): ValidationResult {
  if (!checkSchema(data)) {
    const errors = (checkSchema.errors ?? []).map((e) => `${e.instancePath || "/"} ${e.message}`);
    return { ok: false, errors };
  }
  return { ok: true, ruleSet: data as unknown as RuleSet };
}
