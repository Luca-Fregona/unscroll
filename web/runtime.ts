import type { RuleSet } from "../core/types";
import { buildHideCss } from "./engine/hide";
import { resolveRedirect } from "./engine/redirect";
import { watchUrl } from "./engine/watch";

export interface RuntimeEnv {
  win: Window;
  navigate: (href: string) => void;
  intervalMs?: number;
}

/** Applies one rule set to the current page. Returns a function that stops watching. */
export function run(ruleSet: RuleSet, env: RuntimeEnv): () => void {
  const web = ruleSet.web;
  if (!web) return () => {};

  const initial = resolveRedirect(web.redirects, env.win.location.href);
  if (initial) {
    env.navigate(initial);
    return () => {};
  }

  const doc = env.win.document;
  const style = doc.createElement("style");
  style.id = `unscroll-${ruleSet.id}`;
  style.textContent = buildHideCss(web.hide);
  (doc.head ?? doc.documentElement).appendChild(style);

  return watchUrl(
    env.win,
    (href) => {
      const target = resolveRedirect(web.redirects, href);
      if (target) env.navigate(target);
    },
    env.intervalMs,
  );
}
