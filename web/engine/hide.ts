import type { HideRule } from "../../core/types";

/** One CSS block per rule: an invalid selector invalidates only its own block. */
export function buildHideCss(rules: HideRule[]): string {
  return rules.map((rule) => `${rule.selector} { display: none !important; }`).join("\n");
}
