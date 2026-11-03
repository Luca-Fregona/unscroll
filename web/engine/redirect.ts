import type { RedirectRule } from "../../core/types";

/**
 * Returns the URL to redirect to, or null. The first rule whose `from` matches the
 * path decides; `$1`…`$9` in `to` are replaced with its capture groups.
 */
export function resolveRedirect(rules: RedirectRule[], href: string): string | null {
  const url = new URL(href);
  for (const rule of rules) {
    const match = new RegExp(rule.from).exec(url.pathname);
    if (!match) continue;
    const path = rule.to.replace(/\$([1-9])/g, (_, group: string) => match[Number(group)] ?? "");
    const target = new URL(path, url.origin).href;
    return target === href ? null : target;
  }
  return null;
}
